/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Explicit foreground-only acoustic research. 3 separately user-initiated
 * recordings of the SAME chosen reference WAV, stored in app-private files.
 *
 * Never asserts measured SPL, sample-perfect stimulus alignment or room-EQ.
 * Android AudioRecord/AudioTrack are a hosted I/O edge; mathematical grouping
 * remains in WaveLabTrialMatrix and WaveLabWelchCore.
 */
public final class WaveLabSessionActivity extends Activity implements SensorEventListener {
    private static final int PICK_WAV=141, REQUEST_MIC=142;
    private static final int SAMPLE_RATE=48000;
    private static final float TRACK_GAIN=0.10f; // volume fraction, NOT safe SPL guarantee
    private final WaveLabTrialMatrix trials=new WaveLabTrialMatrix();
    private final String sessionId="session-"+Long.toHexString(SystemClock.elapsedRealtimeNanos());
    private volatile boolean busy, cancelCapture;
    private WaveLabReferencePcm.Parsed reference;
    private String referenceSha;
    private TextView status;
    private Button pickButton, captureButton;
    private File folder;
    private SensorManager sensorManager;
    private final Object sensorLock=new Object();
    private static final int MAX_ACCEL_EVENTS=4096;
    private final long[] accelNs=new long[MAX_ACCEL_EVENTS];
    private final float[] accelX=new float[MAX_ACCEL_EVENTS];
    private final float[] accelY=new float[MAX_ACCEL_EVENTS];
    private final float[] accelZ=new float[MAX_ACCEL_EVENTS];
    private int accelCount;
    private boolean accelOverflow;
    private volatile boolean collectAccel;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        LinearLayout panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(16*getResources().getDisplayMetrics().density);
        panel.setPadding(pad,pad,pad,pad);
        TextView title=new TextView(this);
        title.setTextSize(20);
        title.setText("RAFAELIA Ω · Sessão de aferição 3 tomadas");
        panel.addView(title);
        TextView note=new TextView(this);
        note.setText("EXPERIMENTO RELATIVO. Selecione um WAV PCM16 mono de 48 kHz (3–30 s). " +
                "Cada toque inicia uma nova reprodução e uma nova captura. " +
                "Inicie com o volume do telefone muito baixo; evite fones e sinais altos. " +
                "Não mova o aparelho nem altere o ganho entre tomadas. " +
                "A saída será armazenada SOMENTE em arquivos privados do aplicativo. " +
                "Sem SPL absoluto, sem calibração automática ou controle de potência.");
        panel.addView(note);
        pickButton=new Button(this);
        pickButton.setText("1. Escolher WAV de referência");
        pickButton.setOnClickListener(v->pick());
        panel.addView(pickButton);
        captureButton=new Button(this);
        captureButton.setText("2. Capturar tomada primária (WAV + microfone)");
        captureButton.setOnClickListener(v->capture());
        panel.addView(captureButton);
        status=new TextView(this);
        status.setTextIsSelectable(true);
        panel.addView(status);
        ScrollView root=new ScrollView(this);
        root.addView(panel);
        setContentView(root);
        folder=new File(new File(getFilesDir(),"wavelab_sessions"),sessionId);
        show("SOURCE=NOT_SELECTED; PCM_CAPTURE=NOT_RUN");
    }

    private void show(String message) {
        if(status!=null)
            status.setText("session="+sessionId+"\n"+
                    "reference_sha256="+(referenceSha==null?"TOKEN_VAZIO":referenceSha)+"\n"+
                    "accepted_takes="+trials.count()+"/3\n"+
                    message+"\n"+
                    "ABS_SPL=TOKEN_VAZIO_NOT_CALIBRATED\n"+
                    "SAMPLE_ACCURATE_LOOPBACK=TOKEN_VAZIO_NOT_PROVEN\n"+
                    "claim_allowed=false");
        if(captureButton!=null) {
            captureButton.setEnabled(!busy && reference!=null && trials.count()<3);
            if(trials.count()<3)
                captureButton.setText("Capturar "+WaveLabTrialMatrix.Role.values()[trials.count()]+
                        " (som audível + microfone)");
            else captureButton.setText("Três tomadas armazenadas");
        }
        if(pickButton!=null)pickButton.setEnabled(!busy && trials.count()==0);
    }

    private void pick() {
        if(busy || trials.count()!=0)return;
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(i,PICK_WAV);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=PICK_WAV || resultCode!=RESULT_OK ||
                data==null || data.getData()==null || busy || trials.count()!=0)return;
        Uri uri=data.getData();
        busy=true;
        show("REFERENCE_READ=IN_PROGRESS (NO PLAYBACK)");
        new Thread(()->{
            try(InputStream in=getContentResolver().openInputStream(uri)) {
                byte[] source=WaveLabReferencePcm.readBounded(in);
                WaveLabReferencePcm.Parsed parsed=WaveLabReferencePcm.parse(source);
                String digest=sha256(source);
                runOnUiThread(()->{
                    reference=parsed;
                    referenceSha=digest;
                    busy=false;
                    show("REFERENCE=VERIFIED_FORMAT frames="+parsed.pcm.length+
                            " duration_s="+(parsed.pcm.length/(double)SAMPLE_RATE));
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    busy=false;
                    show("REFERENCE_REJECTED="+e.getClass().getSimpleName()+
                            " (NO PHYSICAL CLAIM)");
                });
            }
        },"rafaelia-validate-wav").start();
    }

    private void capture() {
        if(busy || reference==null || trials.count()==3)return;
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                !=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_MIC);
            return;
        }
        busy=true;
        cancelCapture=false;
        final WaveLabTrialMatrix.Role role=WaveLabTrialMatrix.Role.values()[trials.count()];
        show("CAPTURE_REQUESTED="+role+" (explicit user action)");
        new Thread(()->captureWorker(role),"rafaelia-session-acquire").start();
    }

    @Override public void onRequestPermissionsResult(int req,String[] perms,int[] results) {
        super.onRequestPermissionsResult(req,perms,results);
        if(req==REQUEST_MIC) {
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    ==PackageManager.PERMISSION_GRANTED) capture();
            else show("MIC_PERMISSION_DENIED; NO_CAPTURE");
        }
    }

    private static String sha256(byte[] b) throws NoSuchAlgorithmException {
        MessageDigest d=MessageDigest.getInstance("SHA-256");
        d.update(b);
        return hex(d.digest());
    }
    private static String hex(byte[] hash) {
        char[] out=new char[hash.length*2];
        final char[] alphabet="0123456789abcdef".toCharArray();
        for(int i=0;i<hash.length;i++){
            out[i*2]=alphabet[(hash[i]>>>4)&15];
            out[i*2+1]=alphabet[hash[i]&15];
        }
        return new String(out);
    }
    private static String route(AudioDeviceInfo d) {
        return d==null?"ROUTE_UNOBSERVED":
                "TYPE_"+d.getType()+"_ID_"+d.getId();
    }
    private static boolean disable(AutomaticGainControl e) {
        return e!=null && e.setEnabled(false)==0 && !e.getEnabled();
    }
    private static boolean disable(NoiseSuppressor e) {
        return e!=null && e.setEnabled(false)==0 && !e.getEnabled();
    }
    private static boolean disable(AcousticEchoCanceler e) {
        return e!=null && e.setEnabled(false)==0 && !e.getEnabled();
    }

    private void captureWorker(WaveLabTrialMatrix.Role role) {
        AudioRecord recorder=null;
        AudioTrack player=null;
        AutomaticGainControl agc=null;
        NoiseSuppressor ns=null;
        AcousticEchoCanceler aec=null;
        File pcmFile=null;
        String report="CAPTURE_NOT_COMPLETED";
        int written=0, clipped=0;
        long startNs=0, endNs=0;
        long audioFrame=-1, audioTimestampNs=-1;
        try {
            if(!folder.isDirectory() && !folder.mkdirs())
                throw new IllegalStateException("PRIVATE_SESSION_FOLDER_UNAVAILABLE");
            final short[] stimulus=reference.pcm;
            final int target=stimulus.length+SAMPLE_RATE; // capture extends 1s after stimulus
            if(folder.getUsableSpace()<(long)target*2+1024*1024)
                throw new IllegalStateException("INSUFFICIENT_PRIVATE_STORAGE");
            final AudioManager manager=(AudioManager)getSystemService(AUDIO_SERVICE);
            final int volumeStart=manager==null?-1:
                    manager.getStreamVolume(AudioManager.STREAM_MUSIC);
            final boolean raw=manager!=null && "true".equals(manager.getProperty(
                    AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED));
            final String sourceName=raw?"UNPROCESSED":"VOICE_RECOGNITION";
            int minInput=AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if(minInput<=0)throw new IllegalStateException("UNSUPPORTED_MIC_CONFIGURATION");
            AudioFormat inFormat=new AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();
            recorder=new AudioRecord.Builder()
                    .setAudioSource(raw?MediaRecorder.AudioSource.UNPROCESSED:
                            MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(inFormat)
                    .setBufferSizeInBytes(Math.max(8192,minInput*2)).build();
            if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)
                throw new IllegalStateException("MIC_NOT_INITIALIZED");
            try {
                if(AutomaticGainControl.isAvailable())
                    agc=AutomaticGainControl.create(recorder.getAudioSessionId());
                if(NoiseSuppressor.isAvailable())
                    ns=NoiseSuppressor.create(recorder.getAudioSessionId());
                if(AcousticEchoCanceler.isAvailable())
                    aec=AcousticEchoCanceler.create(recorder.getAudioSessionId());
            }catch(RuntimeException effectError){
                // Evidence will say effects may be present; no false bypass claim.
            }
            final boolean agcOff=disable(agc),nsOff=disable(ns),aecOff=disable(aec);

            AudioFormat outFormat=new AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
            AudioAttributes attrs=new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            player=new AudioTrack.Builder()
                    .setAudioFormat(outFormat).setAudioAttributes(attrs)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(stimulus.length*2).build();
            if(player.getState()!=AudioTrack.STATE_INITIALIZED)
                throw new IllegalStateException("PLAYBACK_NOT_INITIALIZED");
            int loaded=player.write(stimulus,0,stimulus.length,AudioTrack.WRITE_BLOCKING);
            if(loaded!=stimulus.length)
                throw new IllegalStateException("PLAYBACK_REFERENCE_SHORT_WRITE");
            player.setVolume(TRACK_GAIN);
            String micRouteBefore="ROUTE_UNOBSERVED";
            String outRouteBefore="ROUTE_UNOBSERVED";
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            String attemptId=Long.toHexString(SystemClock.elapsedRealtimeNanos());
            pcmFile=new File(folder,role.name().toLowerCase(Locale.US)+"_"+
                    attemptId+"_mic48k_s16le.pcm");
            if(!pcmFile.createNewFile())
                throw new IllegalStateException("CAPTURE_FILE_ALREADY_EXISTS");
            short[] pcm=new short[target];
            short[] block=new short[4096];
            byte[] bytes=new byte[block.length*2];

            AudioTimestamp audioStamp=new AudioTimestamp();
            recorder.startRecording();
            startNs=SystemClock.elapsedRealtimeNanos();
            synchronized(sensorLock) {
                accelCount=0;
                accelOverflow=false;
                collectAccel=true;
            }
            if(recorder.getTimestamp(audioStamp,AudioTimestamp.TIMEBASE_MONOTONIC)
                    ==AudioRecord.SUCCESS) {
                audioFrame=audioStamp.framePosition;
                audioTimestampNs=audioStamp.nanoTime;
            }
            if(cancelCapture)throw new IllegalStateException("ACTIVITY_PAUSED");
            player.play();
            micRouteBefore=route(recorder.getRoutedDevice());
            outRouteBefore=route(player.getRoutedDevice());
            try(FileOutputStream sink=new FileOutputStream(pcmFile,true)) {
                while(written<target && !cancelCapture) {
                    int got=recorder.read(block,0,
                            Math.min(block.length,target-written),AudioRecord.READ_BLOCKING);
                    if(got<=0)throw new IllegalStateException("MIC_SHORT_READ_"+got);
                    for(int i=0;i<got;i++) {
                        short sample=block[i];
                        pcm[written+i]=sample;
                        if(sample==32767 || sample==-32768)clipped++;
                        bytes[i*2]=(byte)sample;
                        bytes[i*2+1]=(byte)(sample>>8);
                    }
                    sink.write(bytes,0,got*2);
                    digest.update(bytes,0,got*2);
                    written+=got;
                }
                sink.getFD().sync();
            }
            endNs=SystemClock.elapsedRealtimeNanos();
            collectAccel=false;
            if(cancelCapture || written!=target)
                throw new IllegalStateException("CAPTURE_INCOMPLETE");
            String micRouteAfter=route(recorder.getRoutedDevice());
            String outRouteAfter=route(player.getRoutedDevice());
            player.stop();
            recorder.stop();
            if(manager!=null &&
                    manager.getStreamVolume(AudioManager.STREAM_MUSIC)!=volumeStart)
                throw new IllegalStateException("PLAYBACK_VOLUME_CHANGED");
            if((!micRouteBefore.equals("ROUTE_UNOBSERVED") &&
                    !micRouteAfter.equals("ROUTE_UNOBSERVED") &&
                    !micRouteBefore.equals(micRouteAfter)) ||
                    (!outRouteBefore.equals("ROUTE_UNOBSERVED") &&
                    !outRouteAfter.equals("ROUTE_UNOBSERVED") &&
                    !outRouteBefore.equals(outRouteAfter)))
                throw new IllegalStateException("AUDIO_DEVICE_ROUTE_CHANGED");

            WaveLabWelchCore.Result spectrum=WaveLabWelchCore.analyze(pcm,written);
            if(spectrum==null)throw new IllegalStateException("SPECTRUM_UNAVAILABLE");
            String pcmHash=hex(digest.digest());
            String microphone=sourceName+"_"+micRouteAfter;
            String loudspeaker="TRACK_"+outRouteAfter;
            String gain="VOL_"+volumeStart+"_TRACK_0p10";
            WaveLabTrialMatrix.Take take=new WaveLabTrialMatrix.Take(
                    role,"USER_SELECTED_WAV_PCM16_MONO",referenceSha,pcmHash,
                    microphone,loudspeaker,"P1_USER_DECLARED",gain,
                    WaveLabWelchCore.ESTIMATOR,"RELATIVE_NOT_CALIBRATED",
                    "SAME_ANDROID_MONOTONIC_BOOT",
                    SAMPLE_RATE,written,clipped,startNs,endNs,
                    spectrum.frequenciesHz,spectrum.bandPowers);
            WaveLabTrialMatrix.Decision outcome=trials.append(take);
            // Persist optional acceleration samples under the same attempt identifier.
            // SensorEvent.timestamp and AudioTimestamp use monotonic time bases,
            // but exact microphone/sensor sample alignment is NOT established.
            String accelHash="TOKEN_VAZIO_NO_SENSOR_EVENTS";
            int capturedAccel;
            boolean capturedOverflow;
            StringBuilder accelerometer=new StringBuilder("monotonic_ns,ax_m_s2,ay_m_s2,az_m_s2\n");
            synchronized(sensorLock) {
                capturedAccel=accelCount;
                capturedOverflow=accelOverflow;
                for(int j=0;j<accelCount;j++) {
                    accelerometer.append(accelNs[j]).append(',')
                            .append(accelX[j]).append(',')
                            .append(accelY[j]).append(',')
                            .append(accelZ[j]).append('\n');
                }
            }
            if(capturedAccel>0) {
                byte[] sensorBytes=accelerometer.toString()
                        .getBytes(StandardCharsets.UTF_8);
                File sensorFile=new File(folder,role.name().toLowerCase(Locale.US)+
                        "_"+attemptId+"_accelerometer.csv");
                if(!sensorFile.createNewFile())
                    throw new IllegalStateException("SENSOR_FILE_ALREADY_EXISTS");
                try(FileOutputStream sensorOut=new FileOutputStream(sensorFile,true)){
                    sensorOut.write(sensorBytes);
                    sensorOut.getFD().sync();
                }
                accelHash=sha256(sensorBytes);
            }
            String envelope="schema=rafaelia.audio.three_takes.v1\n"+
                    "source_commit="+BuildConfig.SOURCE_SHA+"\n"+
                    "role="+role+"\nsource_wav_sha256="+referenceSha+"\n"+
                    "raw_pcm_sha256="+pcmHash+"\nraw_encoding=PCM16LE_MONO_48K\n"+
                    "raw_samples="+written+"\nclipped_samples="+clipped+"\n"+
                    "capture_start_monotonic_ns="+startNs+"\n"+
                    "capture_end_monotonic_ns="+endNs+"\n"+
                    "audio_timestamp_frame="+audioFrame+"\n"+
                    "audio_timestamp_monotonic_ns="+audioTimestampNs+"\n"+
                    "input_route="+microphone+"\noutput_route="+loudspeaker+"\n"+
                    "effects_disabled_agc="+agcOff+"\n"+
                    "effects_disabled_ns="+nsOff+"\n"+
                    "effects_disabled_aec="+aecOff+"\n"+
                    "volume_stream_music="+volumeStart+"\n"+
                    "track_gain="+TRACK_GAIN+"\n"+
                    "estimator="+WaveLabWelchCore.ESTIMATOR+"\n"+
                    "welch_windows="+spectrum.windowCount+"\n"+
                    "accelerometer_events="+capturedAccel+"\n"+
                    "accelerometer_overflow="+capturedOverflow+"\n"+
                    "accelerometer_csv_sha256="+accelHash+"\n"+
                    "decision="+outcome+"\n"+
                    "SPL_ABSOLUTE=TOKEN_VAZIO_UNCALIBRATED\n"+
                    "LOOPBACK_ALIGNMENT=TOKEN_VAZIO_NOT_MEASURED\n"+
                    "CLAIM_ALLOWED=false\n";
            for(int b=0;b<spectrum.bandPowers.length;b++)
                envelope+="band_"+(int)spectrum.frequenciesHz[b]+"Hz_power="+
                        spectrum.bandPowers[b]+"\n";
            try(FileOutputStream meta=new FileOutputStream(
                    new File(folder,role.name().toLowerCase(Locale.US)+"_"+
                            attemptId+"_receipt.txt"),true)) {
                meta.write(envelope.getBytes(StandardCharsets.UTF_8));
                meta.getFD().sync();
            }
            report="CAPTURE="+outcome+"\n"+
                    "PCM_SHA256="+pcmHash+"\n"+
                    "PCM_FRAMES="+written+" (mono @48k)\n"+
                    "FFT_WELCH_WINDOWS="+spectrum.windowCount+"\n"+
                    "RECEIPT=APP_PRIVATE\n"+
                    "SOURCE_SYNC_AND_PA=NOT_CALIBRATED";
            if(trials.count()==3) {
                WaveLabTrialMatrix.Report summary=trials.report();
                StringBuilder b=new StringBuilder(report);
                b.append("\nTHREE_TAKES_RELATIVE_ONLY\n");
                for(int i=0;i<summary.medianLinearPower.length;i++) {
                    b.append((int)spectrum.frequenciesHz[i]).append("Hz: "+
                            "median_relative_to_primary_dB=")
                            .append(summary.medianRelativeToPrimaryDb[i])
                            .append(" spread_dB=").append(summary.spanDb[i]).append('\n');
                }
                report=b.toString();
            }
        } catch(Exception failure) {
            report="CAPTURE_FAILED_"+failure.getClass().getSimpleName()+
                    ": "+failure.getMessage()+"\n"+
                    "PCM_PARTIAL_IF_PRESENT="+(pcmFile==null?"NONE":
                            pcmFile.getName())+"\n"+
                    "CLAIM_ALLOWED=false";
            // Append-only failure receipt; no partial capture is silently promoted.
            try {
                if (folder.isDirectory()) {
                    File errorReceipt=new File(folder,"failed_"+
                            Long.toHexString(SystemClock.elapsedRealtimeNanos())+".txt");
                    if (errorReceipt.createNewFile()) {
                        try(FileOutputStream sink=new FileOutputStream(errorReceipt,true)){
                            sink.write((report+"\nrole="+role+
                                    "\nraw_status=PARTIAL_NOT_ACCEPTED\n")
                                    .getBytes(StandardCharsets.UTF_8));
                            sink.getFD().sync();
                        }
                    }
                }
            }catch(Exception ignored) { /* UI still reports failure */ }
        } finally {
            collectAccel=false;
            if(player!=null) {
                try{if(player.getPlayState()==AudioTrack.PLAYSTATE_PLAYING)
                    player.stop();}catch(RuntimeException ignored){}
                player.release();
            }
            if(recorder!=null) {
                try{if(recorder.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)
                    recorder.stop();}catch(RuntimeException ignored){}
                recorder.release();
            }
            if(agc!=null)agc.release();
            if(ns!=null)ns.release();
            if(aec!=null)aec.release();
            final String finalReport=report;
            runOnUiThread(()->{
                busy=false;
                show(finalReport);
            });
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if(sensorManager!=null) {
            Sensor a=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if(a!=null) sensorManager.registerListener(this,a,SensorManager.SENSOR_DELAY_GAME);
        }
    }
    @Override protected void onPause() {
        if(busy) cancelCapture=true;
        collectAccel=false;
        if(sensorManager!=null) sensorManager.unregisterListener(this);
        super.onPause();
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy) { }
    @Override public void onSensorChanged(SensorEvent event) {
        if(!collectAccel || event.sensor.getType()!=Sensor.TYPE_ACCELEROMETER)return;
        synchronized(sensorLock) {
            if(!collectAccel)return;
            if(accelCount>=MAX_ACCEL_EVENTS) {
                accelOverflow=true;
                return;
            }
            int i=accelCount++;
            accelNs[i]=event.timestamp;
            accelX[i]=event.values[0];
            accelY[i]=event.values[1];
            accelZ[i]=event.values[2];
        }
    }
}
