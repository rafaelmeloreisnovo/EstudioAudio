/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

import java.util.Arrays;
import java.io.IOException;
import java.io.InputStream;

/**
 * Strict bounded PCM16 mono WAV reader for user-selected test stimuli.
 * This does NOT calibrate SPL or verify a loudspeaker/microphone.
 */
final class WaveLabReferencePcm {
    static final int SAMPLE_RATE = 48000;
    static final int MIN_SECONDS = 3;
    static final int MAX_SECONDS = 30;
    static final int MAX_WAV_BYTES = SAMPLE_RATE * MAX_SECONDS * 2 + 65536;
    private WaveLabReferencePcm() {}

    static final class Parsed {
        final short[] pcm;
        final int sampleRate;
        Parsed(short[] pcm, int sampleRate) {
            this.pcm = pcm;
            this.sampleRate = sampleRate;
        }
    }
    private static int u16(byte[] b, int p) {
        return (b[p] & 255) | ((b[p + 1] & 255) << 8);
    }
    private static long u32(byte[] b, int p) {
        return ((long) u16(b, p) & 65535L) |
                (((long) u16(b, p + 2) & 65535L) << 16);
    }
    private static boolean tag(byte[] b, int p, String tag) {
        for (int i = 0; i < 4; i++)
            if ((b[p + i] & 255) != tag.charAt(i)) return false;
        return true;
    }

    static byte[] readBounded(InputStream source) throws IOException {
        if (source == null) throw new IOException("MISSING_SOURCE");
        // Fixed bounded arena; no extensible stream or unbounded allocation.
        byte[] arena = new byte[MAX_WAV_BYTES];
        int used = 0, zeroReads = 0;
        while (used < arena.length) {
            int read = source.read(arena, used, arena.length - used);
            if (read < 0) return Arrays.copyOf(arena, used);
            if (read == 0) {
                if (++zeroReads > 8) throw new IOException("STALL_READING_WAV");
                continue;
            }
            used += read;
            zeroReads = 0;
        }
        if (source.read() >= 0) throw new IOException("WAV_TOO_LARGE");
        return arena;
    }

    static Parsed parse(byte[] b) throws IOException {
        if (b == null || b.length < 44 || b.length > MAX_WAV_BYTES ||
                !tag(b, 0, "RIFF") || !tag(b, 8, "WAVE"))
            throw new IOException("UNSUPPORTED_WAV_CONTAINER");
        if (u32(b, 4) + 8L != b.length) throw new IOException("RIFF_SIZE_MISMATCH");
        int cursor = 12, fmtCount = 0, dataCount = 0;
        int format = -1, channels = -1, rate = -1, bits = -1, align = -1;
        int dataOffset = -1, dataBytes = -1, chunks = 0;
        while (cursor + 8 <= b.length) {
            if (++chunks > 128) throw new IOException("TOO_MANY_WAV_CHUNKS");
            long size = u32(b, cursor + 4);
            long payload = (long) cursor + 8;
            long end = payload + size;
            long paddedEnd = end + (size & 1L);
            if (size > MAX_WAV_BYTES || end > b.length || paddedEnd > b.length)
                throw new IOException("WAV_CHUNK_OUT_OF_BOUNDS");
            if (tag(b, cursor, "fmt ")) {
                if (++fmtCount != 1 || size < 16) throw new IOException("DUPLICATE_OR_SHORT_FMT");
                int pos = (int) payload;
                format = u16(b, pos);
                channels = u16(b, pos + 2);
                long rate32 = u32(b, pos + 4);
                rate = rate32 > Integer.MAX_VALUE ? -1 : (int) rate32;
                long byteRate = u32(b, pos + 8);
                align = u16(b, pos + 12);
                bits = u16(b, pos + 14);
                if (format != 1 || channels != 1 || rate != SAMPLE_RATE ||
                        bits != 16 || align != 2 || byteRate != (long) SAMPLE_RATE * 2)
                    throw new IOException("UNSUPPORTED_PCM_FORMAT");
            }
            if (tag(b, cursor, "data")) {
                if (++dataCount != 1 || (size & 1L) != 0)
                    throw new IOException("DUPLICATE_OR_ODD_DATA");
                dataOffset = (int) payload;
                dataBytes = (int) size;
            }
            cursor = (int) paddedEnd;
        }
        if (cursor != b.length || fmtCount != 1 || dataCount != 1)
            throw new IOException("WAV_INCOMPLETE");
        int samples = dataBytes / 2;
        if (samples < SAMPLE_RATE * MIN_SECONDS ||
                samples > SAMPLE_RATE * MAX_SECONDS)
            throw new IOException("WAV_DURATION_OUT_OF_RANGE");
        short[] pcm = new short[samples];
        for (int i = 0, p = dataOffset; i < samples; i++, p += 2) {
            pcm[i] = (short) u16(b, p);
        }
        return new Parsed(pcm, rate);
    }
}
