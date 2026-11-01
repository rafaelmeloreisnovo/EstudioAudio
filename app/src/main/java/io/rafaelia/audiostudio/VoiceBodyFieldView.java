/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * UI-only mapping: observed Goertzel powers and relative motion -> light filaments.
 * This rendering is NOT a measured magnetic field, voice emotion or heart rhythm.
 */
package io.rafaelia.audiostudio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Reuses NativeDsp.nativeSpectrum's 16 Goertzel bands; no additional recorder. */
final class VoiceBodyFieldView extends View {
    private static final int BANDS = 16;
    private final long[] power = new long[BANDS];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path filament = new Path();
    private VoiceBodyMotionProbe.Snapshot motion =
            VoiceBodyMotionProbe.Snapshot.absent("NOT_STARTED");
    private boolean running;
    private long audioSamples;

    VoiceBodyFieldView(Context context) {
        super(context);
        setMinimumHeight((int)(228f * getResources().getDisplayMetrics().density));
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    void showFrame(long[] spectralPower, int count, long recordedSamples,
                   VoiceBodyMotionProbe.Snapshot sensor) {
        for (int i = 0; i < BANDS; ++i) {
            power[i] = spectralPower != null && i < count &&
                    i < spectralPower.length && spectralPower[i] > 0L
                    ? spectralPower[i] : 0L;
        }
        audioSamples = recordedSamples;
        motion = sensor == null ?
                VoiceBodyMotionProbe.Snapshot.absent("SENSOR_NOT_PRESENT") : sensor;
        running = true;
        invalidate();
    }

    void finish() {
        running = false;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth(), height = getHeight();
        canvas.drawColor(Color.rgb(6, 14, 24));
        float density = getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(205, 226, 236));
        paint.setTextSize(13f * density);
        canvas.drawText("VOICE × BODY  |  CAMPO ESPECTRAL RELATIVO",
                10f * density, 23f * density, paint);

        final long max = peakBand();
        final boolean observed = "OBSERVED_UNPROMOTED".equals(motion.state);
        final double motionLevel = observed
                ? Math.min(1.0, motion.rmsDeltaMs2 / 2.0) : 0.0;
        float left = width * 0.37f, right = width * 0.63f;
        float cy = height * 0.53f;
        // Mirrored bends emulate field-line geometry, not a physical B-field.
        for (int band = BANDS - 1; band >= 0; --band) {
            double energy = max > 0L ? Math.sqrt((double)power[band] / max) : 0.0;
            float spread = (16f + band * 3.8f) * density;
            float rise = (7f + band * 4.1f) * density;
            float bend = (float)(energy * 17f + motionLevel * 11f) * density;
            for (int sign = -1; sign <= 1; sign += 2) {
                filament.reset();
                filament.moveTo(left, cy);
                filament.cubicTo(left - spread - bend, cy + sign * (rise + bend),
                        right + spread + bend, cy + sign * (rise + bend),
                        right, cy);
                int alpha = 48 + (int)(175.0 * energy);
                paint.setColor(sign < 0 ? Color.argb(alpha, 74, 218, 247) :
                        Color.argb(alpha, 255, 126, 185));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth((0.7f + (float)energy * 1.2f) * density);
                canvas.drawPath(filament, paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(178, 205, 218));
        paint.setTextSize(10f * density);
        float base = height - 36f * density;
        canvas.drawText("FONTE: PCM 48 kHz → C Goertzel/16  |  amostras=" +
                        audioSamples, 10f * density, base, paint);
        String sensorText = observed ?
                "μΔ RMS=" + rounded(motion.rmsDeltaMs2, 3) + " m/s²" +
                "  Hz≈" + rounded(motion.effectiveHz, 1) +
                "  idade=" + motion.ageMs + " ms" :
                "SENSOR=" + motion.state;
        canvas.drawText(sensorText, 10f * density, base + 13f * density, paint);
        canvas.drawText((running ? "OBSERVANDO" : "PARADO") +
                " | MOVIMENTO ≠ PULSO ≠ ESTRESSE | SEM FASE CALIBRADA",
                10f * density, base + 26f * density, paint);
    }

    private long peakBand() {
        long max = 0L;
        for (long value : power) if (value > max) max = value;
        return max;
    }

    private static String rounded(double value, int decimals) {
        if (!Double.isFinite(value)) return "TOKEN_VAZIO";
        long scale = decimals == 3 ? 1000L : 10L;
        long units = Math.round(value * scale);
        return (units / scale) + "." +
                (decimals == 3 ?
                    String.format(java.util.Locale.US, "%03d", units % scale) :
                    String.format(java.util.Locale.US, "%01d", units % scale));
    }
}
