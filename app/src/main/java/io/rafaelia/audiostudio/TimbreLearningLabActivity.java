/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Android I/O boundary for on-device, opt-in bounded WAV timbre lab.
 * NO waveform is copied to filesystem, ZIPRAF, external API or telemetry.
 */
package io.rafaelia.audiostudio;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Only uncompressed RIFF/WAVE PCM16 mono: streamed from one granted content URI. */
public final class TimbreLearningLabActivity extends Activity {
    private static final long MAX_RIFF = 2097152L + 4096L;
    private static final int MAX_DATA = 2097152;
    private TextView status;
    private volatile boolean busy;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        layout.setBackgroundColor(Color.rgb(12, 17, 22));
        TextView title = label("RAFAELIA · LAB FÍSICO / TIMBRE EXPERIMENTAL");
        title.setTextSize(19);
        layout.addView(title);
        layout.addView(label("Compartilhe um WAV mono PCM16 da sua voz ou de voz com autorização. "
                + "O áudio entra por URI temporária: não gravamos nem enviamos a voz original."));
        layout.addView(label("Aprendizado limitado: energia, mudanças rápidas e cruzamentos de zero. "
                + "Não há clonagem, reconhecimento de locutor, prosódia ou TTS natural validado."));
        status = label("AGUARDANDO_WAV_COMPARTILHADO · voz-guia PT-BR ainda experimental");
        layout.addView(status);
        Button clear = button("Apagar perfil da memória");
        clear.setOnClickListener(v -> {
            TimbreProfileStore.clear();
            status.setText("PERFIL_APAGADO_DA_MEMÓRIA · sem tocar em arquivos anteriores");
        });
        layout.addView(clear);
        Button export = button("Exportar perfil ZIPRAF → Downloads/RAFAELIA_TTS_LAB");
        export.setOnClickListener(v -> {
            if (busy) return;
            int[] p = TimbreProfileStore.snapshot();
            if (p == null) {
                status.setText("EXPORT_BLOCKED · TOKEN_VAZIO_PERFIL");
                return;
            }
            busy = true;
            new Thread(() -> {
                boolean ok = false;
                try {
                    exportProfile(p);
                    ok = true;
                } catch (Exception ignored) {
                    // Never reveal a provider URI or source metadata in UI logs.
                }
                final boolean pass = ok;
                runOnUiThread(() -> {
                    busy = false;
                    status.setText(pass ? "PROFILE_ZIPRAF_EXPORTED · sem voz bruta" :
                            "EXPORT_FAILED · Downloads/RAFAELIA_TTS_LAB");
                });
            }, "rafaelia-timbre-export").start();
        });
        layout.addView(export);
        scroll.addView(layout);
        setContentView(scroll);
        final Intent shared = getIntent();
        if (Intent.ACTION_SEND.equals(shared.getAction())) {
            Uri uri = shared.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri == null && shared.getClipData() != null &&
                    shared.getClipData().getItemCount() > 0) {
                uri = shared.getClipData().getItemAt(0).getUri();
            }
            if (uri != null && "content".equals(uri.getScheme())) {
                analyze(uri);
            } else {
                status.setText("INPUT_REJECTED · compartilhar somente content:// de WAV");
            }
        }
    }

    private TextView label(String value) {
        TextView label = new TextView(this);
        label.setTextColor(Color.rgb(222, 232, 237));
        label.setPadding(4, 8, 4, 12);
        label.setText(value);
        return label;
    }
    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        return b;
    }

    private void analyze(Uri uri) {
        if (busy) return;
        busy = true;
        status.setText("ANALISANDO_APENAS_MEMÓRIA · limite 2 MiB · sem persistir WAV");
        new Thread(() -> {
            String result;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new IOException("source");
                int[] p = analyzeWav(in);
                TimbreProfileStore.put(p[0], p[1], p[2], p[3], p[4]);
                result = "PERFIL_EXPERIMENTAL_PRONTO · mixQ8=" + p[0] +
                        " meanAbs=" + p[1] + " zeroCrossK=" + p[2] +
                        " samples=" + p[4] +
                        " · reproduzir voz-guia no EstudioAudio para comparação";
            } catch (Exception ignored) {
                result = "ANALYSIS_REJECTED · WAV PCM16 mono (8–48 kHz) íntegro, "
                        + "não silencioso, máximo 2 MiB, URI autorizada";
            }
            final String outcome = result;
            runOnUiThread(() -> {
                busy = false;
                status.setText(outcome);
            });
        }, "rafaelia-timbre-import").start();
    }

    static int[] analyzeWav(InputStream in) throws Exception {
        byte[] header = new byte[12];
        readExact(in, header, 12);
        if (!eq(header, 0, "RIFF") || !eq(header, 8, "WAVE"))
            throw new IOException("not PCM WAVE");
        long riff = u32(header, 4);
        if (riff < 36 || riff > MAX_RIFF) throw new IOException("wav size");
        long remaining = riff - 4L;
        int rate = 0;
        boolean gotFmt = false;
        long[] stats = new long[6];
        byte[] buf = new byte[4096];
        short[] samples = new short[2048];
        for (int chunk = 0; chunk < 16 && remaining >= 8; ++chunk) {
            byte[] ch = new byte[8];
            readExact(in, ch, 8);
            remaining -= 8L;
            long size = u32(ch, 4);
            long padded = size + (size & 1L);
            if (padded > remaining || size > MAX_DATA) throw new IOException("chunk size");
            if (eq(ch, 0, "fmt ")) {
                if (gotFmt || size < 16 || size > 64) throw new IOException("fmt length");
                byte[] fmt = new byte[(int)size];
                readExact(in, fmt, (int)size);
                rate = (int)u32(fmt, 4);
                if (u16(fmt, 0) != 1 || u16(fmt, 2) != 1 ||
                        rate < 8000 || rate > 48000 ||
                        u16(fmt, 14) != 16 || u16(fmt, 12) != 2 ||
                        u32(fmt, 8) != (long)rate * 2L)
                    throw new IOException("unsupported pcm");
                gotFmt = true;
            } else if (eq(ch, 0, "data")) {
                if (!gotFmt || size < 16000 || (size & 1L) != 0)
                    throw new IOException("data length");
                int left = (int)size;
                while (left > 0) {
                    int n = Math.min(left, buf.length);
                    readExact(in, buf, n);
                    for (int j = 0; j < n / 2; ++j) {
                        samples[j] = (short)((buf[j*2] & 255) | (buf[j*2+1] << 8));
                    }
                    if (NativeDsp.nativeTimbreAccumulate(samples, n / 2, stats) != 1)
                        throw new IOException("native profile");
                    left -= n;
                }
                int[] profile = new int[3];
                if (NativeDsp.nativeTimbreFinalize(stats, profile) != 1)
                    throw new IOException("silent or invalid profile");
                return new int[]{profile[0],profile[1],profile[2],rate,(int)stats[0]};
            } else {
                skipExact(in, size, buf);
            }
            if ((size & 1L) != 0) {
                readExact(in, buf, 1);
            }
            remaining -= padded;
        }
        throw new IOException("data chunk absent");
    }

    private static void readExact(InputStream in, byte[] dst, int length)
            throws IOException {
        int i = 0;
        while (i < length) {
            int n = in.read(dst, i, length - i);
            if (n <= 0) throw new IOException("truncated stream");
            i += n;
        }
    }
    private static void skipExact(InputStream in, long n, byte[] buf)
            throws IOException {
        while (n > 0) {
            int take = (int)Math.min(n, buf.length);
            readExact(in, buf, take);
            n -= take;
        }
    }
    private static int u16(byte[] b, int i) {
        return (b[i] & 255) | ((b[i + 1] & 255) << 8);
    }
    private static long u32(byte[] b, int i) {
        return (b[i] & 255L) | ((b[i + 1] & 255L) << 8) |
                ((b[i + 2] & 255L) << 16) | ((b[i + 3] & 255L) << 24);
    }
    private static boolean eq(byte[] b, int off, String ascii) {
        for (int i=0; i<4; ++i) if (b[off+i] != (byte)ascii.charAt(i)) return false;
        return true;
    }

    private void exportProfile(int[] p) throws Exception {
        if (Build.VERSION.SDK_INT < 29) throw new IOException("MediaStore needs API29");
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,
                "rafaelia_timbre_profile_" + System.currentTimeMillis() + ".zipraf.zip");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/RAFAELIA_TTS_LAB");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri target = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (target == null) throw new IOException("create Downloads entry");
        boolean committed = false;
        try {
            try (OutputStream out = getContentResolver().openOutputStream(target)) {
                if (out == null) throw new IOException("output");
                RfaStoredZip zip = new RfaStoredZip(out);
                String profile = "schema=rafaelia.timbre_profile.v1\n"
                        + "mix_q8=" + p[0] + "\nmean_abs=" + p[1] +
                        "\nzero_cross_per_1000=" + p[2] +
                        "\nsample_rate=" + p[3] + "\nframes=" + p[4] + "\n";
                String receipt = "privacy=DERIVED_FEATURES_ONLY\n"
                        + "original_audio=NOT_STORED\n"
                        + "core=FREESTANDING_C\n"
                        + "user_consent=SHARE_TARGET_SELECTED\n"
                        + "voice_identity=NOT_INFERRED\n"
                        + "natural_tts=NOT_VALIDATED\n"
                        + "speaker_physical=NOT_RUN\n";
                zip.add("profile.v1.txt", profile.getBytes(StandardCharsets.UTF_8));
                zip.add("receipt.v1.txt", receipt.getBytes(StandardCharsets.UTF_8));
                zip.finish();
            }
            ContentValues done = new ContentValues();
            done.put(MediaStore.MediaColumns.IS_PENDING, 0);
            if (getContentResolver().update(target, done, null, null) != 1)
                throw new IOException("publish");
            committed = true;
        } finally {
            if (!committed) getContentResolver().delete(target, null, null);
        }
    }
}
