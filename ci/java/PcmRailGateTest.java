/*
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Host-only, no Android services, no audio fixtures and no network.
 */
package io.rafaelia.audiostudio;

public final class PcmRailGateTest {
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }

    public static void main(String[] args) {
        check(PcmRailGate.countFullScaleSamples(null, 1) == -1, "null");
        check(PcmRailGate.countFullScaleSamples(new short[0], 0) == -1, "empty");
        check(PcmRailGate.countFullScaleSamples(new short[]{123}, -1) == -1, "negative");
        check(PcmRailGate.countFullScaleSamples(new short[]{123}, 2) == -1, "out of bounds");

        short[] pcm = {0, 1, -1, Short.MIN_VALUE, Short.MAX_VALUE, 1000};
        check(PcmRailGate.countFullScaleSamples(pcm, pcm.length) == 2, "both rails");
        check(PcmRailGate.countFullScaleSamples(pcm, 3) == 0, "bounded prefix");
        check(PcmRailGate.countFullScaleSamples(
                new short[]{Short.MAX_VALUE}, 1) == 1, "single rail");
        check("TOKEN_VAZIO_INVALID_CAPTURE".equals(
                PcmRailGate.qualityState(-1)), "typed empty");
        check("REVIEW_REQUIRED_PCM_FULL_SCALE".equals(
                PcmRailGate.qualityState(2)), "review classification");
        check("NO_PCM_FULL_SCALE_OBSERVED_NOT_CALIBRATED".equals(
                PcmRailGate.qualityState(0)), "zero is not absolute validity");

        System.out.println("PCM_RAIL_GATE_HOST=PASS_SCOPED");
        System.out.println("PCM_RAIL_DEVICE=NOT_RUN");
        System.out.println("PCM_RAIL_ANALOGUE_CAUSE=TOKEN_VAZIO_UNMEASURED");
    }
}
