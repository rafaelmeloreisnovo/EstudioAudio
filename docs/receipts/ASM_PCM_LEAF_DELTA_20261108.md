# μRECEIPT — EstudioAudio ASM PCM leaves — 2026-11-08

μID: MU-ESTUDIOAUDIO-ASM-PCM-20261108-S1
parent: main@5c473d6298402a78266d936890be5807daa310f5
producer: rafaelmeloreisnovo/EstudioAudio
kind: ADDITIVE_MICROLEAVES
scope: 2 DSP scalar PCM operations × 2 ISA = 4 assembly source files, 2 static targets, one allowlist and a cross-ELF gate
SOURCE: app/src/main/cpp/arch/{armv7,aarch64}/
ARTIFACT: cross-compiled ELF32/ELF64 objects in local test only
EXECUTION: local Clang17 cross-assemble + readelf, source oracle in Python
EVIDENCE: ELF32/ELF64 PASS_SCOPED_LOCAL, 0 undefined, 0 relocations, symbol visibility HIDDEN
CLAIM: false for Android runtime, physical audio, speedup, binary size or complete refactor
gap: ARM physical differential vectors, DSP integration, APK artifact/SHA, ARM64 device, latency/binary A/B, CI exact-head
routes(L/O/T/P/C/R/I/E/A): source/ABI/PCM/provenance/Android boundary/real-time/linker/ELF/readback
rollback: keep main intact; draft PR, independently review; supersede with a successor and revert integration if needed
next: exact-head CI readback; ARM32 + ARM64 device smoke; numerical differential & p95 latency
