# EstudioAudio — Lab físico de timbre por Compartilhar (V1)

Date: 2026-10-13
Status: SOURCE_PROPOSED / EXACT_HEAD_CI_NOT_RUN / PHYSICAL_DEVICE_NOT_RUN.
Owner: RAFAELIA / EstudioAudio.
License for added source: SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1.
Invariant: SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM; TOKEN_VAZIO != 0.

## User path
1. On Android, open a **mono 16-bit PCM WAV (RIFF/WAVE, 8–48 kHz)** owned by you or shared with the speaker's permission; select **Compartilhar → Rafaelia Audio Studio**. Any offered `audio/*` type with a non-WAV payload is **rejected**, not auto-decoded by an external library or network.
2. The dedicated `TimbreLearningLabActivity` reads a temporary `content://` URI through Android `ContentResolver.openInputStream()`. It validates RIFF chunks, PCM, mono, sample rate, byte rate/alignment, length and silences before rendering any derived profile. Bounds: audio data <= 2,000,000 bytes; waveform buffer <= 4096 bytes, samples per C call <= 2048; no microphone recording and no file copy. Android share permission exists only at platform edge. Directly available from existing `WavePhysicsLabActivity` too.
3. `rfa_timbre_core.c/.h` computes a **rough low-level timbral COLOR proxy** from sample amplitude, absolute first differences and sign-crossing count (not formants/pitch/phoneme/acoustic identity). All stats belong to caller, no libc, libm, heap, syscalls, external audio model or mutable static state. ARM32 lacks implicit u64 division helpers by using bounded shift-subtract, subject to exact ABI gate.
4. Accepted profile resides **in process memory only**, used as optional one-pole color filter over the **existing** freestanding PTBR phonetic guide output through a narrow JNI bridge; it never silently replaces the script, records audio or starts playback. `AuthorialGuidePlayer` snapshots the profile at the start of an explicitly user-triggered `Voice guide · experimental` session. `Apagar perfil da memória` disables it. Process restart clears ephemeral profile.
5. Only pressing `Exportar perfil ZIPRAF` writes **derived integer descriptors + typed receipt**, via existing authorial `RfaStoredZip` (STORED) into `Downloads/RAFAELIA_TTS_LAB` using `MediaStore.Downloads` / `IS_PENDING`, exclusively on API29+. No raw PCM/WAV, no source URI, no voice print, no cloud, no network, no background sync, no new storage permission. ZIPRAF here acts as a versioned **record container**, not encrypted database. Persistent query/import/catalog across launches is **NOT_IMPLEMENTED**.
6. ZIPRAF content is sensitive derived voice information. It's local Downloads, potentially readable by the device owner/other apps with access: **not encrypted**. Export requires explicit action. Users may delete it from Downloads. Do not upload this profile to public GitHub/Drive/alphaXiv.

## Source and scopes
Core: `app/src/main/cpp/rfa_timbre_core.c/.h`, `native/tests/timbre_core_smoke.c`.
ABI: `native/abi/public_symbols_v1.txt`. ARM cross-build: `ci/freestanding-runtime-gate.sh`.
Host gate: `ci/timbre-share-core-gate.sh`.
Platform: `TimbreLearningLabActivity.java`, `TimbreProfileStore.java`, `NativeDsp.java`, `jni_bridge.c`; Android share target in manifest and lab button in `WavePhysicsLabActivity`.
TTS: `AuthorialGuidePlayer.java` optional profile filter; existing `rfa_ptbr_core.c` not modified.
Storage: Android `MediaStore` and `OutputStream` remain necessary platform APIs; the C core itself is freestanding but an Android APK never is.
No DOCES/Arruda code, voice, training dataset or other research implementation copied.

## Adversarial gates / P0
- Host: silence rejected; invalid/oversized stream rejected; chunked vs one-shot descriptor and color-filter equality; bounded fixed-point and disabled-profile no behavioral regression; no implicit libc or compiler runtime helpers ARMv7/AArch64, zero writable globals and ABI exact manifest.
- Android: ACTION_SEND URI permission/revocation and malicious URI/CHUNK; non-WAV MP3/Opus negative; sharing directly from recording app; app backgrounded during read; silent/noisy, 8/16/48k mono, unsupported stereo/float/channels reject; retry/stop and process restart; MediaStore publish pending/rollback and deletion on failure; check absence of source PCM/WAV in Downloads/ZIPRAF.
- Human: consent/rights and independent blind intelligibility check; test timbre identity claims only with measured protocols, if ever. This V1 **does not** clone a voice or learn lexical/prosodic pronunciation.
- Threat: ZIPRAF profile descriptors could be sensitive biometric-adjacent data; privacy controls assume local device access and explicit export, not encryption.
- Provider: EstudioAudio `main` currently unprotected and independent review unresolved, so this draft **MUST NOT PROMOTE**, even if host gate passes.

## Rollback
Revert this isolated PR. It introduces only an additive share lab, opt-in transient filter, and no new persistent raw source. Existing records already exported to Downloads are user-owned and **not deleted automatically** by app rollback. Do not reset main or corrupt unrelated sessions.

R3=<F_ok: source candidate, F_gap: exact-head CI/Android physical/import security/privacy review, F_next: falsification and readback before any merge>.
