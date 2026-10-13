# PT-BR Authorial Speech V1 — safe grapheme/phone bridge
2026-10-13 | status=SOURCE_IMPLEMENTED; claimant status=NOT_VALIDATED_SPEECH
SOURCE=app/src/main/cpp/rfa_ptbr_core.c + .h
PARENT=docs/TELEPROMPTER_AUTHORIAL_VOICE_GUIDE_V0.md
PREDECESSOR=existing per-character V0 maintained for regression/rollback
CORE=-nostdinc -ffreestanding -fno-builtin -fno-common, no heap, libc, libm, Android, syscalls or shared mutable state
HOST_GATE=ci/ptbr-core-gate.sh
ABI=public_symbols_v1.txt
TYPE_EMPTY=TOKEN_VAZIO_NOT_ZERO

## What it is
Small and explicit PT-BR orthography-to-phoneme-event *approximation* with caller-owned
cursor/state and integer-only PCM 16kHz. Supports common accented vowels and ç,
ch/lh/nh/rr/ss/qu/gu contextual digraphs; rejects unsupported UTF-8, emoji,
digits and truncated encodings rather than silently substituting meaning.
Deliberate limits: 2048 input bytes, 4096 PCM output frames/call. Every event
has byte span, phone ID and frames. Reproducible whole vs chunk output.
No dictionary, lexical stress engine, full syllabifier, vowel coarticulation,
formant resonator, pitch contour, codec, latency guarantee or human speech
intelligibility baseline. This is still a *synthetic phonetic guide*, NOT natural TTS.
Do not claim accessibility/medical/education voice quality.

## Host/device separation
C core and host tests: no operating system I/O or third-party synthesis engine.
JNI is a hosted adapter. Android must provide AudioTrack and the OS/device HAL.
A bare-metal port must provide its own device clock, memory, DAC/audio driver,
and display interface. Hardware/ABI portability is not one binary for all.

## Falsification / gates
1. Decode negatives + invalid UTF-8 must fail without cursor/output mutation.
2. Chunked waveform bytes must equal whole-block waveform bytes.
3. Compiler must emit zero undefined symbols + no writable B/D globals.
4. Explicit ARMv7/AArch64 cross objects required; host cannot prove hardware.
5. Exact APK build + device replay + blind listening intelligibility trial before TTS claim.
6. Reviewer/source ownership/provider protection remain independent.
No promotion without gates. Rollback revert this PR successor.
