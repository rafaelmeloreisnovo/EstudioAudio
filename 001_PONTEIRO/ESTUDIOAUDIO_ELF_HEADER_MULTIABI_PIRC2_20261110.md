# EstudioAudio — ELF multi-ABI header contract — 2026-11-10

Status: IMPLEMENTED_UNPROMOTED / DRAFT_HOLD / claim_allowed=false
Method: PIRC-2 N0 scoped host test + 6Sigma/DMAIC, reversible successor.
Baseline: main@79539902741647375191eb9c99801f16960f459e
Target branch: hotfix/elf-header-multiabi-pirc2-20261110

## Material delta
- New ci/elf-header-contract.sh: read-only verifier for ELF class, little-endian data, relocatable type and exact ARM/AArch64 machine.
- New ci/test-elf-header-contract.sh: 2 positive compiled object cases, 6 adversarial rejections (wrong-architecture 32/64, host-as-AArch64, unknown target, empty object, corrupted endianness).
- Modified ci/freestanding-runtime-gate.sh: validates ELF header before existing no-undefined-symbol and no-writable-global checks.
- Modified ci/rafaelia-fast-gate.sh: calls source/host-only falsifier before expensive toolchain/APK stages.
- No changes to DSP algorithms, PCM, Java, JNI ABI, Gradle, START.yml, permissions, APK, release or signing.

## Reproducible local host observation
Toolchain: Clang 17.0.0; GNU readelf 2.44; 2026-11-10.
Command: bash ci/test-elf-header-contract.sh
Observed:
ELF_ABI_HEADER_FALSIFIERS=6_REJECTED
ELF_ABI_HEADER_VALID=ARMV7,AARCH64
ELF_HEADER_TEST_SCOPE=HOSTED_OBJECT_INSPECTION_ONLY
Also bash -n succeeded. This is not exact-head GitHub CI and does not execute ARM objects.

## Existing prior evidence (not this branch)
EstudioAudio main exact commit 79539902741647375191eb9c99801f16960f459e: GitHub Actions run 42505076986 concluded success, including debug APK artifact and binary receipts. This run predates this change; cannot validate these new checks.

## Evidence gates and remaining N5
- Git branch blob readback for all 4 changed files: confirmed.
- Exact-head workflow success: NOT_OBSERVED at receipt creation.
- Full two-ABI NDK gate with this branch: NOT_RUN.
- Android APK build from this branch: NOT_RUN.
- Real Android install / mic / speaker / latency: NOT_RUN.
- Source authorization and licensing: repository AGENTS.md and SPDX policy preserved.
- Claims outside inspected object-header contract: false.

## Rollback and succession
Close draft PR to leave main unchanged, or revert only changed files after human-reviewed merge. Preserve receipt and previous source identities; no deletion or upstream rewriting.
Next: confirm exact-head fast and full build gates, compare artifact identities, independent device receipt and provider protection separately.
R3=<F_ok: host positives and six negative falsifiers plus Git readback; F_gap: exact-head CI, full APK and physical Android; F_next: review gated PR with no auto-merge>.
