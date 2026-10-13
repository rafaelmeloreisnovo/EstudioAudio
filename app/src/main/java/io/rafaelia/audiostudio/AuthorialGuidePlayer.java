/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Hosted Android speaker boundary ONLY. The PCM generator is freestanding C.
 */
package io.rafaelia.audiostudio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.nio.charset.StandardCharsets;

final class AuthorialGuidePlayer {
    private volatile boolean stopRequested;
    private volatile boolean playing;
    private Thread worker;

    boolean isPlaying() { return playing; }

    synchronized boolean play(String text) {
        stop();
        if (worker != null && worker.isAlive()) return false;
        final byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        if (utf8.length == 0 || utf8.length > 2048) return false;
        try {
            if (NativeDsp.nativePtbrValidate(utf8) != 1) return false;
        } catch (LinkageError | RuntimeException unavailable) {
            return false;
        }
        stopRequested = false;
        playing = true;
        worker = new Thread(() -> renderToSpeaker(utf8), "rafaelia-authorial-guide");
        worker.start();
        return true;
    }

    private void renderToSpeaker(byte[] text) {
        AudioTrack local = null;
        try {
            int min = AudioTrack.getMinBufferSize(
                    16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (min <= 0) return;
            local = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(16000)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(Math.max(min, 4096))
                    .setTransferMode(AudioTrack.MODE_STREAM).build();
            if (local.getState() != AudioTrack.STATE_INITIALIZED) return;
            int[] state = new int[6];
            int[] timbre = TimbreProfileStore.snapshot();
            int[] toneState = new int[1];
            short[] block = new short[1024];
            local.play();
            while (!stopRequested) {
                int n = NativeDsp.nativePtbrRender(text, state, block);
                if (n <= 0) break;
                if (timbre != null &&
                        NativeDsp.nativeTimbreApply(block, n, timbre[0], toneState) != 1)
                    return;
                int offset = 0;
                while (offset < n && !stopRequested) {
                    int w = local.write(block, offset, n - offset, AudioTrack.WRITE_BLOCKING);
                    if (w <= 0) return;
                    offset += w;
                }
            }
        } catch (RuntimeException | UnsatisfiedLinkError ignored) {
            // A speaker/ABI failure is not evidence of freestanding core failure.
        } finally {
            if (local != null) {
                try { local.pause(); local.flush(); local.stop(); }
                catch (IllegalStateException ignored) {}
                local.release();
            }
            playing = false;
        }
    }

    synchronized void stop() {
        stopRequested = true;
        Thread prior = worker;
        if (prior != null && prior.isAlive() && prior != Thread.currentThread()) {
            prior.interrupt();
            try { prior.join(1200L); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        if (prior == null || !prior.isAlive()) {
            worker = null;
            playing = false;
        }
    }
}
