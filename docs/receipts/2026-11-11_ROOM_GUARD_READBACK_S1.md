# μRECEIPT — EstudioAudio ROOM Guard V1 — 2026-11-11

Copyright (c) 2026 Rafael Melo Reis  
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

**μID:** MU-ESTUDIOAUDIO-ROOM-GUARD-20261111-S1  
**Date:** 2026-11-11 (UTC)  
**Scope:** source write and GitHub blob readback only; **not** a physical-device quality claim.  
**Producer:** https://github.com/rafaelmeloreisnovo/EstudioAudio  
**Branch:** `feature/room-guard-multipoint-20261111` (draft/hold; no merge).  
**PR:** https://github.com/rafaelmeloreisnovo/EstudioAudio/pull/81  
**Exact source HEAD at receipt creation:** `50f91d9bd8c914e5354f52db6f3afda983de1212`  
**Baseline main at PR opening:** `654fa0ba450a2a5b0aae5f6763d69fad7e1b2bcb`  
**Route:** [START HERE V2.2](https://docs.google.com/document/d/1T4OIN2RC1I-KFsdaQTCqieeZUGYBXissWrdrZMG0DN8/edit) → repo `AGENTS.md` → `docs/START_HERE.md` → implementation.

## Readback evidence (provider files on exact branch)

| Path | Observed Git blob SHA |
| --- | --- |
| `app/src/main/cpp/rfa_room_guard_core.h` | `1c602caffaaa70c21e3e8c3064c24f0860e2fbdf` |
| `app/src/main/cpp/rfa_room_guard_core.c` | `6f15e86db54d122c3b048750f7d84d166350af42` |
| `native/tests/room_guard_smoke.c` | `790c9864b37575ec8dbde27bce24715c85b33fb8` |
| `ci/room-guard-gate.sh` | `19365035f46664b30835724c6405cc9f4319ae06` |
| `docs/ROOM_CAL_NOISE_TRUE_RTA_ROUTE_V1.md` | `bf0e03d764beba3cb5465f35d3d43ae9bd61b704` |
| `app/src/main/cpp/CMakeLists.txt` | `9cb92d1f66045570c12289c87b66ab3040859544` |
| `ci/freestanding-runtime-gate.sh` | `b671c4510afcec1f7bd04c55c24a73dac6deb14a` |
| `native/abi/public_symbols_v1.txt` | `4238ccf8ec828e6c3f82d5ef4c0ab929fc8b97ef` |
| `ci/rafaelia-fast-gate.sh` | `03523b8deb89c495963aad90b3ef937902cb7792` |

**Git blob SHA is NOT SHA-256 of the raw file.** `SOURCE_READBACK=CONFIRMED_SCOPED`, 9/9.  
**Source change:** a freestanding fixed-point (Q8) planner, 16 spectral bands × max 8 listener positions, median and spread guards, cuts only with capped gain, typed refusal, zero boost, no automatic EQ. Adds own unit/adversarial test plus exact-source quick-gate wiring; original PCM, Android UI, permissions and filter application are untouched.

## Workflow observation — scoped, at receipt creation

`https://github.com/rafaelmeloreisnovo/EstudioAudio/actions/runs/43113210671`  
- `event=pull_request`, `head_sha=50f91d9bd8c914e5354f52db6f3afda983de1212`.
- `00 · plan / route=SUCCESS`.
- `gate / quick=IN_PROGRESS` at observation; `FAST_HOST_PASS=TOKEN_VAZIO_PENDING`.
- `gate / build=SKIPPED`, no APK; `PHYSICAL_ANDROID=NOT_RUN`.
- No automated release, main update, permissions change, or service connection mutation.

If CI terminates, write an **append-only successor** with conclusion and failing/passing step evidence. This record must NOT be silently rewritten into PASS.

## Semantic provenance / boundaries

- `DeltaP≈0.18` source is RMRCTI stability association `P(stable|peak)-P(stable|nonpeak)`, not Pa/dB nor a room-EQ coefficient. Source: https://github.com/instituto-Rafael/relativity-living-light/blob/main/receipts/2026-09-22_RLL_DYNAMIC_STRUCTURAL_GEOMETRY_CONTEXT_V1.md
- `r≈70` = `TOKEN_VAZIO_INDEX_AROUND_70`; acoustic meaning unverified. Do not conflate with `CUSTODY70`.
- Absolute SPL, microphone transfer calibration, room impulse response deconvolution, calibrated RTA, actual FIR/IIR application, validated NLMS noise removal, human listening results, physical RT60 and standards certification = `NOT_RUN` / `NOT_IMPLEMENTED` / `TOKEN_VAZIO` as applicable.
- Literature is research rationale, not the implementation's result. The GitHub source change is not a physical microphone measurement.

## Gates and rollback

- G0 source + readback **CONFIRMED_SCOPED**.
- G1 quick host unit test **PENDING** (exact workflow above).
- G2 ARMv7/AArch64 strict object/ABI **NOT_RUN** for this head.
- G3 APK **NOT_RUN**; G4 Android physical **NOT_RUN**; G5 calibrated acoustic evidence **TOKEN_VAZIO**.
- `claim_allowed=false`; **draft title does not enforce ruleset protection**.
- Rollback: close draft PR without merge, leaving `main` untouched. Never delete source history.

**R3:** F_ok=branch/source/readback and PR #81 confirmed; F_gap=exact-head quick conclusion and every physical gate; F_next=inspect exact quick result, remediate any scoped C/gate failure in branch, then full ABI/APK and controlled physical test if authorized.
