# EstudioAudio — reconciliação da PR #64 e orçamento de bytes (2026-10-23)

status: SOURCE_RECONCILIATION_PROPOSED; exact_HEAD_CI=NOT_OBSERVED; APK=NOT_BUILT_FOR_THIS_HEAD; PHYSICAL_REPLAY=EVIDENCE_MISSING
claim_allowed: false
source: GitHub EstudioAudio PR #64; main@38d62708b31d387ebf2b4f2d89fb57ee430fd726; predecessor PR head@9a6915e20e5094051bd6680f8eed2a2293e72ba7
parent: docs/PHYSICAL_PLAYBACK_OBSERVER_V0.md
provider: issue #66 (unprotected main, ruleset readback empty); owner/admin effective enforcement still P0
rollback: revert the PR #64 reconciliation merge commit after dependency review; never rewrite or delete the historical branch or source receipts

## INTENT / CURRENT_STATE / FACTS

Resolve PR #64 against the newer main preserving the merged PT-BR, timbre, JNI, ZIP32 and documentation improvements. Preserve the original standalone playback observer (C) and synthetic falsifiers without representing its Android integration as implemented. Do not silently replace changed MainActivity, narration V2, runtime gate or ABI with the older PR copies.

**Historical device executions:** the author reports having installed and run previous APKs. This is meaningful USER_REPORTED_HISTORICAL_EXECUTION, not "zero" and not a hash-bound receipt for this exact merge candidate. No APK bytes/physical playback from the successor head are claimed by this document.

The new UI button inserts a script; it does NOT invoke `rfa_playback_observe_mono` from the recorder. Source-level observer != user-visible live acoustic diagnosis. Until explicit recorder/JNI wiring, token `ANDROID_OBSERVER_WIRING=NOT_IMPLEMENTED`.

## Resolved by source reconciliation

1. Preserve main as the winning baseline for overlapping files, and integrate only additive observer deltas.
2. Canonical display name `EstudioAudio`; preserve package `io.rafaelia.audiostudio`, technical identifiers, existing filenames, ABI, commits and historical name references. Mass-renaming is unsafe without an install/ABI compatibility plan.
3. Index this contract from README and START_HERE. Historical static SHA/PASS statements are historical, not current-state evidence.
4. DSP is a general technical acronym, not demonstrated infringement. Avoid premature destructive renames. If a specific right is later identified, record source/owner/license/jurisdiction and use additive compatibility gates.
5. Preserve RAFCODE source SPDX, explicit platform/compiler/third-party boundaries; do not claim exclusive authorship of toolchain bytes.
6. Extend AGENTS for AI/human change discipline and no unverified provider or CI claims.
7. Typed TOKEN_VAZIO contract: `UNKNOWN_SOURCE`, `NOT_OBSERVED`, `NOT_RUN`, `NOT_IMPLEMENTED`, `EVIDENCE_MISSING`, `AUTHORITY_UNRESOLVED`, `CONFLICT_UNRESOLVED`, `NOT_APPLICABLE_WITH_REASON`.
8. NIST/IEEE/RFC mapping remains candidate research, never a false certification or unspecified legal obligation.
9. Require falsifier/independent-review record before semantic claims.
10. Keep promotion distinct from merge and branch protection distinct from a descriptive PR title.

## Compact binary / freestanding control

- Build path: existing GitHub Actions `gradle :app:rafaeliaDebugApk`, NDK/Clang at Android boundary; no Python packager.
- C core: `-ffreestanding -fno-builtin -nostdinc` and zero undefined-sym tests; no new libc/heap/system headers or JNI dependencies in the observer C.
- Linker: preserve `-ffunction-sections -fdata-sections -Wl,--gc-sections -Wl,--exclude-libs,ALL`. Renaming symbols, reordering translation units, comments and layout do not imply a smaller APK; inspect exact resulting ELF sections, map and archive bytes before any optimization.
- Full build **measures** `apk_size_bytes`, `apk_sha256`, source SHA, run ID and archive-entry digests in the existing binary-origin receipt. Reference `1048576` bytes (1 MiB) is a *soft comparison band*, NOT an evidence-backed passed upper bound. A historical ~300 KB report also needs immutable APK hash/receipt before numeric comparisons.
- No size threshold relaxation can hide a genuine previous known PASS. Once the exact baseline is measured, record deltas and choose an evidence-backed budget with owner approval.
- Smoke C and byte-size measurement are CI-host steps. `CI_HOST_PASS != APK_PASS != PHYSICAL_PASS`.

## Remaining falsifiers/gates

| Scope | Required discriminant | State at source creation |
|---|---|---|
| L0 C | original + adversarial smoke (long count, 96 kHz, short buffers); undefined symbols | SOURCE_PRESENT; exact-head runner NOT_OBSERVED |
| Full APK | Actions full build for exact PR merge HEAD; SHA256 + bytes; ARMv7/AArch64 ABI & installable layout | NOT_RUN |
| Linker | section/ABI comparison vs known good SHAs; no accidental symbol loss or changing package ID | EVIDENCE_MISSING |
| Android integration | explicit reviewed recorder→JNI→observer pipeline and capture bounds | NOT_IMPLEMENTED |
| Physical | same APK hash/phone/build, mic/speaker test, permission/consent, synthetic and real acoustics | EVIDENCE_MISSING |
| Rights | file SPDX + dependency matrix + independent review | PARTIAL_SOURCE; review NOT_OBSERVED |
| Promotion | provider rulesets + required checks + effective negative merge-denial + reviewer | AUTHORITY_UNRESOLVED; issue #66 OPEN |

## Decision and counter-hypotheses

H1: smallest additive merge retains main's features and introduces observer source with no new APK packaging method. H0/falsifiers: CI failures, missing ABI symbols, altered PT-BR scripts, unexpected APK size, lost timbre flow or physical false claims. The result of a source-only merge is NOT a released or experimentally verified audio feature.

R3=<F_ok: reproducible source delta, native CI gates and size receipt route; F_gap: exact-head and physical tests, active provider enforcement, independent review; F_next: inspect CI on merged PR HEAD, compare bytes and ELF, then opt-in JNI and real-device receipt without automatic promotion>

## Successor — topology falsifier and minimal fix

- First CI: run `39899333240`, job `gate / build` failed at `30 · single-root topology` BEFORE compiling APK: `ACTIVE_WORKFLOW_COUNT=2 expected=1`.
- Root cause: the original observer PR contributed a second active YAML under `.github/workflows/` although `START.yml` is the repository's intentionally single active workflow. The observer source-host smoke separately passed in run `39899333365`, but this did not bypass the topology gate.
- Resolution: relocate `.github/workflows/playback-observer-freestanding-l0.yml` to `ci/archive/workflows/playback-observer-freestanding-l0.yml` preserving byte-identical blob `37ece4241b1273110eb917345fb8db853d5461ca`. Git tracks the move; no historical material is erased. The original C tests remain invoked by `ci/rafaelia-fast-gate.sh` and `ci/rafaelia-pipeline.sh quality` via `ci/playback-observer-core-gate.sh`.
- Deliberately **do not** relax `ACTIVE_WORKFLOW_COUNT=1`; changing the topology invariant would hide the regression. Next: new exact-HEAD full CI, inspect APK and binary size receipt, then host/Android evidence separately.
- Status at hotfix creation: successor exact-head full CI = `NOT_OBSERVED`. Promotion and release remain blocked; issue #66 remains open.

## Successor — narration V2 immutability falsifier

- Second full CI: run `39899696059` passed topology, Java/Android source tasks and audio-session gate, but stopped in `40 · quality capability` with `NARRATION_V2_LOCK`: pinned blob `0f48f96e5d6512282fa859c122bbf96887aadb67` vs modified blob `03877c62510184f323502ebd552c6d74256a9ab2`.
- Root cause: observer preset method was appended to the V2 narration file. This changed its source hash despite preserving existing text. Do not disable or repin the lock.
- Minimal resolution: restore the exact original V2 file/blob `0f48f96e5d6512282fa859c122bbf96887aadb67`, inline the same observer-script constant in existing `MainActivity` click handler; no extra Java class, permission, runtime component or L0 C mutation.
- Next CI exact-HEAD needed: verify lock and full build to APK, then read size/ABI/physical separation. Promotion remains blocked.
