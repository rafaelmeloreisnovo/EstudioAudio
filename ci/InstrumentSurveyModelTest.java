/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

import java.util.ArrayList;
import java.util.List;

/** Host vectors for retention/quality/export. Does not exercise Android hardware. */
public final class InstrumentSurveyModelTest {
    private static int assertions;
    private static void check(boolean value, String message) {
        ++assertions;
        if (!value) throw new AssertionError(message);
    }
    private static void fails(Runnable action, String message) {
        try { action.run(); } catch (IllegalArgumentException expected) {
            ++assertions; return;
        }
        throw new AssertionError(message);
    }
    private static InstrumentSurveyModel.Item item(String id) {
        return new InstrumentSurveyModel.Item(id, "Movimento", "Aceleração", "Eixo X", "m/s²");
    }
    private static InstrumentSurveyModel model(int rows, int windows) {
        InstrumentSurveyModel m = new InstrumentSurveyModel(new InstrumentSurveyModel.Config(
                1000, rows, windows, 2, 1000, 20));
        m.register(item("sensor.0.x"));
        return m;
    }
    private static void sample(InstrumentSurveyModel m, String id, long now, long source,
                               double value, double accuracy, int sector, String state) {
        m.observe(id, 1700000000000L + now / 1000000, now, source, value, accuracy, sector, state);
    }
    private static List<List<String>> csv(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"'); ++i;
                } else quoted = !quoted;
            } else if (!quoted && (c == ',' || c == '\n')) {
                row.add(field.toString()); field.setLength(0);
                if (c == '\n') { rows.add(row); row = new ArrayList<>(); }
            } else field.append(c);
        }
        check(!quoted && field.length() == 0 && row.isEmpty(), "complete CSV records");
        if (!rows.isEmpty()) for (List<String> r : rows)
            check(r.size() == rows.get(0).size(), "rectangular CSV");
        return rows;
    }
    private static void near(String actual, double expected, String message) {
        check(Math.abs(Double.parseDouble(actual) - expected) < 1e-8, message);
    }

    private static void configVectors() {
        fails(() -> new InstrumentSurveyModel.Config(99, 1, 1, 2, 0, 0), "short window");
        fails(() -> new InstrumentSurveyModel.Config(600001, 1, 1, 2, 0, 0), "long window");
        fails(() -> new InstrumentSurveyModel.Config(100, 0, 1, 2, 0, 0), "zero rows");
        fails(() -> new InstrumentSurveyModel.Config(100, 20000, 2, 2, 0, 0), "total memory cap");
        fails(() -> new InstrumentSurveyModel.Config(100, 1, 65, 2, 0, 0), "rotation cap");
        fails(() -> new InstrumentSurveyModel.Config(100, 1, 1, 1, 0, 0), "min samples");
        fails(() -> new InstrumentSurveyModel.Config(100, 1, 1, 2, -1, 0), "negative age");
        fails(() -> new InstrumentSurveyModel.Config(100, 1, 1, 2, 0, Double.NaN), "NaN threshold");
        fails(() -> new InstrumentSurveyModel.Config(100, 1, 1, 2, 0, Double.POSITIVE_INFINITY), "infinite threshold");
        fails(() -> new InstrumentSurveyModel(null), "null policy");
        fails(() -> new InstrumentSurveyModel.Item("x", "f", "g", "x".repeat(257), "u"), "metadata length cap");
        InstrumentSurveyModel m = model(10, 1);
        m.register(item("sensor.0.x"));
        fails(() -> m.register(new InstrumentSurveyModel.Item("sensor.0.x", "Other", "g", "l", "u")),
                "metadata identity stable");
        fails(() -> sample(m, "unknown", 1, 1, 1, 0, -1, "OBSERVED"), "unknown item");
        fails(() -> sample(m, "sensor.0.x", 1, 1, 1, 0, 8, "OBSERVED"), "invalid sector");
        for (int i = 1; i < InstrumentSurveyModel.MAX_ITEMS; ++i) m.register(item("sensor." + i));
        fails(() -> m.register(item("overflow")), "bounded catalog");
    }

    private static void ringAndStatistics() {
        InstrumentSurveyModel m = model(2, 2);
        for (int i = 0; i < 4; ++i) sample(m, "sensor.0.x", i * 100000000L,
                i * 100000000L, 1 + 2 * i, Double.NaN, 0, "OBSERVED");
        check(m.rawCount() == 4 && m.isFull(), "ring full exactly");
        sample(m, "sensor.0.x", 400000000L, 400000000L, 9, Double.NaN, 0, "OBSERVED");
        check(m.rawCount() == 3 && m.rotations() == 2, "evict complete oldest chunk");
        List<List<String>> raw = csv(m.exportCsv(true));
        check(raw.size() == 4, "three retained rows plus header");
        near(raw.get(1).get(9), 5, "oldest retained value");
        near(raw.get(3).get(9), 9, "latest retained value");
        List<String> sum = csv(m.exportSummaryCsv(true)).get(1);
        check(sum.get(7).equals("5") && sum.get(8).equals("0"), "summaries survive raw rotation");
        near(sum.get(9), 5, "mean known vector");
        near(sum.get(10), 10, "unbiased variance known vector");
        near(sum.get(11), 10, "accepted density from source clock");
        check(sum.get(14).equals("DESCRIPTIVE_ONLY_NOT_CALIBRATION"), "no physical claim");
        List<String> cfg = csv(m.configurationCsv()).get(1);
        check(cfg.get(10).equals("2"), "explicit raw eviction count");
        check(m.dashboard().contains("variância: 10.0"), "dashboard exposes variance");
        InstrumentSurveyModel timed = model(4, 2);
        sample(timed, "sensor.0.x", 0, 0, 1, Double.NaN, -1, "OBSERVED");
        sample(timed, "sensor.0.x", 1000000000L, 1000000000L, 1, Double.NaN, -1, "OBSERVED");
        sample(timed, "sensor.0.x", 2000000000L, 2000000000L, 1, Double.NaN, -1, "OBSERVED");
        check(timed.rawCount() == 2 && timed.rotations() == 2, "time windows rotate at boundary");
        InstrumentSurveyModel single = model(1, 1);
        sample(single, "sensor.0.x", 0, 0, 1, Double.NaN, -1, "OBSERVED");
        sample(single, "sensor.0.x", 1000000000L, 500000000L, 3, Double.NaN, -1, "OBSERVED");
        check(single.rawCount() == 1 && single.rotations() == 1, "simultaneous time/count boundary rotates once");
        near(csv(single.exportSummaryCsv(true)).get(1).get(11), 2, "rate uses source interval not collection interval");
    }

    private static void qualityVectors() {
        InstrumentSurveyModel m = model(32, 1);
        long s = 2000000000L;
        sample(m, "sensor.0.x", s, s, 3, Double.NaN, -1, "OBSERVED");
        sample(m, "sensor.0.x", s + 1, s, 3, Double.NaN, 0, "OBSERVED");
        sample(m, "sensor.0.x", s + 2, s - 1, 3, Double.NaN, -1, "OBSERVED");
        sample(m, "sensor.0.x", s + 3, -1, 3, Double.NaN, -1, "OBSERVED");
        sample(m, "sensor.0.x", s + 4, s + 5, 3, Double.NaN, -1, "OBSERVED");
        sample(m, "sensor.0.x", s + 5, 0, 3, Double.NaN, -1, "OBSERVED");
        sample(m, "sensor.0.x", s + 6, s + 6, Double.NaN, Double.NaN, -1, "OBSERVED");
        sample(m, "sensor.0.x", s + 7, s + 7, 3, -1, -1, "OBSERVED");
        sample(m, "sensor.0.x", s + 8, s + 8, 3, Double.POSITIVE_INFINITY, -1, "OBSERVED");
        sample(m, "sensor.0.x", s + 9, s + 9, 3, Double.NaN, -1, "UNRELIABLE");
        sample(m, "sensor.0.x", s + 10, s + 10, Double.MAX_VALUE, Double.NaN, -1, "OBSERVED");
        String raw = m.exportCsv(true);
        for (String reason : new String[]{"ACCEPTED_ACCURACY_UNKNOWN", "DUPLICATE_SOURCE", "SOURCE_OUT_OF_ORDER",
                "TIMESTAMP_MISSING", "SOURCE_IN_FUTURE", "STALE", "VALUE_UNAVAILABLE", "INVALID_ACCURACY",
                "SOURCE_UNRELIABLE", "NUMERIC_RANGE"}) check(raw.contains(reason), reason);
        List<List<String>> summaries = csv(m.exportSummaryCsv(true));
        check(summaries.get(1).get(7).equals("1"), "only valid sample accepted");
        check(summaries.get(1).get(9).equals("3.0"), "invalid samples cannot bias mean");
        check(summaries.get(1).get(10).equals("TOKEN_VAZIO"), "one sample no invented variance");
        check(summaries.get(1).get(11).equals("TOKEN_VAZIO"), "one sample no invented rate");
        check(summaries.get(2).get(7).equals("0"), "duplicate cannot train another pose sector");
        m.register(item("location.latitude"));
        sample(m, "location.latitude", s, s, -23, Double.NaN, -1, "OBSERVED");
        sample(m, "location.latitude", s + 1, s + 1, -23, 21, -1, "OBSERVED");
        sample(m, "location.latitude", s + 2, s + 2, -23, 20, -1, "OBSERVED");
        check(m.exportCsv(true).contains("ACCURACY_MISSING"), "GPS needs reported uncertainty");
        check(m.exportCsv(true).contains("ACCURACY_LIMIT"), "GPS metre threshold");
        sample(m, "sensor.0.x", s + 11, s + 11, 5, 100, -1, "OBSERVED");
        check(csv(m.exportSummaryCsv(true)).get(1).get(7).equals("2"), "GPS limit not applied to different source units");
    }

    private static void exportVectors() {
        InstrumentSurveyModel m = model(32, 1);
        String[] hidden = {"location.latitude", "cell.cid", "wifi.rssi", "gnss.mean_cn0", "bluetooth.enabled"};
        for (String id : hidden) {
            m.register(id.equals("cell.cid") ? new InstrumentSurveyModel.Item(id, "Rádio", "Celular", "CID", "id") : item(id));
            sample(m, id, 100, 100, 10, 1, -1, "OBSERVED");
        }
        m.register(new InstrumentSurveyModel.Item("cell.mcc", "Rádio", "Celular", "MCC", "id"));
        sample(m, "cell.mcc", 100, 100, 724, Double.NaN, -1, "OBSERVED");
        m.register(new InstrumentSurveyModel.Item("sensor.label", "=formula", "g,roup", "quote\"\nline", "u"));
        sample(m, "sensor.label", 100, 100, 0, Double.NaN, -1, "OBSERVED");
        List<List<String>> raw = csv(m.exportCsv(false));
        check(raw.size() == 2, "sensitive rows absent by default");
        check(raw.get(1).get(2).equals("'=formula"), "spreadsheet formula neutralized");
        check(raw.get(1).get(3).equals("g,roup") && raw.get(1).get(4).equals("quote\"\nline"), "CSV escaping round trip");
        for (String id : hidden) {
            check(!m.exportCsv(false).contains(id), "raw redaction " + id);
            check(!m.exportSummaryCsv(false).contains(id), "summary redaction " + id);
        }
        List<List<String>> summaries = csv(m.exportSummaryCsv(true));
        for (List<String> row : summaries) if (row.get(1).equals("cell.cid") || row.get(1).equals("cell.mcc")) {
            check(row.get(9).equals("TOKEN_VAZIO") && row.get(10).equals("TOKEN_VAZIO"), "cell identity is not a numeric stability measure");
            check(row.get(14).equals("IDENTITY_NOT_NUMERIC_STABILITY"), "identity explanation");
        }
        check(csv(m.exportCsv(true)).size() == 8, "explicit sensitive export keeps rows");
    }

    public static void main(String[] args) {
        configVectors(); ringAndStatistics(); qualityVectors(); exportVectors();
        System.out.println("INSTRUMENT_SURVEY_MODEL_HOST=PASS assertions=" + assertions);
        System.out.println("INSTRUMENT_SURVEY_PHYSICAL_DEVICE=NOT_RUN");
    }
}
