/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

/**
 * Hosted Java record management and descriptive statistics, not acoustic DSP.
 * Raw rows rotate in bounded windows; accepted summaries live for this session.
 * Neither low variance nor sample count proves calibration or physical stability.
 */
public final class InstrumentSurveyModel {
    public static final int MAX_ITEMS = 128;
    private static final int SECTORS = 9; // unknown (-1), then eight yaw sectors

    public static final class Item {
        public final String id, family, group, label, unit;
        public Item(String id, String family, String group, String label, String unit) {
            this.id = required(id, 128);
            this.family = required(family, 128);
            this.group = required(group, 128);
            this.label = required(label, 256);
            this.unit = required(unit, 64);
        }
    }

    public static final class Config {
        public final int windowMs, maxRows, rotationCount, minSamples;
        public final long maxAgeMs;
        /** Maximum location horizontal accuracy in metres, never applied to other units. */
        public final double maxAccuracy;
        public Config(int windowMs, int maxRows, int rotationCount, int minSamples,
                      long maxAgeMs, double maxAccuracy) {
            if (windowMs < 100 || windowMs > 600000 || maxRows < 1 || maxRows > 20000
                    || rotationCount < 1 || rotationCount > 64
                    || (long) maxRows * rotationCount > 20000
                    || minSamples < 2 || minSamples > 20000
                    || maxAgeMs < 0 || maxAgeMs > 600000
                    || !Double.isFinite(maxAccuracy) || maxAccuracy < 0 || maxAccuracy > 1000000)
                throw new IllegalArgumentException("survey_configuration_out_of_range");
            this.windowMs = windowMs;
            this.maxRows = maxRows;
            this.rotationCount = rotationCount;
            this.minSamples = minSamples;
            this.maxAgeMs = maxAgeMs;
            this.maxAccuracy = maxAccuracy;
        }
    }

    private static final class Summary {
        long accepted, rejected;
        long firstSourceNs = -1, lastSourceNs = -1;
        double mean, m2;
    }

    private static final class Registered {
        final Item item;
        final Summary[] sectors = new Summary[SECTORS];
        long lastAcceptedSourceNs = -1;
        Registered(Item item) {
            this.item = item;
            for (int i = 0; i < sectors.length; ++i) sectors[i] = new Summary();
        }
    }

    private static final class Row {
        Item item;
        long epochMs, elapsedNs, sourceNs;
        double value, accuracy;
        int sector;
        String state, quality;
    }

    private final Config config;
    private final Registered[] items = new Registered[MAX_ITEMS];
    private int itemCount;
    private final Row[][] chunks;
    private final int[] counts;
    private int current, rawCount, rotations;
    private long windowStartNs = -1;
    private long accepted, rejected, evicted;

    public InstrumentSurveyModel(Config config) {
        if (config == null) throw new IllegalArgumentException("config_required");
        this.config = config;
        chunks = new Row[config.rotationCount][config.maxRows];
        counts = new int[config.rotationCount];
    }

    public Config config() { return config; }
    public synchronized int rawCount() { return rawCount; }
    public synchronized int rotations() { return rotations; }
    public synchronized boolean isFull() { return rawCount == config.maxRows * config.rotationCount; }

    public synchronized void register(Item item) {
        if (item == null) throw new IllegalArgumentException("item_required");
        for (int i = 0; i < itemCount; ++i) {
            if (items[i].item.id.equals(item.id)) {
                Item old = items[i].item;
                if (!old.family.equals(item.family) || !old.group.equals(item.group)
                        || !old.label.equals(item.label) || !old.unit.equals(item.unit))
                    throw new IllegalArgumentException("item_identity_changed");
                return;
            }
        }
        if (itemCount == MAX_ITEMS) throw new IllegalArgumentException("item_capacity_exceeded");
        items[itemCount++] = new Registered(item);
    }

    /**
     * sourceNs and elapsedNs use the same Android elapsed-realtime clock domain.
     * Unknown timestamps use -1; unknown measurements/accuracy use NaN, never zero.
     * Only OBSERVED rows may train summaries. All rejected rows retain a reason.
     */
    public synchronized void observe(String itemId, long epochMs, long elapsedNs,
                                     long sourceNs, double value, double accuracy,
                                     int sector, String state) {
        if (sector < -1 || sector > 7) throw new IllegalArgumentException("invalid_pose_sector");
        state = required(state, 128);
        Registered entry = find(itemId);
        Summary summary = entry.sectors[sector + 1];
        String quality = rejection(entry, epochMs, elapsedNs, sourceNs, value, accuracy, state);
        if (quality == null && summary.accepted >= 1000000000L) quality = "SUMMARY_LIMIT";
        double nextMean = summary.mean, nextM2 = summary.m2;
        if (quality == null && !categorical(entry.item)) {
            double delta = value - summary.mean;
            nextMean = summary.mean + delta / (summary.accepted + 1);
            nextM2 = summary.m2 + delta * (value - nextMean);
            if (!Double.isFinite(nextMean) || !Double.isFinite(nextM2)) quality = "NUMERIC_RANGE";
        }
        if (quality == null) {
            ++summary.accepted;
            ++accepted;
            summary.mean = nextMean;
            summary.m2 = nextM2;
            if (summary.firstSourceNs < 0) summary.firstSourceNs = sourceNs;
            summary.lastSourceNs = sourceNs;
            entry.lastAcceptedSourceNs = sourceNs;
            quality = Double.isNaN(accuracy) ? "ACCEPTED_ACCURACY_UNKNOWN" : "ACCEPTED";
        } else {
            ++summary.rejected;
            ++rejected;
        }
        // Invalid clock rows remain evidence but cannot rotate or reset a window.
        if (elapsedNs >= 0 && windowStartNs < 0) windowStartNs = elapsedNs;
        boolean timedOut = elapsedNs >= windowStartNs && windowStartNs >= 0
                && elapsedNs - windowStartNs >= config.windowMs * 1000000L;
        if (counts[current] == config.maxRows || timedOut) rotate(elapsedNs);
        Row row = new Row();
        row.item = entry.item;
        row.epochMs = epochMs;
        row.elapsedNs = elapsedNs;
        row.sourceNs = sourceNs;
        row.value = value;
        row.accuracy = accuracy;
        row.sector = sector;
        row.state = state;
        row.quality = quality;
        chunks[current][counts[current]++] = row;
        ++rawCount;
    }

    private String rejection(Registered entry, long epochMs, long elapsedNs,
                             long sourceNs, double value, double accuracy, String state) {
        if (!"OBSERVED".equals(state)) return "SOURCE_" + state;
        if (epochMs < 0 || elapsedNs < 0 || sourceNs < 0) return "TIMESTAMP_MISSING";
        if (sourceNs > elapsedNs) return "SOURCE_IN_FUTURE";
        if (elapsedNs - sourceNs > config.maxAgeMs * 1000000L) return "STALE";
        if (!Double.isFinite(value)) return "VALUE_UNAVAILABLE";
        if (!Double.isNaN(accuracy) && (!Double.isFinite(accuracy) || accuracy < 0))
            return "INVALID_ACCURACY";
        if (entry.item.id.startsWith("location.")) {
            if (Double.isNaN(accuracy)) return "ACCURACY_MISSING";
            if (accuracy > config.maxAccuracy) return "ACCURACY_LIMIT";
        }
        if (sourceNs == entry.lastAcceptedSourceNs) return "DUPLICATE_SOURCE";
        if (sourceNs < entry.lastAcceptedSourceNs) return "SOURCE_OUT_OF_ORDER";
        return null;
    }

    private void rotate(long elapsedNs) {
        current = (current + 1) % chunks.length;
        evicted += counts[current];
        rawCount -= counts[current];
        for (int i = 0; i < counts[current]; ++i) chunks[current][i] = null;
        counts[current] = 0;
        if (elapsedNs >= 0 && elapsedNs > windowStartNs) windowStartNs = elapsedNs;
        if (rotations < Integer.MAX_VALUE) ++rotations;
    }

    private Registered find(String id) {
        for (int i = 0; i < itemCount; ++i) if (items[i].item.id.equals(id)) return items[i];
        throw new IllegalArgumentException("unregistered_item");
    }

    private static boolean sensitive(String id) {
        return id.startsWith("location.") || id.startsWith("gnss.")
                || id.startsWith("cell.") || id.startsWith("wifi.") || id.startsWith("bluetooth.");
    }

    private static boolean categorical(Item item) {
        return item.unit.equals("id") || item.unit.equals("status");
    }

    /** Chronological retained rows only; missing measurements are explicit tokens. */
    public synchronized String exportCsv(boolean includeSensitive) {
        StringBuilder out = new StringBuilder("schema,item,family,group,label,unit,epoch_ms,elapsed_ns,source_ns,value,accuracy,pose_sector,state,quality\n");
        for (int i = 1; i <= chunks.length; ++i) {
            int index = (current + i) % chunks.length;
            for (int j = 0; j < counts[index]; ++j) {
                Row row = chunks[index][j];
                if (!includeSensitive && sensitive(row.item.id)) continue;
                out.append("rafaelia.instrument-survey/v1,");
                metadata(out, row.item);
                out.append(',').append(timestamp(row.epochMs)).append(',')
                        .append(timestamp(row.elapsedNs)).append(',').append(timestamp(row.sourceNs))
                        .append(',').append(number(row.value)).append(',').append(number(row.accuracy))
                        .append(',').append(row.sector).append(',');
                csv(out, row.state); out.append(','); csv(out, row.quality); out.append('\n');
            }
        }
        return out.toString();
    }

    public synchronized String exportSummaryCsv(boolean includeSensitive) {
        StringBuilder out = new StringBuilder("schema,item,family,group,label,unit,pose_sector,accepted,rejected,mean,sample_variance,accepted_density_hz,first_source_ns,last_source_ns,assessment\n");
        for (int i = 0; i < itemCount; ++i) {
            Registered entry = items[i];
            if (!includeSensitive && sensitive(entry.item.id)) continue;
            for (int k = 0; k < SECTORS; ++k) {
                Summary s = entry.sectors[k];
                if (s.accepted + s.rejected == 0) continue;
                out.append("rafaelia.instrument-summary/v1,");
                metadata(out, entry.item);
                out.append(',').append(k - 1).append(',').append(s.accepted)
                        .append(',').append(s.rejected).append(',');
                boolean identity = categorical(entry.item);
                out.append(s.accepted > 0 && !identity ? number(s.mean) : "TOKEN_VAZIO")
                        .append(',').append(s.accepted > 1 && !identity
                                ? number(Math.max(0, s.m2) / (s.accepted - 1)) : "TOKEN_VAZIO")
                        .append(',').append(rate(s)).append(',').append(timestamp(s.firstSourceNs))
                        .append(',').append(timestamp(s.lastSourceNs)).append(',')
                        .append(identity ? "IDENTITY_NOT_NUMERIC_STABILITY" : s.accepted < config.minSamples
                                ? "INSUFFICIENT_SAMPLES" : "DESCRIPTIVE_ONLY_NOT_CALIBRATION")
                        .append('\n');
            }
        }
        return out.toString();
    }

    public synchronized String configurationCsv() {
        return "schema,window_ms,rows_per_window,retained_windows,min_samples,max_age_ms,max_location_accuracy_m,registered_items,retained_rows,rotations,evicted_rows,summary_scope\n"
                + "rafaelia.instrument-config/v1," + config.windowMs + ',' + config.maxRows + ','
                + config.rotationCount + ',' + config.minSamples + ',' + config.maxAgeMs + ','
                + number(config.maxAccuracy) + ',' + itemCount + ',' + rawCount + ',' + rotations
                + ',' + evicted + ",SESSION_ACCEPTED_DESCRIPTIVE_ONLY\n";
    }

    public synchronized String dashboard(InstrumentSurveyText labels) {
        if (labels == null) throw new IllegalArgumentException("labels_required");
        StringBuilder text = new StringBuilder(labels.t("descriptive")).append('\n');
        text.append(labels.t("accepted")).append(": ").append(accepted).append(" | ")
                .append(labels.t("rejected")).append(": ").append(rejected)
                .append('\n').append(labels.t("retained")).append(": ").append(rawCount)
                .append('/').append(config.maxRows * config.rotationCount)
                .append(" | ").append(labels.t("evicted")).append(": ").append(evicted).append('\n');
        int displayed = 0;
        for (int i = 0; i < itemCount; ++i) {
            Registered entry = items[i];
            long total = 0, refused = 0;
            int populated = 0;
            int latest = 0;
            for (int k = 0; k < SECTORS; ++k) {
                total += entry.sectors[k].accepted;
                refused += entry.sectors[k].rejected;
                if (k > 0 && entry.sectors[k].accepted >= config.minSamples) ++populated;
                if (entry.sectors[k].lastSourceNs > entry.sectors[latest].lastSourceNs) latest = k;
            }
            if (total + refused == 0) continue;
            if (displayed++ == 12) {
                text.append(labels.t("more_export")).append('\n');
                break;
            }
            text.append(entry.item.label).append(": n=").append(total)
                    .append(" | ").append(labels.t("rejected")).append('=').append(refused)
                    .append(" | ").append(labels.t("sector")).append('=').append(populated).append("/8\n");
            Summary last = entry.sectors[latest];
            text.append("  ").append(labels.t("sector")).append(": ").append(latest - 1).append(" | ").append(labels.t("variance")).append(": ")
                    .append(last.accepted > 1 && !categorical(entry.item)
                            ? number(Math.max(0, last.m2) / (last.accepted - 1)) + " (" + entry.item.unit + ")²"
                            : labels.t("not_available"))
                    .append(" | ").append(labels.t("density")).append(": ")
                    .append(last.accepted > 1 && last.lastSourceNs > last.firstSourceNs
                            ? rate(last) + " Hz" : labels.t("not_available")).append('\n');
        }
        return text.toString();
    }

    private static String rate(Summary summary) {
        long span = summary.lastSourceNs - summary.firstSourceNs;
        return summary.accepted > 1 && span > 0
                ? number((summary.accepted - 1) * 1000000000.0 / span) : "TOKEN_VAZIO";
    }

    private static void metadata(StringBuilder out, Item item) {
        csv(out, item.id); out.append(','); csv(out, item.family); out.append(',');
        csv(out, item.group); out.append(','); csv(out, item.label); out.append(','); csv(out, item.unit);
    }

    private static String required(String value, int max) {
        if (value == null || value.length() == 0 || value.length() > max || value.indexOf('\0') >= 0)
            throw new IllegalArgumentException("invalid_metadata");
        return value;
    }

    private static String timestamp(long value) { return value < 0 ? "TOKEN_VAZIO" : Long.toString(value); }
    private static String number(double value) { return Double.isFinite(value) ? Double.toString(value) : "TOKEN_VAZIO"; }

    private static void csv(StringBuilder out, String value) {
        // Text columns are quoted and formula-prefixed metadata cannot execute in a spreadsheet.
        out.append('"');
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@'
                || first == '\t' || first == '\r' || first == '\n') out.append('\'');
        for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            if (c == '"') out.append('"');
            out.append(c);
        }
        out.append('"');
    }
}
