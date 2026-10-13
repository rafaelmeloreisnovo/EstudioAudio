# Teleprompter / authorial voice guide — V0

status: IMPLEMENTED_UNTESTED (until exact SHA CI completes)
claim_allowed_end_to_end: false
target_repo: rafaelmeloreisnovo/EstudioAudio
scope: ASCII/selected Brazilian-Portuguese Latin-1 UTF-8 vowel/consonant *cue synthesis*, not natural voice TTS.

## Boundaries

- Pure core: `rfa_tele_tts_core.c/.h`, -nostdinc -ffreestanding -fno-builtin; integer only; no syscalls, libc, libm, heap, threads, persistent mutable state.
- Teleprompter: caller-supplied time delta (milliseconds) and caller-owned cursor. Physical display/touch/clock not in core. WPM conversion lives at the adapter.
- Synthetic guide: render mono PCM16 at *exactly* 16000 samples/s. Caller supplies UTF-8 bytes and output memory; length <= 4096 bytes in C (Android adapter <= 2048). Continuation state: six u32 words. No external speech engine or prerecorded dataset.
- JNI: copies caller-owned state; no native persistent script pointer; app `AudioTrack` sends PCM to a real speaker. `AuthorialGuidePlayer` is a hosted Android-only boundary.
- Android has an existing teleprompter UI; this change adds an opt-in voice-guide button; no new microphone permission.
- UI speech guide is not synchronized word-for-word with the existing scrolling teleprompter yet. No recognition/voice cloning/semantic phoneme dictionary.
- Actual understandable PT-BR TTS, prosody, phonemic accuracy, listening/accessibility verification: TOKEN_VAZIO_NOT_VALIDATED.
- No universal single binary: each ISA/ABI requires an appropriate compiler/assembler/linker, calling convention, memory/device drivers and clock/audio/display adapter. True bare-metal speaker output requires hardware-specific drivers or DMA/MMIO; no sound appears solely by generating PCM in memory.
- Build toolchain (Clang/GCC, Android Gradle/SDK/NDK for app) remains external at BUILD time; Android framework/JNI/AudioTrack remain required at APP RUNTIME. Core logic alone has no such runtime dependencies.

## Scope tests and falsifiers

1. `bash ci/tele-tts-core-gate.sh`: deterministic chunk vs whole PCM, word boundaries / caller clock, bounds / no writes on error, no missing symbol linkage.
2. `python3 ci/core-standalone-gate.py` plus `Makefile.core`: public symbol manifest parity; zero undefined symbols and zero writable static state.
3. `ci/freestanding-runtime-gate.sh`: ARMv7 and AArch64 objects, no implicit runtime helpers.
4. Full Android gate: JNI/AudioTrack compiled into APK. Then independently check install, audio sample rate, stop/restart, and speaker outputs on physical hardware.
5. Adversarial: null/oversize inputs, malformed UTF-8, rapid Start/Stop, backgrounding, concurrent recording, direct input resubmission. Devices cannot inherit a PASS from a host object.
6. Test natural speech intelligibility and pronouncing Portuguese words **before** claiming production TTS; a synthetic cue is not equivalent to speech.

## Rollback / safe promotion

Rollback: revert the isolated PR/commit; the existing teleprompter, microphone and mastered playback paths remain. Review exact HEAD and dependencies before merge. No automatic promotion while physical speech, provider protection and independent reviewer are missing.

R3=<F_ok: bounded authorial synthesis candidate and Android opt-in integration source; F_gap: intelligibility/physical device/independent review/provider; F_next: CI exact-head then physical listening + negative test receipt>.
