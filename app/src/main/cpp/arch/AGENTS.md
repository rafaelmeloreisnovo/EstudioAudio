# AGENTS.md — ASM PCM microleaves
- Authority inherits `/AGENTS.md` and `/app/src/main/cpp/AGENTS.md`.
- Every `.S` source owns exactly one leaf operation. No calls, system services, heap, mutable state, libc or dynamic relocations.
- Objects must be ELF32 ARMv7 or ELF64 AArch64. Use only ABI caller-saved registers. AArch64 `x18` is reserved on Android.
- Stable external ABI symbol names are mandatory even if local registers are numeric.
- Caller supplies correctly typed PCM16 operands; no unchecked pointers or hidden global state.
- `native/abi/asm_public_symbols_v1.txt` is the assembly allowlist, distinct from C-only standalone host gate.
- ARMv7 objects use ARM state (`-marm`). ARMv7 and AArch64 versions must be semantically equivalent; cross-object compile does not prove this.
- Do not integrate into real-time DSP until device differential vectors, clipping, latency, ELF/stack checks and A/B regression gate exist.
- Existing Android/JNI/Gradle/SDK boundary remains unchanged. License and source lineage preserved.
