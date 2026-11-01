/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Bounded, opt-in Android sensor observation. No telemetry, exports or diagnosis.
 */
package io.rafaelia.audiostudio;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/** Shares the existing Android sensor boundary; does not modify the evidence probe. */
final class VoiceBodyMotionProbe implements SensorEventListener {
    static final class Snapshot {
        final String state;
        final int samples;
        final double effectiveHz;
        final double rmsDeltaMs2;
        final double peakDeltaMs2;
        final long ageMs;

        Snapshot(String state, int samples, double hz, double rms, double peak, long ageMs) {
            this.state = state;
            this.samples = samples;
            this.effectiveHz = hz;
            this.rmsDeltaMs2 = rms;
            this.peakDeltaMs2 = peak;
            this.ageMs = ageMs;
        }

        static Snapshot absent(String reason) {
            return new Snapshot("TOKEN_VAZIO_" + reason, 0, 0.0, 0.0, 0.0, -1L);
        }
    }

    private static final int CAPACITY = 256;
    private static final long WINDOW_NS = 1_000_000_000L;
    private final SensorManager manager;
    private final Sensor sensor;
    private final long[] timesNs = new long[CAPACITY];
    private final double[] deltaSquares = new double[CAPACITY];
    private int next;
    private int size;
    private boolean active;
    private boolean previousValid;
    private float previousX, previousY, previousZ;

    private VoiceBodyMotionProbe(SensorManager manager, Sensor sensor) {
        this.manager = manager;
        this.sensor = sensor;
    }

    static VoiceBodyMotionProbe begin(Context context) {
        SensorManager manager = (SensorManager)
                context.getSystemService(Context.SENSOR_SERVICE);
        if (manager == null) return null;
        Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensor == null) return null;
        VoiceBodyMotionProbe probe = new VoiceBodyMotionProbe(manager, sensor);
        probe.active = true;
        if (!manager.registerListener(probe, sensor, SensorManager.SENSOR_DELAY_GAME)) {
            probe.active = false;
            return null;
        }
        return probe;
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (!active || event == null || event.values == null ||
                event.values.length < 3) return;
        final float x = event.values[0], y = event.values[1], z = event.values[2];
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) return;
        if (previousValid) {
            double dx = (double)x - previousX, dy = (double)y - previousY;
            double dz = (double)z - previousZ;
            double square = dx * dx + dy * dy + dz * dz;
            if (Double.isFinite(square) && square >= 0.0) {
                timesNs[next] = event.timestamp;
                deltaSquares[next] = square;
                next = (next + 1) % CAPACITY;
                if (size < CAPACITY) ++size;
            }
        }
        previousX = x;
        previousY = y;
        previousZ = z;
        previousValid = true;
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // An Android accuracy notification is not a physical calibration.
    }

    /** Last-second temporal differences, in original acceleration units. */
    Snapshot snapshot() {
        if (!active || size == 0) return Snapshot.absent("SENSOR_SAMPLES");
        long nowNs = SystemClock.elapsedRealtimeNanos();
        long cutoffNs = nowNs - WINDOW_NS;
        long first = 0L, last = 0L;
        double sum = 0.0, peak = 0.0;
        int count = 0;
        for (int j = 0; j < size; ++j) {
            int idx = (next - size + j + CAPACITY) % CAPACITY;
            long timestamp = timesNs[idx];
            if (timestamp < cutoffNs || timestamp > nowNs) continue;
            if (count == 0) first = timestamp;
            last = timestamp;
            sum += deltaSquares[idx];
            if (deltaSquares[idx] > peak) peak = deltaSquares[idx];
            ++count;
        }
        if (count < 2 || last <= first)
            return Snapshot.absent("INSUFFICIENT_WINDOW");
        double hz = (count - 1) * 1_000_000_000.0 / (last - first);
        long ageMs = (nowNs - last) / 1_000_000L;
        return new Snapshot("OBSERVED_UNPROMOTED", count, hz,
                Math.sqrt(sum / count), Math.sqrt(peak), ageMs);
    }

    void stop() {
        if (!active) return;
        active = false;
        manager.unregisterListener(this);
    }
}
