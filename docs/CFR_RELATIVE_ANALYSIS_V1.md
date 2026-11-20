# CFR Relative Analysis V1

Copyright (c) 2026 Rafael Melo Reis  
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

**State:** `IMPLEMENTED_LOCAL_TESTED / REMOTE_CI_PENDING`

## Purpose

This layer derives bounded, reproducible information from an already aligned CFR capture without converting relative measurements into absolute acoustic claims.

```text
sync alignment
-> exponential sweep window
-> 16 equal-time/log-frequency energy windows
-> relative power ratio Q20
-> post-sweep tail
-> backward-integrated relative decay
-> EDT/T20/T30 candidates when dynamic range permits
```

The original reference PCM and microphone response remain preserved in the same CFR. Derived data is additive; it does not replace the source capture.

## 16-band relative sweep profile

`rfa_sweep_band_profile_q20` divides the exponential sweep into 16 equal-time windows. Because the sweep frequency grows exponentially, the windows are approximately logarithmic in frequency, but they are **not** a calibrated FFT/STFT filter bank.

For each band `k`:

```text
E_ref[k]  = sum(reference^2)
E_resp[k] = sum(response^2)
R_q20[k]  = E_resp[k] / E_ref[k]
```

`R_q20` is a **power ratio** with 20 fractional bits. A UI may display `10*log10(R)`, but this is a relative speaker-room-microphone-ADC path only.

## Relative decay profile

`rfa_decay_profile_q30` estimates the tail noise energy, performs a backward energy integration and searches threshold crossings at -5, -10, -25 and -35 dB relative to the corrected initial integrated energy.

When sufficient dynamic range exists:

```text
EDT candidate = 6 * time(0 -> -10 dB)
T20 candidate = 3 * time(-5 -> -25 dB)
T30 candidate = 2 * time(-5 -> -35 dB)
```

These are relative estimators. They are not promoted to ISO 3382 conformance without controlled acquisition, geometry, noise criteria, repeatability and the required standard-specific evidence.

## CFR chunks

A V1 analyzed relative CFR writes:

```text
WAVE
CAL
PCM reference
PCM response
SPEC relative 16-band profile
ROOM relative decay profile
```

`SPEC` and `ROOM` are derived artifacts linked to the preserved PCM source inside the same container.

## PCM16 rail quality gate — isolated hotfix, 2026-11-20

A captured response can contain exact digital-rail samples (`-32768` or
`32767`) even when the reference sweep does not. The platform edge observes
these samples **after microphone capture** and preserves the original PCM/CFR:

```text
PCM_INVALID_OR_EMPTY -> TOKEN_VAZIO_INVALID_CAPTURE
PCM_RAIL_SAMPLES > 0  -> REVIEW_REQUIRED_PCM_FULL_SCALE
PCM_RAIL_SAMPLES == 0 -> NO_PCM_FULL_SCALE_OBSERVED_NOT_CALIBRATED
```

This is a *data-quality observation*, **not** a diagnosis of analogue clipping,
hardware damage, audible dropouts or absolute SPL. A zero count does not prove
an unclipped analogue path. Review-required sweeps retain the raw CFR and the
16 relative time-slice metrics for inspection, but must **not** be promoted to
a trustworthy frequency-response or room-quality claim until a controlled
repeat has resolved the cause.

- Source: `PcmRailGate.java` + `RelativeCalibrationEngine.java`.
- Display: `MainActivity.java` shows the typed count and quality status.
- Custody: `AssuranceZiprafWriter.java` records both fields only when the
  calibration result exists; missing capture remains `NOT_RUN`.
- Host test: `ci/java/PcmRailGateTest.java` covers null/empty/out-of-bounds,
  both PCM16 rails, bounded prefix, and typed zero/unknown status.
- Existing CFR bytes and historical receipts are **not modified**. This
  metadata is produced only by subsequent execution of the hotfixed APK.
- Exact-head APK CI, Android 10/16 E2E and independent device reproduction:
  `TOKEN_VAZIO_NOT_RUN` until evidence is recorded for the changed commit.
- Rollback: revert the isolated branch delta; retain historical CFRs and
  append a correction receipt instead of deleting or mutating evidence.

## Claim boundary

```text
RELATIVE_SWEEP_PROFILE = IMPLEMENTED
RELATIVE_DECAY_ESTIMATOR = IMPLEMENTED
ABSOLUTE_SPL = PENDING_PHYSICAL_REFERENCE
CALIBRATED_FFT_STFT = NOT_CLAIMED
IR_DECONVOLUTION = NOT_CLAIMED
ISO3382_CONFORMANCE = NOT_CLAIMED
AUTOMATIC_ROOM_CORRECTION = PENDING
```

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`.
