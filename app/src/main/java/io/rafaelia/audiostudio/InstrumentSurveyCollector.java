/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrength;
import android.telephony.TelephonyManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Foreground platform edge. Uses only public APIs, never changes radio settings.
 * Snapshot radio data can be cached; original monotonic timestamps are retained.
 * Coordinates describe the handset in WGS84, not a tower or acoustic source.
 */
public final class InstrumentSurveyCollector implements SensorEventListener, LocationListener {
    public interface Listener {
        void onSample(String id, long epochMs, long elapsedNs, long sourceNs,
                      double value, double accuracy, int sector, String state);
    }

    private static final int MAX_SENSOR_ITEMS = 64;
    private static final int MAX_CAPABILITY_ITEMS = 32;
    private final Context context;
    private final Listener listener;
    private final SensorManager sensors;
    private final LocationManager locations;
    private final ArrayList<InstrumentSurveyModel.Item> items = new ArrayList<>();
    private final ArrayList<SensorSource> sensorSources = new ArrayList<>();
    private final Set<String> selected = new HashSet<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private boolean gpsListening;
    private boolean gnssListening;
    private int currentSector = -1;
    private long poseNs = -1;
    private String servingKey;
    private String wifiKey;
    private boolean cellChanged;
    private boolean wifiChanged;
    private int unsupportedSensors;
    private int omittedSensors;
    private long periodNs;
    private String lifecycle = "NOT_RUN";
    private String serviceState = "NOT_OBSERVED";

    private static final class SensorSource {
        final Sensor sensor;
        final String prefix;
        final String[] axes;
        long lastNs = -1;
        boolean listening;
        SensorSource(Sensor sensor, String prefix, String[] axes) {
            this.sensor = sensor; this.prefix = prefix; this.axes = axes;
        }
    }

    private final GnssStatus.Callback gnssCallback = new GnssStatus.Callback() {
        @Override public void onSatelliteStatusChanged(GnssStatus status) {
            if (!running) return;
            long now = SystemClock.elapsedRealtimeNanos();
            int count = status.getSatelliteCount(), used = 0, valid = 0;
            double cn0 = 0;
            for (int i = 0; i < Math.min(count, 256); ++i) {
                if (status.usedInFix(i)) ++used;
                float value = status.getCn0DbHz(i);
                if (Float.isFinite(value) && value > 0) { cn0 += value; ++valid; }
            }
            emit("gnss.satellites", now, count, Double.NaN, "OBSERVED");
            emit("gnss.used", now, used, Double.NaN, count <= 256 ? "OBSERVED" : "TRUNCATED");
            emit("gnss.mean_cn0", now, valid > 0 ? cn0 / valid : Double.NaN,
                    Double.NaN, count > 256 ? "TRUNCATED" : valid > 0 ? "OBSERVED" : "NOT_OBSERVED");
        }
    };

    public InstrumentSurveyCollector(Context context, Listener listener) {
        if (context == null || listener == null) throw new IllegalArgumentException("context/listener");
        this.context = context.getApplicationContext();
        this.listener = listener;
        sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        locations = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        discoverSensors();
        add("location.latitude", "Localização", "GPS / WGS84", "Latitude", "degree");
        add("location.longitude", "Localização", "GPS / WGS84", "Longitude", "degree");
        add("location.altitude", "Localização", "GPS / WGS84", "Altitude elipsoidal", "m");
        add("location.accuracy", "Localização", "GPS / WGS84", "Precisão horizontal estimada", "m");
        add("location.speed", "Localização", "GPS / WGS84", "Velocidade", "m/s");
        add("location.bearing", "Localização", "GPS / WGS84", "Rumo do deslocamento", "degree");
        add("gnss.satellites", "Localização", "GNSS", "Satélites reportados", "count");
        add("gnss.used", "Localização", "GNSS", "Satélites usados no fix", "count");
        add("gnss.mean_cn0", "Localização", "GNSS", "C/N0 médio reportado", "dB-Hz");
        add("cell.cid", "Rádio", "Celular / conexão servidora", "Cell ID / NCI", "id");
        add("cell.rat", "Rádio", "Celular / conexão servidora", "Tecnologia de rádio Android", "id");
        add("cell.mcc", "Rádio", "Celular / conexão servidora", "Código do país MCC", "id");
        add("cell.mnc", "Rádio", "Celular / conexão servidora", "Código da rede MNC", "id");
        add("cell.mnc_digits", "Rádio", "Celular / conexão servidora", "Comprimento do MNC (preserva zeros)", "id");
        add("cell.area", "Rádio", "Celular / conexão servidora", "Área LAC / TAC", "id");
        add("cell.pci", "Rádio", "Celular / conexão servidora", "PCI / PSC / BSIC", "id");
        add("cell.channel", "Rádio", "Celular / conexão servidora", "Canal ARFCN", "id");
        add("cell.dbm", "Rádio", "Celular / conexão servidora", "Sinal da célula servidora", "dBm");
        add("wifi.rssi", "Rádio", "Wi-Fi / cache", "RSSI da rede de referência", "dBm");
        add("wifi.frequency", "Rádio", "Wi-Fi / cache", "Frequência da rede de referência", "MHz");
        add("wifi.count", "Rádio", "Wi-Fi / cache", "Redes reportadas no cache", "count");
        add("bluetooth.enabled", "Rádio", "Bluetooth / adaptador", "Adaptador habilitado", "boolean");
        add("clock.elapsed_ms", "Sistema", "Relógio", "Tempo desde inicialização", "ms");
        add("capability.servicemode", "Capacidades", "Diagnóstico do fabricante", "ServiceMode: sem API pública", "status");
        add("capability.tower_database", "Capacidades", "Base geográfica", "Coordenadas de torres: base não conectada", "status");
    }

    private void add(String id, String family, String group, String label, String unit) {
        items.add(new InstrumentSurveyModel.Item(id, family, group, label, unit));
    }

    private void discoverSensors() {
        if (sensors == null) return;
        int itemCount = 0, index = 0, capabilityCount = 0;
        for (Sensor sensor : sensors.getSensorList(Sensor.TYPE_ALL)) {
            int type = sensor.getType();
            String unit = unit(type);
            String[] axes = axes(type);
            if (unit == null || axes == null) {
                ++unsupportedSensors;
                if (capabilityCount < MAX_CAPABILITY_ITEMS) {
                    add("capability.sensor." + capabilityCount++, "Capacidades", "Sensores não amostrados",
                            safeName(sensor.getName()) + " (tipo " + type + ")", "status");
                } else ++omittedSensors;
                continue;
            }
            if (itemCount + axes.length > MAX_SENSOR_ITEMS) { ++omittedSensors; continue; }
            String prefix = "sensor." + index++ + ".";
            String group = safeName(sensor.getName()) + " (#" + index + ")";
            sensorSources.add(new SensorSource(sensor, prefix, axes));
            for (String axis : axes) add(prefix + axis, "Sensores", group, axis, unit);
            itemCount += axes.length;
        }
    }

    private static String safeName(String name) {
        if (name == null || name.isEmpty()) return "Sensor sem nome";
        String clean = name.replace('\0', ' ').replace('\r', ' ').replace('\n', ' ');
        return clean.length() > 96 ? clean.substring(0, 96) : clean;
    }

    private static String[] axes(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER: case Sensor.TYPE_GYROSCOPE:
            case Sensor.TYPE_MAGNETIC_FIELD: case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return new String[]{"x", "y", "z"};
            case Sensor.TYPE_ROTATION_VECTOR: case Sensor.TYPE_GAME_ROTATION_VECTOR:
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return new String[]{"x", "y", "z", "w"};
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED: case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
            case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                return new String[]{"x", "y", "z", "bias_x", "bias_y", "bias_z"};
            case Sensor.TYPE_LIGHT: case Sensor.TYPE_PRESSURE: case Sensor.TYPE_PROXIMITY:
            case Sensor.TYPE_AMBIENT_TEMPERATURE: case Sensor.TYPE_RELATIVE_HUMIDITY:
                return new String[]{"value"};
            default: return null;
        }
    }

    private static String unit(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER: case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
            case Sensor.TYPE_GRAVITY: case Sensor.TYPE_LINEAR_ACCELERATION: return "m/s2";
            case Sensor.TYPE_GYROSCOPE: case Sensor.TYPE_GYROSCOPE_UNCALIBRATED: return "rad/s";
            case Sensor.TYPE_MAGNETIC_FIELD: case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED: return "uT";
            case Sensor.TYPE_ROTATION_VECTOR: case Sensor.TYPE_GAME_ROTATION_VECTOR:
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR: return "1";
            case Sensor.TYPE_LIGHT: return "lux";
            case Sensor.TYPE_PRESSURE: return "hPa";
            case Sensor.TYPE_PROXIMITY: return "cm";
            case Sensor.TYPE_AMBIENT_TEMPERATURE: return "C";
            case Sensor.TYPE_RELATIVE_HUMIDITY: return "%";
            default: return null;
        }
    }

    public List<InstrumentSurveyModel.Item> inventory() { return new ArrayList<>(items); }

    public String status() {
        return "session=" + lifecycle + "\nplatform=" + serviceState
                + "\nprotected_or_unsupported_sensors=" + unsupportedSensors
                + "\ninventory_limit_omitted_sensors=" + omittedSensors
                + "\nsensor_accuracy=ANDROID_STATUS_NOT_PHYSICAL_CALIBRATION"
                + "\npose=8_DEVICE_YAW_SECTORS_ROTATION_VECTOR_SELECTED_AND_FRESH_ONLY"
                + "\ngnss_time=CALLBACK_RECEIPT_TIME_NO_RAW_MEASUREMENT_TIMESTAMP"
                + "\ncell=FIRST_REGISTERED_PUBLIC_IDENTITY_HANDOVER_REQUIRES_NEW_SESSION"
                + "\ncell_rat_codes=GSM:16,UMTS:3,LTE:13,NR:20;MNC_RESTORE_LEADING_ZEROS_USING_mnc_digits"
                + "\nwifi=CACHE_ONLY_FIRST_REFERENCE_NETWORK_NO_ACTIVE_SCAN"
                + "\nlocation_accuracy=HORIZONTAL_METRES_EXCEPT_ALTITUDE_VERTICAL_METRES"
                + "\nservice_mode=NOT_PUBLIC_API\ntower_coordinates=NOT_AVAILABLE"
                + "\ngeospatial_database=NOT_CONNECTED\nphysical_validation=NOT_RUN";
    }

    private boolean has(String permission) {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean wants(String prefix) {
        for (String id : selected) if (id.startsWith(prefix)) return true;
        return false;
    }

    public void start(Set<String> selection, int periodUs) {
        if (selection == null || periodUs < 20000 || periodUs > 1000000)
            throw new IllegalArgumentException("period_us must be 20000..1000000");
        stop();
        selected.clear();
        for (InstrumentSurveyModel.Item item : items) if (selection.contains(item.id)) selected.add(item.id);
        servingKey = null; wifiKey = null; cellChanged = false; wifiChanged = false;
        periodNs = periodUs * 1000L;
        currentSector = -1; poseNs = -1;
        running = true; lifecycle = "FOREGROUND_RUNNING"; serviceState = "OBSERVING_SELECTED_ONLY";
        for (SensorSource source : sensorSources) {
            source.lastNs = -1;
            if (!wants(source.prefix)) continue;
            try {
                source.listening = sensors.registerListener(this, source.sensor, periodUs, handler);
                if (!source.listening) missing(source.prefix, "LISTENER_REGISTRATION_FAILED");
            } catch (SecurityException | IllegalArgumentException e) {
                source.listening = false;
                missing(source.prefix, "PERMISSION_OR_SENSOR_UNAVAILABLE");
            }
        }
        if (wants("location.") || wants("gnss.")) startLocation();
        missing("capability.sensor.", "PROTECTED_OR_UNSUPPORTED_SENSOR");
        missing("capability.servicemode", "NOT_PUBLIC_API");
        missing("capability.tower_database", "DATABASE_NOT_CONNECTED");
        snapshot();
    }

    private void startLocation() {
        if (!has(Manifest.permission.ACCESS_FINE_LOCATION)) {
            missing("location.", "PRECISE_PERMISSION_REQUIRED");
            missing("gnss.", "PRECISE_PERMISSION_REQUIRED"); return;
        }
        if (locations == null) {
            missing("location.", "SERVICE_UNAVAILABLE"); missing("gnss.", "SERVICE_UNAVAILABLE"); return;
        }
        try {
            if (!locations.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                missing("location.", "PROVIDER_DISABLED"); missing("gnss.", "PROVIDER_DISABLED"); return;
            }
            locations.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper());
            gpsListening = true;
            if (wants("gnss.")) {
                gnssListening = locations.registerGnssStatusCallback(gnssCallback, handler);
                if (!gnssListening) missing("gnss.", "LISTENER_REGISTRATION_FAILED");
            }
        } catch (SecurityException | IllegalArgumentException | UnsupportedOperationException e) {
            missing("location.", "PERMISSION_OR_PROVIDER_UNAVAILABLE");
            missing("gnss.", "PERMISSION_OR_PROVIDER_UNAVAILABLE");
        }
    }

    public void snapshot() {
        if (!running) return;
        long now = SystemClock.elapsedRealtimeNanos();
        emit("clock.elapsed_ms", now, now / 1000000.0, Double.NaN, "OBSERVED");
        if (wants("location.")) {
            if (!has(Manifest.permission.ACCESS_FINE_LOCATION)) missing("location.", "PRECISE_PERMISSION_REQUIRED");
            else try {
                Location fix = locations == null ? null : locations.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (fix != null) onLocationChanged(fix); else missing("location.", "NO_FIX");
            } catch (SecurityException | IllegalArgumentException e) { missing("location.", "PERMISSION_OR_PROVIDER_UNAVAILABLE"); }
        }
        if (wants("wifi.")) readWifi();
        if (wants("cell.")) readCell();
        if (wants("bluetooth.")) readBluetooth();
    }

    public void stop() {
        running = false;
        if (sensors != null) sensors.unregisterListener(this);
        for (SensorSource source : sensorSources) source.listening = false;
        if (locations != null) {
            if (gpsListening) try { locations.removeUpdates(this); } catch (SecurityException ignored) { }
            if (gnssListening) try { locations.unregisterGnssStatusCallback(gnssCallback); } catch (SecurityException ignored) { }
        }
        gpsListening = false; gnssListening = false; currentSector = -1; poseNs = -1;
        lifecycle = "STOPPED";
    }

    public int sector() {
        long age = SystemClock.elapsedRealtimeNanos() - poseNs;
        return poseNs >= 0 && age >= 0 && age <= 2000000000L ? currentSector : -1;
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (!running) return;
        for (SensorSource source : sensorSources) {
            if (source.sensor != event.sensor || !source.listening) continue;
            // Platform rate is a hint; bound delivery to the requested software rate too.
            long interval = event.timestamp - source.lastNs;
            if (source.lastNs >= 0 && interval > 0 && interval < periodNs) return;
            String state = event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ? "UNRELIABLE" : "OBSERVED";
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && event.values.length >= 3) {
                if ("OBSERVED".equals(state) && Float.isFinite(event.values[0])
                        && Float.isFinite(event.values[1]) && Float.isFinite(event.values[2])
                        && (event.values.length < 4 || Float.isFinite(event.values[3]))) {
                    float[] matrix = new float[9], angles = new float[3];
                    SensorManager.getRotationMatrixFromVector(matrix, event.values);
                    SensorManager.getOrientation(matrix, angles);
                    double yaw = (Math.toDegrees(angles[0]) + 360.0) % 360.0;
                    currentSector = Double.isFinite(yaw) ? (int)(yaw / 45.0) : -1;
                    poseNs = event.timestamp;
                } else { currentSector = -1; poseNs = -1; }
            }
            source.lastNs = event.timestamp;
            for (int i = 0; i < source.axes.length; ++i) {
                boolean available = i < event.values.length;
                emit(source.prefix + source.axes[i], event.timestamp,
                        available ? event.values[i] : Double.NaN, Double.NaN,
                        available ? state : "VALUE_NOT_REPORTED");
            }
            break;
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override public void onLocationChanged(Location fix) {
        if (!running || !LocationManager.GPS_PROVIDER.equals(fix.getProvider())) return;
        if (!has(Manifest.permission.ACCESS_FINE_LOCATION)) {
            missing("location.", "PRECISE_PERMISSION_REQUIRED"); return;
        }
        long source = fix.getElapsedRealtimeNanos();
        double accuracy = fix.hasAccuracy() ? fix.getAccuracy() : Double.NaN;
        // A location without uncertainty must not qualify for calibrated quality thresholds.
        String state = fix.hasAccuracy() ? "OBSERVED" : "ACCURACY_NOT_REPORTED";
        if (fix.isFromMockProvider()) state = "MOCK_LOCATION";
        emit("location.latitude", source, fix.getLatitude(), accuracy, state);
        emit("location.longitude", source, fix.getLongitude(), accuracy, state);
        emit("location.accuracy", source, accuracy, accuracy, state);
        emit("location.altitude", source, fix.hasAltitude() ? fix.getAltitude() : Double.NaN,
                fix.hasVerticalAccuracy() ? fix.getVerticalAccuracyMeters() : Double.NaN,
                !fix.hasAltitude() ? "VALUE_NOT_REPORTED" : !fix.hasVerticalAccuracy() ? "ACCURACY_NOT_REPORTED" : state);
        // Positional quality field remains in meters; speed/bearing accuracy has different units.
        emit("location.speed", source, fix.hasSpeed() ? fix.getSpeed() : Double.NaN,
                accuracy, fix.hasSpeed() ? state : "VALUE_NOT_REPORTED");
        emit("location.bearing", source, fix.hasBearing() ? fix.getBearing() : Double.NaN,
                accuracy, fix.hasBearing() ? state : "VALUE_NOT_REPORTED");
    }

    @Override public void onProviderDisabled(String provider) {
        if (running && LocationManager.GPS_PROVIDER.equals(provider)) {
            missing("location.", "PROVIDER_DISABLED"); missing("gnss.", "PROVIDER_DISABLED");
        }
    }
    @Override public void onProviderEnabled(String provider) { }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }

    private void readWifi() {
        if (!has(Manifest.permission.ACCESS_FINE_LOCATION)) { missing("wifi.", "PRECISE_PERMISSION_REQUIRED"); return; }
        try {
            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> cache = manager == null ? null : manager.getScanResults();
            if (cache == null || cache.isEmpty()) { missing("wifi.", "NO_CACHED_RESULTS"); return; }
            ScanResult strongest = null;
            long newest = -1;
            for (int i = 0; i < Math.min(cache.size(), 256); ++i) {
                ScanResult candidate = cache.get(i);
                newest = Math.max(newest, candidate.timestamp);
                if (strongest == null || candidate.level > strongest.level) strongest = candidate;
            }
            emit("wifi.count", microsToNanos(newest), cache.size(), Double.NaN, "OBSERVED");
            if (strongest == null || strongest.BSSID == null || strongest.BSSID.isEmpty()
                    || "02:00:00:00:00:00".equals(strongest.BSSID)) {
                missing("wifi.rssi", "IDENTITY_NOT_REPORTED"); missing("wifi.frequency", "IDENTITY_NOT_REPORTED"); return;
            }
            if (wifiKey == null) wifiKey = strongest.BSSID;
            if (!wifiKey.equals(strongest.BSSID)) wifiChanged = true;
            if (wifiChanged) {
                missing("wifi.rssi", "CONTEXT_CHANGED_RESTART_REQUIRED");
                missing("wifi.frequency", "CONTEXT_CHANGED_RESTART_REQUIRED"); return;
            }
            long source = microsToNanos(strongest.timestamp);
            emit("wifi.rssi", source, strongest.level, Double.NaN,
                    strongest.level < 0 && strongest.level > -200 ? "OBSERVED" : "VALUE_NOT_REPORTED");
            emit("wifi.frequency", source, strongest.frequency, Double.NaN,
                    strongest.frequency > 0 ? "OBSERVED" : "VALUE_NOT_REPORTED");
        } catch (SecurityException | UnsupportedOperationException e) { missing("wifi.", "PERMISSION_OR_API_UNAVAILABLE"); }
    }

    private static long microsToNanos(long value) {
        return value > 0 && value <= Long.MAX_VALUE / 1000L ? value * 1000L : -1;
    }

    private void readCell() {
        if (!has(Manifest.permission.ACCESS_FINE_LOCATION)) { missing("cell.", "PRECISE_PERMISSION_REQUIRED"); return; }
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            List<CellInfo> cache = manager == null ? null : manager.getAllCellInfo();
            CellInfo serving = null;
            if (cache != null) for (int i = 0; i < Math.min(cache.size(), 64); ++i) {
                if (cache.get(i).isRegistered()) { serving = cache.get(i); break; }
            }
            if (serving == null) { missing("cell.", "NO_REGISTERED_CELL_REPORTED"); return; }
            // Base CellInfo getters were added in API30; concrete getters work on minSdk29.
            CellIdentity identity;
            CellSignalStrength strength;
            int rat;
            if (serving instanceof CellInfoLte) {
                identity = ((CellInfoLte) serving).getCellIdentity();
                strength = ((CellInfoLte) serving).getCellSignalStrength();
                rat = TelephonyManager.NETWORK_TYPE_LTE;
            } else if (serving instanceof CellInfoNr) {
                identity = ((CellInfoNr) serving).getCellIdentity();
                strength = ((CellInfoNr) serving).getCellSignalStrength();
                rat = TelephonyManager.NETWORK_TYPE_NR;
            } else if (serving instanceof CellInfoGsm) {
                identity = ((CellInfoGsm) serving).getCellIdentity();
                strength = ((CellInfoGsm) serving).getCellSignalStrength();
                rat = TelephonyManager.NETWORK_TYPE_GSM;
            } else if (serving instanceof CellInfoWcdma) {
                identity = ((CellInfoWcdma) serving).getCellIdentity();
                strength = ((CellInfoWcdma) serving).getCellSignalStrength();
                rat = TelephonyManager.NETWORK_TYPE_UMTS;
            } else { missing("cell.", "RADIO_TECHNOLOGY_UNSUPPORTED"); return; }
            long id = -1;
            int area = Integer.MAX_VALUE, pci = Integer.MAX_VALUE, channel = Integer.MAX_VALUE;
            String mcc = null, mnc = null;
            if (identity instanceof CellIdentityLte) {
                CellIdentityLte x = (CellIdentityLte) identity;
                id = x.getCi(); area = x.getTac(); pci = x.getPci(); channel = x.getEarfcn();
                mcc = x.getMccString(); mnc = x.getMncString();
            } else if (identity instanceof CellIdentityNr) {
                CellIdentityNr x = (CellIdentityNr) identity;
                id = x.getNci(); area = x.getTac(); pci = x.getPci(); channel = x.getNrarfcn();
                mcc = x.getMccString(); mnc = x.getMncString();
            } else if (identity instanceof CellIdentityGsm) {
                CellIdentityGsm x = (CellIdentityGsm) identity;
                id = x.getCid(); area = x.getLac(); pci = x.getBsic(); channel = x.getArfcn();
                mcc = x.getMccString(); mnc = x.getMncString();
            } else if (identity instanceof CellIdentityWcdma) {
                CellIdentityWcdma x = (CellIdentityWcdma) identity;
                id = x.getCid(); area = x.getLac(); pci = x.getPsc(); channel = x.getUarfcn();
                mcc = x.getMccString(); mnc = x.getMncString();
            } else { missing("cell.", "RADIO_TECHNOLOGY_UNSUPPORTED"); return; }
            if (id < 0 || (!(identity instanceof CellIdentityNr) && id == Integer.MAX_VALUE)
                    || id == Long.MAX_VALUE || area < 0 || area == Integer.MAX_VALUE) {
                missing("cell.", "IDENTITY_NOT_REPORTED"); return;
            }
            long source = serving.getTimeStamp();
            int country = mcc != null && mcc.length() == 3 ? numericCode(mcc) : -1;
            int networkCode = numericCode(mnc);
            if (country < 0 || networkCode < 0) {
                missing("cell.", "IDENTITY_CONTEXT_INCOMPLETE"); return;
            }
            String network = mcc + ":" + mnc;
            String key = identity.getClass().getSimpleName() + ":" + network + ":" + area + ":" + id;
            if (servingKey == null) servingKey = key;
            if (!servingKey.equals(key)) cellChanged = true;
            if (cellChanged) { missing("cell.", "HANDOVER_RESTART_REQUIRED"); return; }
            emit("cell.rat", source, rat, Double.NaN, "OBSERVED");
            cellValue("cell.mcc", source, country); cellValue("cell.mnc", source, networkCode);
            cellValue("cell.mnc_digits", source, mnc.length());
            emit("cell.cid", source, id, Double.NaN, "OBSERVED");
            emit("cell.area", source, area, Double.NaN, "OBSERVED");
            cellValue("cell.pci", source, pci); cellValue("cell.channel", source, channel);
            int dbm = strength == null ? Integer.MAX_VALUE : strength.getDbm();
            emit("cell.dbm", source, dbm == Integer.MAX_VALUE ? Double.NaN : dbm,
                    Double.NaN, dbm < 0 && dbm > -200 ? "OBSERVED" : "VALUE_NOT_REPORTED");
        } catch (SecurityException | UnsupportedOperationException e) { missing("cell.", "PERMISSION_OR_API_UNAVAILABLE"); }
    }

    private static int numericCode(String text) {
        if (text == null || text.length() < 2 || text.length() > 3) return -1;
        int value = 0;
        for (int i = 0; i < text.length(); ++i) {
            char digit = text.charAt(i);
            if (digit < '0' || digit > '9') return -1;
            value = value * 10 + digit - '0';
        }
        return value;
    }

    private void cellValue(String id, long source, int value) {
        boolean valid = value >= 0 && value != Integer.MAX_VALUE;
        emit(id, source, valid ? value : Double.NaN, Double.NaN, valid ? "OBSERVED" : "VALUE_NOT_REPORTED");
    }

    private void readBluetooth() {
        if (Build.VERSION.SDK_INT >= 31 && !has(Manifest.permission.BLUETOOTH_CONNECT)) {
            missing("bluetooth.", "CONNECT_PERMISSION_REQUIRED"); return;
        }
        try {
            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
            if (adapter == null) { missing("bluetooth.", "ADAPTER_UNAVAILABLE"); return; }
            emit("bluetooth.enabled", SystemClock.elapsedRealtimeNanos(), adapter.isEnabled() ? 1 : 0, Double.NaN, "OBSERVED");
        } catch (SecurityException | UnsupportedOperationException e) { missing("bluetooth.", "PERMISSION_OR_API_UNAVAILABLE"); }
    }

    private void missing(String prefix, String state) {
        for (String id : selected) if (id.startsWith(prefix)) emit(id, -1, Double.NaN, Double.NaN, state);
        serviceState = state;
    }

    private void emit(String id, long sourceNs, double value, double accuracy, String state) {
        if (!running || !selected.contains(id)) return;
        listener.onSample(id, System.currentTimeMillis(), SystemClock.elapsedRealtimeNanos(),
                sourceNs, value, accuracy, sector(), state);
    }
}
