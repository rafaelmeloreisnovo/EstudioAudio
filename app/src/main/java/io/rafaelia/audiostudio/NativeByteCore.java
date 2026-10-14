/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 *
 * Opt-in byte-core consumer. Java remains the baseline; JNI may only replace
 * a result after exact native success. Any native rejection falls back closed.
 */
package io.rafaelia.audiostudio;

final class NativeByteCore {
    static final int ROUTE_JAVA_BASELINE = 0;
    static final int ROUTE_NATIVE_EXACT = 1;
    static final int ROUTE_NATIVE_REJECTED_JAVA_FALLBACK = 2;

    private NativeByteCore() {}

    static Result sha256(byte[] input, int offset, int count, boolean allowNative) {
        checkRange(input, offset, count);
        byte[] baseline = sha256Java(input, offset, count);
        if (!allowNative) return new Result(baseline, 0, ROUTE_JAVA_BASELINE);
        byte[] nativeDigest = new byte[32];
        int ok;
        try {
            ok = NativeDsp.nativeSha256(input, offset, count, nativeDigest);
        } catch (LinkageError | RuntimeException ex) {
            ok = 0;
        }
        if (ok == 1 && same32(baseline, nativeDigest)) {
            return new Result(nativeDigest, 0, ROUTE_NATIVE_EXACT);
        }
        return new Result(baseline, 0, ROUTE_NATIVE_REJECTED_JAVA_FALLBACK);
    }

    static Result crc32(byte[] input, int offset, int count, boolean allowNative) {
        checkRange(input, offset, count);
        int baseline = crc32Java(input, offset, count);
        if (!allowNative) return new Result(null, baseline, ROUTE_JAVA_BASELINE);
        int[] nativeCrc = new int[1];
        int ok;
        try {
            ok = NativeDsp.nativeCrc32(input, offset, count, nativeCrc);
        } catch (LinkageError | RuntimeException ex) {
            ok = 0;
        }
        if (ok == 1 && nativeCrc[0] == baseline) {
            return new Result(null, nativeCrc[0], ROUTE_NATIVE_EXACT);
        }
        return new Result(null, baseline, ROUTE_NATIVE_REJECTED_JAVA_FALLBACK);
    }

    private static byte[] sha256Java(byte[] input, int offset, int count) {
        byte[] copy = new byte[count];
        for (int i = 0; i < count; ++i) copy[i] = input[offset + i];
        LowSha256 sha = new LowSha256();
        sha.update(copy, 0, copy.length);
        return sha.finish();
    }

    private static int crc32Java(byte[] input, int offset, int count) {
        int crc = -1;
        for (int i = 0; i < count; ++i) {
            crc ^= input[offset + i] & 255;
            for (int bit = 0; bit < 8; ++bit) {
                int mask = -(crc & 1);
                crc = (crc >>> 1) ^ (0xedb88320 & mask);
            }
        }
        return ~crc;
    }

    private static void checkRange(byte[] input, int offset, int count) {
        if (input == null) throw new IllegalArgumentException("byte-core input is null");
        if (offset < 0 || count < 0 || offset > input.length ||
                count > input.length - offset) {
            throw new IllegalArgumentException("invalid byte-core input range");
        }
    }

    private static boolean same32(byte[] left, byte[] right) {
        int diff = 0;
        for (int i = 0; i < 32; ++i) diff |= left[i] ^ right[i];
        return diff == 0;
    }

    static final class Result {
        final byte[] bytes;
        final int word;
        final int route;

        Result(byte[] bytes, int word, int route) {
            this.bytes = bytes;
            this.word = word;
            this.route = route;
        }
    }
}
