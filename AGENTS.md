# AGENTS.md — Rafaelia Audio Studio

## Authority

This file governs repository-wide work. More specific `AGENTS.md` files govern their own subtrees.

## Canonical invariant

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
OBSERVED_UNPROMOTED != PASS
```

Never replace missing evidence with a plausible value.

## Route

```text
INTENT -> CURRENT_STATE -> READ_MIN -> ROUTE -> SOURCE_MIN
-> AUTHORITY -> ACT -> EVIDENCE -> WRITE_MIN -> R3
```

Read the smallest authoritative surface first. Expand only for a missing source, contradiction, unresolved authority, missing evidence, or explicit request.

## Repository roles

- Android/Java: platform UI, microphone, storage, codecs, sensors and packaging.
- JNI: narrow platform-to-core boundary.
- C core: authorial freestanding fixed-point algorithms.
- docs: contracts, claims, protocols, receipts and publication.
- workflows: reproducible gates; workflow PASS is evidence only for executed gates.

## Claim rules

A feature can be documented as implemented only when the source exists.
A build can be documented as PASS only after the matching run completed successfully.
Physical microphone/speaker/sensor claims require a physical receipt.
Absolute SPL requires a physical acoustic reference.
Sensor vibration observations are not hardware-fault diagnoses.

## Change discipline

1. Preserve raw/source artifacts.
2. Prefer small micromodules and explicit state.
3. Keep Android services outside freestanding C.
4. Extend ABI manifest when adding a public C symbol.
5. Add or update smoke vectors for new primitives.
6. Record material FAIL -> correction -> PASS chains.
7. End substantive work with `R3=<F_ok,F_gap,F_next>`.

## Licensing

New source should carry:
`SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1`.

Research/evaluation rights and commercial rights are governed by the repository license documents; technical controls do not substitute for legal enforcement.

<!-- RAFAELIA_FREESTANDING_POLICY_V1 -->
## Freestanding / dependency-minimization gate

Default engineering direction: move implementation toward the smallest **authorial, low-level, deterministic and freestanding** core that the target can truthfully support. This is a direction plus gate, not permission to relabel hosted code as freestanding.

Rules:

- Prefer repository-owned primitives over a new external dependency when the local implementation can be smaller, auditable, testable and maintainable.
- Keep pure/freestanding cores free of network access, package managers, dynamic downloads, hosted runtimes, GC, heap, libc, syscalls and platform APIs unless the owning scoped contract explicitly permits that item.
- Keep unavoidable OS/Android/JVM/JNI/POSIX/toolchain integrations at an explicit hosted/platform boundary. A build-time tool is not automatically a runtime dependency, and neither may be hidden.
- Any new or retained external dependency must state: purpose, owner/source, license, version or immutable identity when available, build-time/runtime scope, reason it cannot yet be replaced, tests/evidence, rollback and the smallest credible authorial replacement path. Unknown fields remain `TOKEN_VAZIO`.
- Never copy/vendor third-party code and call it authorial. Provenance and license survive transformation.
- Do not add a dependency merely to shorten code. Optimize for reconstructibility, bounded complexity and falsifiable behavior, not dependency-count theater.
- Node.js 20 is EOL and MUST NOT be introduced or retained as an active CI/tooling baseline. If Node is actually required, use a maintained LTS line; the repository migration baseline on 2026-10-07 is Node 24. Do not add Node to a repository that does not need it.
- For low-level changes, inspect emitted artifacts/ABI where applicable; source resemblance alone is not freestanding evidence.
- Preserve `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`, `TOKEN_VAZIO != 0`, and `IMPLEMENTED_UNTESTED != PASS`.
- Before merging a dependency-reduction change, prove that required behavior did not silently disappear. Removal without equivalent behavior or an explicit scope reduction is a regression, not a freestanding win.

For every material dependency delta record:

`boundary -> previous dependency -> replacement/retention reason -> execution -> evidence -> rollback -> remaining TOKEN_VAZIO`.

<!-- /RAFAELIA_FREESTANDING_POLICY_V1 -->

## EstudioAudio compact-binary and evidence contract — successor 2026-10-23

- Canonical project display name: **EstudioAudio**. Historical names and existing Android package names, file paths, JNI/public ABI symbols must not be mass-renamed without compatibility evidence. A technical field term such as DSP is not, by its presence alone, a proven third-party copyright infringement; record any specific trademark/license concern with identified right, jurisdiction, source, and review.
- Compile and package APKs using the existing GitHub Actions Gradle/NDK chain. Host C smoke executables in that same CI are scoped engineering tests, not APK/device runs. Do not use Python to compile or package Android artifacts.
- Freestanding applies to the pure C core, **not** Android/JNI/Gradle/AudioTrack packaging. Retain the current linker/compiler section flags and compare exact artifacts before optimizing symbols, source order or layout. Treat names, comments, layout and flags as separate measured variables; do not assume a byte reduction.
- In a full APK CI build, record exact APK byte count + SHA256 + source SHA/run and the 1 MiB reference band. Above the reference is a reviewed deviation, not an automatically fabricated PASS or an untested release claim. The 1 MiB reference is provisional until an exact baseline is observed.
- Previous APK installation/playback described by the author is USER_REPORTED_HISTORICAL_EXECUTION unless an exact matching APK hash, device and receipt are present. It is not ZERO or NOT_RUN historically; it does not validate a new commit.
- Typed absence: UNKNOWN_SOURCE, NOT_OBSERVED, NOT_RUN, NOT_IMPLEMENTED, EVIDENCE_MISSING, AUTHORITY_UNRESOLVED, CONFLICT_UNRESOLVED, NOT_APPLICABLE_WITH_REASON. Every material token must carry gate/next/evidence pointer. UNKNOWN is not zero.
- No automatic main merge, release, license reinterpretation or PHYSICAL_PASS from source-only tests. Existing issue #66 requires native provider enforcement proof. Human/integration review remains separate from CI.
