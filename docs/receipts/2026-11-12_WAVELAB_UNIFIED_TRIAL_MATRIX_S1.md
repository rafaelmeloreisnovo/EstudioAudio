# μRECEIPT — EstudioAudio · WaveLab matriz de três tomadas · 2026-11-12

**μID:** MU-ESTUDIOAUDIO-WAVELAB-TRIAL-MATRIX-20261112-S1
**AUTHOR:** Rafael Melo Reis / RAFCODEΦ; assisted source/research drafting.
**KIND:** SOURCE_DELTA_AND_METHOD_EXPERIMENT / APPEND_ONLY / claim_allowed=false
**OBSERVED_DATE:** 2026-11-12; horário exato não capturado.
**PRODUCER:** rafaelmeloreisnovo/EstudioAudio
**BASE_MAIN_SHA:** 58d45fe2c8b17575ea13fafa8cf4bde54b691851 (PR82 merged)
**SOURCE_BRANCH:** feature/wavelab-trial-matrix-20261112 (não mesclada)
**LATEST_SOURCE_EDIT_COMMIT:** 737a6014e4159906409cd015e0d220d30965ec32
**SOURCE:** docs/START_HERE.md, AGENTS.md, WavePhysicsLabActivity.java, WaveLabCore.java; experimento acústico descrito pelo autor; referências REW, Audio Precision e DigiKey no protocolo.
**PREDECESSOR:** docs/CFR_RELATIVE_ANALYSIS_V1.md + docs/ROOM_CAL_NOISE_TRUE_RTA_ROUTE_V1.md (ambos preservados); histórico da auditoria local de artefatos do usuário em 2026-11-12.

## Quatro arquivos e leitura de volta GitHub (blob SHA-1, NÃO hash SHA-256 dos dados da sessão)

1. app/src/main/java/io/rafaelia/audiostudio/WaveLabTrialMatrix.java — blob 6bd415a0f55b791205e7063c81a2c3e6b88b394d — 8221 caracteres;
2. ci/WaveLabTrialMatrixTest.java — blob 39010d1b9a2c083844b2b8b492312b29cc46fa81 — 9376 caracteres;
3. ci/wave-lab-gate.sh — blob e013fff45b587e590b1d3b3aae228a7bb2963e42 — 1540 caracteres;
4. docs/WAVE_LAB_UNIFIED_RTA_REPEATABILITY_EXPERIMENT_V1.md — blob 492f57c5883e8d8e324cadab3d273d547f858819 — 12324 caracteres.

**Provider source readback:** VERIFIED_SCOPED for paths and content markers.
**COMPILE:** NOT_RUN for this branch; the wave-lab-gate was extended to run new tests but no exact-HEAD CI or host result was observed.
**APK:** NOT_RUN. **ANDROID_DEVICE:** NOT_RUN. **REAL_AUDIO_SESSION:** NOT_IMPLEMENTED. **RTA_FFT:** NOT_IMPLEMENTED in this change. **ABS_SPL:** TOKEN_VAZIO_PHYSICAL_REFERENCE.
**PR:** NOT_CREATED. **MERGE/PUBLICATION:** NOT_PERFORMED.
**SOURCE_BYTES_CAPTURED:** no raw audio/data files were added to GitHub.

## Scope and falsifiers

Model compares three independently captured files at one controlled condition only. Input requires digital stimulus and raw SHA-256 supplied by caller, same microphone chain, speaker, position, playback gain, spectral estimator, calibration reference, clock domain and sample rate. Strict primary-secondary-tertiary sequence, >=3 seconds each, clipping rejection, non-overlapping time and positive finite spectral powers. It calculates median on linear powers, relative logarithmic level and maximum/minimum spread; does not compute confidence, SPL nor physical fidelity. Log-domain differences avoid overflow from extreme but finite power inputs.

Source-level negative tests are staged for duplicate SHA, unknown source hash, missing primary, short read, clipping, mic/gain/position/estimator drift, clock overlap, 0/NaN power and extreme power ratios. This receipt is not a PASS for test execution.

**Specific old gap:** Existing WavePhysicsLabActivity captures 4096 samples at 48kHz (=85.333ms) with six single-frequency Goertzel measures. These are not a proper 10s synchronized RTA nor calibrated SPL, independent of the older sweep/relative CFR pipeline. No unverified correction to current UI is claimed.

**Safeguards:** no playback, no microphone access, no new permissions, no network calls, no raw corpus duplication, no silent DSP changes. Branch isolated from main; if tests fail, revise only this branch with successor evidence.

## R3
- F_ok: current code baseline, shortest-capture gap, academic method and safe comparator written; source readback.
- F_gap: exact-head Java host gate, Android session/audio timestamp/FFT/stimulus generator/source capture, physical calibration and actual three independent takes not performed.
- F_next: run java host gate and falsifiers, add explicit 10–30s AudioRecord capture with sample-timed metadata + raw PCM receipt, add deterministic pink-periodic/sweep reference and physical A/B; no merge until gates.
