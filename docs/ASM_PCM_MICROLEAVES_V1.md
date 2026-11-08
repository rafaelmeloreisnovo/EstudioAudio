# EstudioAudio — ASM PCM microleaves V1

status: IMPLEMENTED_UNTESTED_DEVICE
claim_allowed: false
source_base: main@5c473d6298402a78266d936890be5807daa310f5
branch: refactor/asm-pcm-leaves-20261108
execution_target: ARMv7 `armeabi-v7a` and AArch64 `arm64-v8a` objects, Android user space

## Intent and scope

Each source file implements exactly one stateless operation. There are no internal named C variables, `if`, `while`, `for`, native heap, system calls, C runtime calls, or stack frame in these leaves.

| Module / each ISA | Contract | Registers | Width |
|---|---|---|---|
| `rfa_pcm_abs_u16.S` | `abs((int16_t)input)`, -32768 → 32768 | r0/r1 or w0/w1 | signed PCM16 input; unsigned 0…32768 output |
| `rfa_pcm_sat_add_s16.S` | saturate(A+B) to [-32768,32767] | r0/r1 or w0/w1/w2 | each input must be signed 16-bit extended |

Compiled instructions observed locally (Clang17 cross objects, 2026-11-08):
- ARMv7 abs: 5, sat-add: 3, ELF32 ARM, no relocations/undefined.
- AArch64 abs: 5, sat-add: 8, ELF64 AArch64, no relocations/undefined.
- No device execution and no end-to-end APK integration asserted.

The module names and ELF symbols are ABI addresses, not runtime overhead proven by name length. Source whitespace and comments do not overlay executable bits; compiler and assembler directives plus linker layout control output. Removing conditionals/loops is not an optimization unless latency, size, numerical accuracy and side-channel impact are measured.

## Boundary

Android controls MMU, privileged registers, scheduling, DMA and NUMA policy. The APK must respect the OS; Assembly leaves are *freestanding kernels*, not a bare-metal replacement for Android. Do not access privileged registers or hardcode instruction bytes without ISA/endianness verification.

ABI: no stack frame; only caller-saved registers used; AArch64 x18 untouched; ARMv7 uses ARM state. Save/restore the registers actually required by an ABI rather than pushing all registers linearly; indiscriminate spills add latency. No dynamic imports or hidden mutable state.

The Android, Java, JNI, native C algorithms, packaging and permissions remain unchanged. New `.S` modules are compiled as independent static targets but not called by DSP/JNI. This prevents unverified audio regressions.

## Evidence gates and remaining typed tokens

| Gate / token | State | Next |
|---|---|---|
| CROSS_ASSEMBLY_ELF32/ELF64 | PASS_SCOPED_LOCAL | repeat on exact PR SHA |
| SYMBOL_ALLOWLIST / ZERO_UNDEFINED / ZERO_RELOC | PASS_SCOPED_LOCAL | CI readback |
| HOST_REFERENCE_ORACLE | PASS_SCOPED_LOCAL | differential against executable ARM |
| HOSTED_TOOLCHAIN_DEPENDENCY | REQUIRED_BUILD_ONLY | pin NDK/Clang, compare versions |
| APK_NATIVE_LINK / ARM_V7_DEVICE / ARM64_DEVICE | NOT_RUN | full build, physical replay |
| DSP_BYTE_EQUIVALENCE / ACCURACY / LATENCY_P95 / BINARY_DELTA | TOKEN_VAZIO:EVIDENCE_MISSING | golden vectors + benchmark |
| MMU_PRIVILEGED / NUMA_CONTROL | NOT_APPLICABLE_ANDROID_APP | never claim bare-metal |
| JNI_ACTIVATION | NOT_IMPLEMENTED | add after exact input/output gates |
| PROVENANCE_EXTERNAL | TOOLCHAIN_ONLY_OBSERVED | license audit |

## Rollback and promotion

Main unchanged. Leave branch and PR as draft; no auto-merge, no release. Supersede this receipt when exact-head CI and physical vectors exist. If a regression is demonstrated, revert only the additive integration while retaining the previous source and append-only evidence.
