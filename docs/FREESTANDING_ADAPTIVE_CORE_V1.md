# Standalone authorial bytes + bounded adaptation V1

Producer: EstudioAudio. Baseline main@78f6eb2a4c36c2de30a71fc19e429364ad09c140. Successor preserves PR58 source and leaves the separate PR59 documentation branch untouched. Status: source implemented + locally executed scoped gates; exact-head CI pending until its receipt. No promotion/merge authorization is inferred from core tests.

## Executable boundary

| Surface | Implemented action | Remaining authority/dependency |
|---|---|---|
| SHA256/CRC32 | C streaming caller-state primitives, ported from project LowSha256/RfaStoredZip; byte/KAT oracles | Standard algorithms, not a novel hash; CRC is not authentication |
| ZIP32 STORED | Caller-buffer serialization, exact little-endian layout, ASCII path safety, 32 entries, 8 MiB/entry, 64 MiB/archive | Input/output buffers must be disjoint and inputs immutable during the call; no ZIP64/compression/encryption |
| NLMS adaptation | Compare adapted trial to frozen generic baseline on the same block; accept only when clipped output sum-of-squares does not increase | One numeric metric, not speech quality/physical correctness; two executions per trial, no speed claim |
| Rollback/fallback | Caller-owned checkpoints restore coefficients/history/position, then execute frozen baseline; rejection does not leak candidate state | Checkpoint is in-process algorithm state, not app/package/storage rollback |
| Standalone ELF | 18 core units independently compiled, zero undefined imports and writable globals, linked ELF relocatable and oracle shared object with no DT_NEEDED | Compiler/linker and hosted test runner are build/test dependencies; relocatable ELF is not a bootable bare-metal executable |
| Tail/shadow | -fno-optimize-sibling-calls plus -Wshadow -Werror in core build/ARM/portability gates | Source identifiers retained for ABI/provenance; no mutable named global state. This does not mean registers require no source identifiers |
| ART/JNI/DEX | Newly ported APIs work without ART/JNI. Existing Java implementations remain byte oracles and working app path | APK lifecycle/UI/DEX/runtime and current native DSP bridge are platform boundaries, not removed |
| SDK/NDK/Gradle/CI | Standalone gate requires none of them; existing APK path retains them | No authorial Android packager/compiler/DEX runtime claimed; full APK CI remains mandatory |
| NUMA/architecture | Portable generic C remains the baseline; ARM32/64/x86/RISC-V object gates retain scope | NUMA discovery/placement and target physical execution TOKEN_VAZIO; no CPU/NUMA optimized route is fabricated |
| OS/radio/audio hardware | No OS/native-library call enters new core | AudioRecord/Track, media codecs, sensors, location, radio metadata stay protected platform I/O |

## Actual commands

`CC=gcc python3 ci/core-standalone-gate.py` runs without JDK/Gradle/SDK/NDK/Android. It rejects ABI drift, undefined symbols and named writable storage; then links without libc (`-nostdlib`) and verifies no DT_NEEDED. Hosted Python ctypes loads the test artifact; that host oracle is not bare-metal physical evidence.

`bash ci/core-byte-parity-gate.sh` additionally compares complete C ZIP bytes and SHA digest with the exact project Java classes. This optional cross-language oracle needs JDK only for testing, not for the C algorithm. Both CI lanes require it before promotion.

`bash ci/core-adaptive-sanitizer-gate.sh` exercises max block 4096, exact known SHA/CRC vectors, malformed state, oversize blocks and failed ZIP capacity with ASan+UBSan. Existing six core smoke programs and parser/ARM gates remain mandatory.

The normal APK/JNI path is preserved. No new JNI consumer is added for the new digest/ZIP/guarded-NLMS APIs, so existing bytes/behavior are not silently switched. These functions are independently usable C APIs and may be dead-stripped from an APK until a separately tested consumer is connected. No callbacks, heap, thread/runtime API, syscalls or dynamic downloads in the C core.

## Adaptation contract

`rfa_nlms_trial_block_q15` uses a caller-owned `rfa_nlms_trial` (~9 KiB), 1..256 taps and 1..4096 samples. All state, primary/reference/output/scratch buffers must be disjoint and valid for their declared capacities; lifetime and aliasing are caller obligations. No adaptation runs when allow_adapt=0, reference energy=0 or mu=0. Corrupt position/taps/mu/epsilon and oversized/null inputs reject before mutation. Ties permit adaptation; future-block stability is not implied. Error energy is calculated after existing Q15 saturation; clipping/voice preservation require additional scientific/acoustic gates.

Fail-safe=frozen generic processing; rollback=restore the failed trial state; fallback=produce exact baseline output and its exact history progression. Capability routing beyond generic remains absent until a real alternative and parity/target tests exist.

## Measurements / receipt

Local GCC x86_64: 18 objects, zero undefined imports/no writable globals; ABI manifest exact; relocatable/shared core DT_NEEDED absent; six existing host smoke programs PASS. SHA/CRC 670 streaming boundary/payload/split vectors match Python hashlib/zlib. ZIP exact independent layout + zipfile CRC/payload + insufficient-capacity/duplicate/traversal/count negatives PASS. Adaptive 2000 deterministic adversarial blocks, 1593 rejected trials; every rejected trial matched independent frozen baseline output/weights/history/position. Accept/freeze/no-reference/corrupt-state/oversize/alias-equality negatives PASS. ASan+UBSan PASS locally with only LSan disabled because this container cannot inspect host processes; default sanitizer behavior remains in CI. Java-byte oracle and new ARM targets require remote CI; do not turn local GCC into NDK/physical proof.

Physical MotoE7 device-generated source receipts are present in canonical S5 but do not validate this successor core. External calibration, independent installed-byte readback, authorial signer authority, NUMA tests and provider main protection remain separate gaps. Current provider main protection was false on the observed baseline; no provider mutation or merge occurs here.

Rollback of source: revert only the successor PR. Original Java writers, raw data, ZIPs, receipts and branch histories are preserved. SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM. R3=<local scoped verification,exact-head/physical/consumer/provider gaps,next exact-head CI and reviewed optional consumer>.
