/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Host JVM test: proves fail-closed fallback without Android/JNI availability.
 */
package io.rafaelia.audiostudio;

final class NativeByteCoreFallbackTest {
    public static void main(String[] args) {
        int vectors = 0;
        for (int length : new int[] {0, 3, 255, 4096, 65536}) {
            byte[] input = new byte[length + 7];
            for (int i = 0; i < input.length; ++i) input[i] = (byte)(i * 73 + 19);
            NativeByteCore.Result sha = NativeByteCore.sha256(input, 3, length, false);
            NativeByteCore.Result crc = NativeByteCore.crc32(input, 3, length, false);
            if (sha.route != NativeByteCore.ROUTE_JAVA_BASELINE ||
                    crc.route != NativeByteCore.ROUTE_JAVA_BASELINE) {
                throw new AssertionError("baseline route changed");
            }
            if (sha.bytes.length != 32) throw new AssertionError("digest length");
            NativeByteCore.Result rejectedSha = NativeByteCore.sha256(input, 3, length, true);
            NativeByteCore.Result rejectedCrc = NativeByteCore.crc32(input, 3, length, true);
            if (rejectedSha.route != NativeByteCore.ROUTE_NATIVE_REJECTED_JAVA_FALLBACK ||
                    rejectedCrc.route != NativeByteCore.ROUTE_NATIVE_REJECTED_JAVA_FALLBACK) {
                throw new AssertionError("native rejection did not fall back");
            }
            if (!same32(sha.bytes, rejectedSha.bytes) || crc.word != rejectedCrc.word) {
                throw new AssertionError("fallback changed value");
            }
            ++vectors;
        }
        boolean rejected = false;
        try {
            NativeByteCore.sha256(new byte[1], 2, 1, false);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        if (!rejected) throw new AssertionError("invalid range accepted");
        System.out.println("NATIVE_BYTE_CORE_FAIL_CLOSED_FALLBACK_VECTORS=" + vectors + " PASS");
    }

    private static boolean same32(byte[] left, byte[] right) {
        int diff = 0;
        for (int i = 0; i < 32; ++i) diff |= left[i] ^ right[i];
        return diff == 0;
    }
}
