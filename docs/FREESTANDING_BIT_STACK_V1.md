# Bounded bit/symbol stack — freestanding V1

New project-owned C primitive uses existing `rfa_core_types.h`, caller storage and bounded integer loops. Widths 1..32, byte-addressed bit packing, push/pop LIFO, no allocation/system headers/libc/syscalls/VM. No dictionary interpretation, UTF-8 transformation or physical meaning is invented. Symbol-to-integer mapping and width sequence belong to the caller. Exact capacity checks precede writes; invalid operation keeps state/output unchanged. Storage and state/output must not alias; this is a single-owner API, not a lock-free concurrent stack.

This small primitive is not the full BitStack World Model, overlay geometry, ECC, witness or compression engine. It is linked into the manifold static library and indexed in the ABI manifest; no JNI/UI consumer is added and dead-strip may omit unused code from the final APK. No functional DSP path was replaced.

Execution: strict GCC host object compile of all 16 cores, zero undefined imports, ABI manifest equality, six smoke programs PASS. Stack vectors cover widths 1..32 at unaligned bit offsets, full capacity, LIFO ordering, overflow/underflow, invalid widths/symbol range/null/corrupt state and storage guards. Address+undefined sanitizers PASS with local `ASAN_OPTIONS=detect_leaks=0` (container forbids LeakSanitizer process inspection; no allocator exists in this core). CI uses its default sanitizer settings.

Portability: existing mandatory NDK ARMv7/AArch64 object gates include the new module. New Clang x86_64 ELF + RISC-V ELF object gates require zero imports/no persistent mutable globals. These are compile/ABI tests, not runtime or full operating-system port claims. Windows/macOS/iOS packaging and physical runtime remain NOT_RUN; Android/JNI remain platform dependencies.

Reference lineage: producer EstudioAudio source and AGENTS; scoped reading of RAFAELIA private bit-addressable specification, Termux RAFCODEPHI freestanding controller Makefile and Vectras producer boundary AGENTS. No external source copied, no VM/QEMU/payload/Gradle reclassified as authorial freestanding code.

Rollback: revert successor PR; preserve prior sources and receipts. Existing audio behavior must pass the same mandatory core/Java/Android checks before promotion.
