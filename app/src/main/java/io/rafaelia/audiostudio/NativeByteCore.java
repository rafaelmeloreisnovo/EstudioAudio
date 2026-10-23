/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 *
 * Opt-in byte-core consumer. Java remains the baseline; JNI may only replace
 * a result after exact native success. Any native rejection falls back closed.
 */
package io.rafaelia.audiostudio;

import java.io.IOException;

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

    static Result zipStored(String[] names, byte[][] payloads, boolean allowNative)
            throws IOException {
        byte[] baseline = zipJava(names, payloads);
        if (!allowNative) return new Result(baseline, 0, ROUTE_JAVA_BASELINE);

        FlattenedZipInput flat = flatten(names, payloads);
        byte[] nativeZip = new byte[baseline.length];
        int[] written = new int[1];
        int ok;
        try {
            ok = NativeDsp.nativeZip32Store(
                    flat.names, flat.nameOffsets, flat.nameSizes,
                    flat.payloads, flat.payloadOffsets, flat.payloadSizes,
                    names.length, nativeZip, written);
        } catch (LinkageError | RuntimeException ex) {
            ok = 0;
        }
        if (ok == 1 && written[0] == baseline.length && sameBytes(baseline, nativeZip)) {
            return new Result(nativeZip, 0, ROUTE_NATIVE_EXACT);
        }
        return new Result(baseline, 0, ROUTE_NATIVE_REJECTED_JAVA_FALLBACK);
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

    private static byte[] zipJava(String[] names, byte[][] payloads) throws IOException {
        checkEntries(names, payloads);
        FixedZipOutput stream = new FixedZipOutput(zipCapacity(names, payloads));
        RfaStoredZip zip = new RfaStoredZip(stream);
        for (int i = 0; i < names.length; ++i) zip.add(names[i], payloads[i]);
        zip.finish();
        return stream.exactBytes();
    }

    private static int zipCapacity(String[] names, byte[][] payloads) throws IOException {
        long bytes = 22L;
        for (int i = 0; i < names.length; ++i) {
            int nameSize = asciiPath(names[i]).length;
            long entryBytes = 30L + nameSize + payloads[i].length + 46L + nameSize;
            if (entryBytes > RfaStoredZip.MAX_ZIP_BYTES - bytes) {
                throw new IOException("ZIPRAF total size exceeded");
            }
            bytes += entryBytes;
        }
        return (int) bytes;
    }

    private static FlattenedZipInput flatten(String[] names, byte[][] payloads)
            throws IOException {
        int nameBytes = 0;
        int payloadBytes = 0;
        byte[][] asciiNames = new byte[names.length][];
        for (int i = 0; i < names.length; ++i) {
            asciiNames[i] = asciiPath(names[i]);
            nameBytes += asciiNames[i].length;
            payloadBytes += payloads[i].length;
        }

        FlattenedZipInput out = new FlattenedZipInput(names.length, nameBytes, payloadBytes);
        int nameAt = 0;
        int payloadAt = 0;
        for (int i = 0; i < names.length; ++i) {
            byte[] path = asciiNames[i];
            byte[] data = payloads[i];
            out.nameOffsets[i] = nameAt;
            out.nameSizes[i] = path.length;
            for (int j = 0; j < path.length; ++j) out.names[nameAt++] = path[j];
            out.payloadOffsets[i] = payloadAt;
            out.payloadSizes[i] = data.length;
            for (int j = 0; j < data.length; ++j) out.payloads[payloadAt++] = data[j];
        }
        return out;
    }

    private static void checkEntries(String[] names, byte[][] payloads) throws IOException {
        if (names == null || payloads == null || names.length != payloads.length) {
            throw new IOException("ZIPRAF entry arrays mismatch");
        }
        if (names.length > RfaStoredZip.MAX_ENTRIES) {
            throw new IOException("ZIPRAF entry count exceeded");
        }
        for (int i = 0; i < names.length; ++i) {
            if (payloads[i] == null || payloads[i].length > RfaStoredZip.MAX_ENTRY_BYTES) {
                throw new IOException("ZIPRAF embedded entry missing or too large");
            }
        }
    }

    private static byte[] asciiPath(String path) throws IOException {
        if (path == null || path.isEmpty() || path.length() > 255 ||
                path.charAt(0) == '/' || path.endsWith("/") ||
                path.contains("..") || path.contains("//")) {
            throw new IOException("ZIPRAF unsafe/invalid path");
        }
        byte[] out = new byte[path.length()];
        for (int i = 0; i < path.length(); ++i) {
            char c = path.charAt(i);
            boolean safe = (c >= 'a' && c <= 'z') ||
                    (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') ||
                    c == '_' || c == '-' || c == '.' || c == '/';
            if (!safe) throw new IOException("ZIPRAF non-ASCII or unsafe path");
            out[i] = (byte)c;
        }
        return out;
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

    private static boolean sameBytes(byte[] left, byte[] right) {
        if (left.length != right.length) return false;
        int diff = 0;
        for (int i = 0; i < left.length; ++i) diff |= left[i] ^ right[i];
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

    private static final class FlattenedZipInput {
        final byte[] names;
        final int[] nameOffsets;
        final int[] nameSizes;
        final byte[] payloads;
        final int[] payloadOffsets;
        final int[] payloadSizes;

        FlattenedZipInput(int count, int nameBytes, int payloadBytes) {
            names = new byte[nameBytes];
            nameOffsets = new int[count];
            nameSizes = new int[count];
            payloads = new byte[payloadBytes];
            payloadOffsets = new int[count];
            payloadSizes = new int[count];
        }
    }

    private static final class FixedZipOutput extends java.io.OutputStream {
        private final byte[] data;
        private int size;

        FixedZipOutput(int capacity) throws IOException {
            if (capacity < 0 || capacity > RfaStoredZip.MAX_ZIP_BYTES) {
                throw new IOException("ZIPRAF output capacity exceeded");
            }
            data = new byte[capacity];
        }

        @Override
        public void write(int value) throws IOException {
            if (size >= data.length) throw new IOException("ZIPRAF output overflow");
            data[size++] = (byte) value;
        }

        @Override
        public void write(byte[] source, int offset, int count) throws IOException {
            if (source == null || offset < 0 || count < 0 ||
                    offset > source.length || count > source.length - offset) {
                throw new IOException("ZIPRAF invalid output window");
            }
            if (count > data.length - size) throw new IOException("ZIPRAF output overflow");
            for (int i = 0; i < count; ++i) data[size + i] = source[offset + i];
            size += count;
        }

        byte[] exactBytes() throws IOException {
            if (size != data.length) throw new IOException("ZIPRAF output length mismatch");
            byte[] out = new byte[size];
            for (int i = 0; i < size; ++i) out[i] = data[i];
            return out;
        }
    }
}
