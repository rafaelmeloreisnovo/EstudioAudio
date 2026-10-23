/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Process-local only: no retained waveform or voice identity fingerprint.
 */
package io.rafaelia.audiostudio;

final class TimbreProfileStore {
    private static int mixQ8;
    private static int meanAbs;
    private static int crossingsK;
    private static int sourceRate;
    private static int frames;
    private static boolean ready;

    private TimbreProfileStore() {}

    static synchronized void put(int mix, int amplitude, int crossings,
                                 int rate, int sampleCount) {
        if (mix < 64 || mix > 240 || amplitude < 64 || amplitude > 32768 ||
                crossings < 0 || crossings > 1000 || rate < 8000 ||
                rate > 48000 || sampleCount < 8000 || sampleCount > 1000000) {
            throw new IllegalArgumentException("invalid bounded timbre profile");
        }
        mixQ8 = mix;
        meanAbs = amplitude;
        crossingsK = crossings;
        sourceRate = rate;
        frames = sampleCount;
        ready = true;
    }

    static synchronized int[] snapshot() {
        return ready ? new int[]{mixQ8, meanAbs, crossingsK, sourceRate, frames} : null;
    }

    static synchronized void clear() {
        ready = false;
        mixQ8 = 0;
        meanAbs = 0;
        crossingsK = 0;
        sourceRate = 0;
        frames = 0;
    }
}
