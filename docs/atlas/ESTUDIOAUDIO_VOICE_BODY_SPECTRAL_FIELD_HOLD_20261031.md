# EstudioAudio · VOICE × BODY · Campo Vibroacústico Ω (DRAFT/HOLD)

**Owner/producer:** `rafaelmeloreisnovo/EstudioAudio`. **Baseline:** `main@05487a625c9e02add2984f24858c4223848aef13`. **Stage:** source integration on isolated experiment branch; exact-head APK/physical device NOT_RUN; `claim_allowed=false`.

## Intent / scope

Observe, when deliberately enabled during an **existing** REC session, simultaneous acoustic power in 16 Goertzel bands and short-window changes of the Android accelerometer. Draw opposing luminous filaments as a visual mapping. This is **not** magnetic-field metrology, heart-rate capture, stress/emotion detection, EEG, or a contact microphone.

## Existing producer components (unchanged)

- `AudioRecorderEngine`: Android AudioRecord, PCM16 mono 48 kHz, preserves raw PCM for the normal studio mastering path.
- `NativeDsp.nativeSpectrum` -> `meter_spectrum16` in freestanding C: existing 16-band Goertzel observer. The new overlay feeds **the 512 most recent PCM samples only** (~10.7 ms at 48 kHz), refreshed at the pre-existing 500 ms UI telemetry interval. It is not continuous 16-band spectral sampling or a calibrated spectrogram.
- `MicroDeltaVibrationProbe`: original short-lived evidence action remains unchanged and its results keep their original meaning.
- `MainActivity`: existing REC/master, calibration, permission, PCM and ZIPRAF paths remain intact.

## Small addition

- `VoiceBodyMotionProbe`: second **explicit opt-in** motion observation only during an already active REC, same Android SensorManager edge. Bounded ring of 256 timestamped differences, rolling one-second RMS/peak, sensor timestamp-age and effective Hz. Never exported, no extra permission, no background sensor after pause/stop/destroy. Sensor absent => `TOKEN_VAZIO`, never zero-as-observation.
- `VoiceBodyFieldView`: Android Canvas opposing arcs driven by **relative per-frame** native Goertzel energies and by the bounded accelerometer-delta envelope. Colors and geometry are UI encoding only; they do not represent a measured vector magnetic field, phase relationship or measured F0.
- A button under **More tools**, `▶ Observar voz + movimento (durante REC)`; it refuses when REC is absent, and stops on user request, stop/master, pause or destroy. It does not instantiate a second `AudioRecord`.

## Boundaries / falsifiers

1. Only press the overlay action after granting existing REC permission and starting a recording. A rejected/missing sensor must not block microphone recording. Verify the original REC -> STOP -> MASTER -> export round-trip remains unaffected.
2. No new permissions, CI workflows, remote endpoints, dependencies, JNI symbols or ABI surface. The overlay never uploads speech, motions or a profile. Existing PCM recording persists **only via the studio's pre-existing recording behavior**; the additional motion trace stays in memory.
3. Simultaneous collection does **not** establish synchronized sample-level phases: Android audio frame age and sensor events are not measured with a shared offset in this implementation. Cross-correlation, vocal-fundamental estimation and physiological inference are outside scope.
4. ~124 Hz accelerometer sampling historically observed has a theoretical Nyquist around 62 Hz under ideal conditions; it cannot establish ordinary voice harmonic structure. Motion can be hand movement, ambient vibration or posture, and no causal linkage to vocal folds is proven.
5. Stress, heart-rate and pulse remain `TOKEN_VAZIO`. Add any external contact sensor, ECG/PPG or wearables under a separately reviewed permission/consent/clock contract.
6. Check device at normal conservative playback level; avoid headset feedback and **do not** strap a phone, press a transducer on the larynx, or insert improvised parts in the ear. No health diagnosis.

## Acceptance gate

- Source: **candidate implemented**, readback/compare exact-head required.
- `CI_JAVA_COMPILE`, `APK_BUILD_SHA`, `ANDROID10_REC_SENSOR_RUNTIME`, `SENSOR_EFFECTIVE_RATE`, `DEVICE_PLAYBACK`, `HOSTED_INDEPENDENT_REVIEW` = `TOKEN_VAZIO_NOT_TESTED` until exact evidence.
- Negative cases: missing accelerometer, absent REC, permission denial, pause/background, zero/quiet audio, sensor timestamps stale, repeated start/stop, long bounded window.
- Preservation: no changes to `app/src/main/cpp`, `AudioRecorderEngine`, `MicroDeltaVibrationProbe`, mastering, SDK, Gradle, workflows, warnings or authorial license.
- `rollback`: close draft PR/delete isolated branch; main and original master/evidence files unaffected.

`SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM`; `IMPLEMENTED_UNTESTED ≠ PASS`.

`R3 = <F_ok: source wired to existing recorder/JNI/SensorManager, F_gap: hosted CI and Android physical proof, F_next: exact-head Android build, opt-in A/B device falsifiers, then review>`.
