/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 *
 * PCM16 rail observation only. No claim of analogue clipping or SPL calibration.
 */
package io.rafaelia.audiostudio;

final class PcmRailGate {
    static final int TOKEN_VAZIO_INVALID_CAPTURE = -1;

    private PcmRailGate() {}

    static int countFullScaleSamples(short[] samples, int count) {
        if (samples == null || count <= 0 || count > samples.length) {
            return TOKEN_VAZIO_INVALID_CAPTURE;
        }
        int hits = 0;
        for (int i = 0; i < count; ++i) {
            if (samples[i] == Short.MIN_VALUE || samples[i] == Short.MAX_VALUE) {
                ++hits;
            }
        }
        return hits;
    }

    static String qualityState(int railSamples) {
        if (railSamples < 0) return "TOKEN_VAZIO_INVALID_CAPTURE";
        if (railSamples > 0) return "REVIEW_REQUIRED_PCM_FULL_SCALE";
        return "NO_PCM_FULL_SCALE_OBSERVED_NOT_CALIBRATED";
    }
}
