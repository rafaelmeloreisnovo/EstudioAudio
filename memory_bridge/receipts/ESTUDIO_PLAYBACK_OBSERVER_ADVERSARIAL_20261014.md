# ESTUDIO · Playback observer adversarial successor — 2026-10-14

schema: rafaelia.audio.observer.adversarial.v1
parent: memory_bridge/receipts/ESTUDIO_PHYSICAL_PLAYBACK_OBSERVER_V0_20261013.json
source: https://github.com/rafaelmeloreisnovo/EstudioAudio/pull/64
branch: feature/physical-playback-observer-v0-20261013
source_fix_commit: d416cbcbdb1d2b86a960b9ad6dcc53bf57c02e1d
falsifier_commit: 15ddd8012371a95b8c3d322b983accfcd15939a5
ci_gate_commit: 7d55a6c49d5a141785ccdd424236c0b5ceaad846
docs_commit: d1312fb234f3ad2cda9e992133bb5f47765a6a43
claim_allowed: false

## Precise delta

- 8 window energy accumulators promoted from unsigned 32 to unsigned 64 bits (no library calls).
- Bucket index multiplication by 8 replaced with at most seven integer threshold transitions using ceil(k * count / 8), constructed with 32-bit quotient and remainder.
- Silence minimum is ceil(sample_rate / 50), representing 20ms in sample frames for any positive integer rate.
- Original synthetic fixtures preserved; independent adversarial fixture added in CI; no changes to JNI, microphone, PCM files or Android permissions.
- PR #64 converted to draft for advisory containment (not equivalent to native provider enforcement).

## Falsification execution (local host, source logic reconstructed from GitHub branch)

- Clang `-std=c11 -O2 -ffreestanding -fno-builtin -nostdinc -Wall -Wextra -Wshadow -Werror`.
- Former source, 17,600,000-frame synthetic clip, high first bucket: FAIL(exit 2), acoustic shift missed by wraparound.
- Former source, 96kHz 1100-frame silent gap: FAIL(exit 4), gap incorrectly counted as >=20ms.
- Corrected source, combined long/high-rate/small-buffer fixtures: PASS(exit 0).
- Corrected core object: `nm -u` empty on this host.
- Local test confirms source-level behavior in a transcribed reconstruction, not a downloaded byte-identical repository checkout. Independent exact-HEAD CI is required.

## Non-equivalence / open gates

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
- New PR head: source and tests written, **CI on exact successor SHA = NOT_OBSERVED** at creation.
- HEAD APK and ARM32/ARM64 binary builds = NOT_RUN_OR_NOT_OBSERVED.
- Android physical capture and observer JNI/recorder wiring = NOT_IMPLEMENTED.
- Environmental sensor data (temperature, RH, barometer) = TOKEN_VAZIO; no thermodynamic correction used.
- Speaker, provider and listener identity = TOKEN_VAZIO; human intelligibility = NOT_RUN.
- Independent review/provider main protection = NOT_PROVEN; full CI on main failed at provider verification while build job succeeded.
- No merge/release authorised by this receipt.

## Next and rollback

Retain draft + CI gate; require exact-HEAD CI run, inspect ELF/ABI, add separately reviewed recorder-to-observer boundary, capture device hash-bound receipts, and close owner/admin server-enforcement issue #66. Roll back successor commits on PR #64 only if a regression is proven. Do not overwrite past evidence.
