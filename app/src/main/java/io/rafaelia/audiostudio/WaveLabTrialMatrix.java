/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

import java.util.Arrays;

/**
 * Platform-free hosted Java algebra for three genuinely new acoustic takes.
 * No AudioRecord, speaker playback, SPL conversion, statistics of independent
 * populations, Android permissions, file I/O or automatic room correction.
 *
 * Each double is an observed NONNEGATIVE LINEAR SPECTRAL POWER under the exact
 * same, externally identified estimator and normalization. Goertzel point
 * amplitudes, FFT PSD, and octave-band powers are not interchangeable.
 * This class never estimates the acoustic system from an unknown reference.
 */
final class WaveLabTrialMatrix {
    static final int MIN_BANDS = 1;
    static final int MAX_BANDS = 32;
    static final int TAKE_COUNT = 3;
    static final int MIN_SECONDS = 3;

    enum Role { PRIMARY, SECONDARY, TERTIARY }
    enum Decision {
        ACCEPTED_PRIMARY, ACCEPTED_REPEAT,
        REJECTED_ORDER, REJECTED_COMPLETE, REJECTED_INPUT,
        REJECTED_UNBOUND_SOURCE, REJECTED_SHORT,
        REJECTED_CLIPPING, REJECTED_CONTEXT, REJECTED_CLOCK
    }

    static final class Take {
        final Role role;
        final String stimulusKind, stimulusSha256, rawSha256;
        final String micChain, speakerChain, position, gain, estimator;
        final String calibrationRef, clockDomain;
        final int sampleRate, samples, clippedSamples;
        final long startMonotonicNs, endMonotonicNs;
        final double[] bandHz, linearPower;

        Take(Role role, String stimulusKind, String stimulusSha256,
             String rawSha256, String micChain, String speakerChain,
             String position, String gain, String estimator,
             String calibrationRef, String clockDomain,
             int sampleRate, int samples, int clippedSamples,
             long startMonotonicNs, long endMonotonicNs,
             double[] bandHz, double[] linearPower) {
            this.role = role;
            this.stimulusKind = stimulusKind;
            this.stimulusSha256 = stimulusSha256;
            this.rawSha256 = rawSha256;
            this.micChain = micChain;
            this.speakerChain = speakerChain;
            this.position = position;
            this.gain = gain;
            this.estimator = estimator;
            this.calibrationRef = calibrationRef;
            this.clockDomain = clockDomain;
            this.sampleRate = sampleRate;
            this.samples = samples;
            this.clippedSamples = clippedSamples;
            this.startMonotonicNs = startMonotonicNs;
            this.endMonotonicNs = endMonotonicNs;
            this.bandHz = bandHz == null ? null : bandHz.clone();
            this.linearPower = linearPower == null ? null : linearPower.clone();
        }
    }

    static final class Report {
        final int acceptedTakes;
        final double[] medianLinearPower, medianRelativeToPrimaryDb, spanDb;
        final boolean absoluteSplClaimAllowed;
        final boolean independentCalibrationProven;
        final String state;
        Report(int n, double[] median, double[] relative, double[] span) {
            acceptedTakes = n;
            medianLinearPower = median;
            medianRelativeToPrimaryDb = relative;
            spanDb = span;
            absoluteSplClaimAllowed = false;
            independentCalibrationProven = false;
            state = n == 3 ? "THREE_TAKES_RELATIVE_ONLY" : "INCOMPLETE_REPEATS";
        }
    }

    private final Take[] takes = new Take[TAKE_COUNT];
    private int count;
    int count() { return count; }

    private static boolean nonempty(String s) {
        return s != null && s.length() > 0 && s.length() <= 128 &&
                !s.startsWith("TOKEN_VAZIO");
    }
    private static boolean digest(String s) {
        if (s == null || s.length() != 64) return false;
        for (int i = 0; i < 64; ++i) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') ||
                    (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }
    private static boolean same(String x, String y) {
        return x.equals(y);
    }

    Decision append(Take t) {
        if (count == TAKE_COUNT) return Decision.REJECTED_COMPLETE;
        if (t == null || t.role == null) return Decision.REJECTED_INPUT;
        if (t.role.ordinal() != count) return Decision.REJECTED_ORDER;
        if (!digest(t.stimulusSha256) || !digest(t.rawSha256))
            return Decision.REJECTED_UNBOUND_SOURCE;
        if (!nonempty(t.stimulusKind) || !nonempty(t.micChain) ||
            !nonempty(t.speakerChain) || !nonempty(t.position) ||
            !nonempty(t.gain) || !nonempty(t.estimator) ||
            !nonempty(t.calibrationRef) || !nonempty(t.clockDomain) ||
            t.sampleRate < 8000 || t.sampleRate > 192000 ||
            t.clippedSamples < 0 || t.clippedSamples > t.samples ||
            t.startMonotonicNs <= 0 || t.endMonotonicNs <= t.startMonotonicNs ||
            t.bandHz == null || t.linearPower == null ||
            t.bandHz.length != t.linearPower.length ||
            t.bandHz.length < MIN_BANDS || t.bandHz.length > MAX_BANDS)
            return Decision.REJECTED_INPUT;
        if (t.samples < (long) t.sampleRate * MIN_SECONDS ||
            t.samples > (long) t.sampleRate * 120L)
            return Decision.REJECTED_SHORT;
        if (t.clippedSamples != 0) return Decision.REJECTED_CLIPPING;

        for (int i = 0; i < t.bandHz.length; ++i) {
            if (!Double.isFinite(t.bandHz[i]) || t.bandHz[i] <= 0.0 ||
                t.bandHz[i] >= t.sampleRate * 0.5 ||
                (i > 0 && t.bandHz[i] <= t.bandHz[i-1]) ||
                !Double.isFinite(t.linearPower[i]) || t.linearPower[i] <= 0.0)
                return Decision.REJECTED_INPUT;
        }
        for (int i = 0; i < count; ++i)
            if (t.rawSha256.equals(takes[i].rawSha256))
                return Decision.REJECTED_UNBOUND_SOURCE; // replay is not a new take

        if (count > 0) {
            Take primary = takes[0];
            if (!same(primary.stimulusKind, t.stimulusKind) ||
                !same(primary.stimulusSha256, t.stimulusSha256) ||
                !same(primary.micChain, t.micChain) ||
                !same(primary.speakerChain, t.speakerChain) ||
                !same(primary.position, t.position) ||
                !same(primary.gain, t.gain) ||
                !same(primary.estimator, t.estimator) ||
                !same(primary.calibrationRef, t.calibrationRef) ||
                !same(primary.clockDomain, t.clockDomain) ||
                primary.sampleRate != t.sampleRate ||
                primary.bandHz.length != t.bandHz.length)
                return Decision.REJECTED_CONTEXT;
            for (int i=0;i<t.bandHz.length;++i)
                if (primary.bandHz[i] != t.bandHz[i])
                    return Decision.REJECTED_CONTEXT;
            if (t.startMonotonicNs <= takes[count-1].endMonotonicNs)
                return Decision.REJECTED_CLOCK;
        }
        takes[count++] = t;
        return count == 1 ? Decision.ACCEPTED_PRIMARY : Decision.ACCEPTED_REPEAT;
    }

    Report report() {
        if (count == 0) return new Report(0, new double[0], new double[0], new double[0]);
        int n = takes[0].bandHz.length;
        double[] median = new double[n], relative = new double[n], span = new double[n];
        for (int band = 0; band < n; ++band) {
            double[] values = new double[count];
            for (int j=0;j<count;++j) values[j] = takes[j].linearPower[band];
            Arrays.sort(values);
            median[band] = count % 2 == 1 ? values[count/2]
                    : (values[count/2-1] / 2.0 + values[count/2] / 2.0);
            // Log-differences avoid overflow/underflow of large power ratios.
            relative[band] = 10.0 * (Math.log10(median[band])
                    - Math.log10(takes[0].linearPower[band]));
            span[band] = count < 2 ? Double.NaN
                    : 10.0 * (Math.log10(values[count-1]) - Math.log10(values[0]));
        }
        return new Report(count, median, relative, span);
    }
}
