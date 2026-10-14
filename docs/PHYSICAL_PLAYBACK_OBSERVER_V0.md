# Physical playback observer — V0

status: SOURCE_IMPLEMENTED_HOST_TESTED; successor HEAD remote CI, APK and physical replay require separate readback.
claim_allowed_end_to_end: false
source: app/src/main/cpp/rfa_playback_observer_core.c + .h
parent: docs/TELEPROMPTER_AUTHORIAL_VOICE_GUIDE_V0.md

## Intent

This module supports the physical workflow Rafael described: paste text, play an external TTS reader in another app, record the playback through EstudioAudio, and inspect observable timing/acoustic anomalies.

It does **not** learn a human voice and does **not** identify a person, sex, speaker, provider or model. Those remain TOKEN_VAZIO unless a later physical receipt proves a narrower claim.

## What V0 measures

- text words and punctuation pause marks;
- audio silence runs and maximum silence run;
- high-slope/clip-like glitch candidates;
- zero-crossings and coarse acoustic shift candidates across eight time buckets;
- pause mismatch candidate when text pause marks materially exceed measured silence.

These are heuristic *candidates*, not acoustic diagnoses or a verified transcription. ASCII punctuation and unsigned fixed PCM16 thresholds are the current limited contract. No dB SPL, psychoacoustic intelligibility, speaker identity, or calibrated glitch classifier is implemented.

## Boundary

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.

- Source: freestanding C core + smoke test.
- Artifact: separately measured APK/native build output, exact HEAD only.
- Execution: actual Android device recording a played external TTS source.
- Evidence: hash-bound PCM/ZIPRAF receipt plus observer report.
- Wiring reality: the UI button only inserts a *testing script*. The native `rfa_playback_observe_mono` function is not yet invoked by Android recording/JNI. A physical recorder report is **NOT_IMPLEMENTED**, not merely an unmeasured result.
- Core behavior: bucket energy uses 64-bit accumulation, boundaries are overflow-safe for positive signed-int `count`, and the 20 ms silence floor follows sample rate. These changes have source-host tests, not device proof.
- Claim: only after physical replay and independent review.

## Physical gate required next

1. Paste the same text into the app script field.
2. Play the external TTS source from GPT, DeepSeek, Claude or another explicit provider.
3. Record through EstudioAudio using the physical device path.
4. Store raw PCM/ZIPRAF with SHA-256.
5. Run observer and write receipt with source text hash, audio hash, app APK hash and device label.

Until that exists: NATURAL_TTS, HUMAN_LISTENING, PROVIDER_IDENTITY, SPEAKER_IDENTITY, SEX_OF_READER and PHYSICAL_REPLAY remain TOKEN_VAZIO.

## Source-side regression falsifiers — successor audit

- `native/tests/playback_observer_adversarial.c` reproduces the predecessor 32-bit energy-bucket wrap with a 17.6-million-frame synthetic recording (roughly 5m 56s at 48kHz) and rejects its false negative.
- 96kHz fixture rejects the predecessor fixed 960-frame threshold, which mislabelled 11.46ms silence as a 20ms event.
- Buffer lengths 1..31 reject incorrect nonmultiple-of-eight bucket boundaries.
- `ci/playback-observer-core-gate.sh` runs this host gate alongside the existing fixture and a zero-undefined-symbols object check.
- Baseline source in local host audit: first fixture FAIL(exit 2); independent 96kHz fixture FAIL(exit 4); corrected source PASS for both, Clang no undefined symbols. **Remote exact-HEAD CI, cross-ABI and device execution are separate gates.**

## Thermodynamics / atmospheric observation — typed research boundary

Physical interpretation must distinguish electrical PCM from pressure-wave propagation. Barometric pressure, air temperature and relative humidity may be measured as **context**, but are not inferred from PCM/TTS or silently used to alter pitch, tempo or perceived speaker identity.

A future opt-in, local, sensor-backed receipt should bind:
`temperature_c`, `relative_humidity_pct`, `pressure_pa`, `sensor_source`, `measurement_time`, `calibration_id`, `uncertainty`, `distance_m`, `sample_rate_hz`, `PCM_SHA256`, `APK_SHA256`, `device_id_pseudonym` and any missing fields as `TOKEN_VAZIO`. Keep a distinct receipt for hosted sensor acquisition, model computation, and validation against a calibrated reference.

The approximate **dry-air ideal-gas** relationship `c^2 = gamma * R_specific * T_kelvin` is a thermodynamic reference, **not** an implemented playback correction or evidence of acoustic quality; pressure alone does not change ideal-gas sound speed when temperature and composition are fixed. Humidity alters gas properties; atmospheric absorption depends on frequency, humidity, temperature and pressure. Do not claim ISO 9613-1 compliance without an implemented equation, domain checks, calibration and reference vectors.

Standards/references (read as scientific provenance, not imported algorithms):
- ISO 9613-1:1993, reviewed and confirmed 2026, https://www.iso.org/standard/17426.html
- NIST, *Speed of Sound as a Thermodynamic Property of Fluids*, https://www.nist.gov/publications/speed-sound-thermodynamic-property-fluids

Gates before environmental claims: permission/no-permission negatives; sensor absent/stale/out-of-range; fixed-distance geometry and device volume; calibrated SPL/temperature/RH/pressure reference; cross-device replication; exact SHA/receipts. Until those run: `ENVIRONMENT_SENSOR_DATA=TOKEN_VAZIO`, `PHYSICAL_ACOUSTIC_COUPLING=NOT_VALIDATED`, `claim_allowed_end_to_end=false`.

## Rollback / promotion

Revert only successor commits on PR #64; retain predecessor failing fixture and CI logs as evidence. Do not merge PR #64 while main provider protection, independent review, exact APK+ARM build, on-device capture wiring and physical acoustic falsification remain open. An advisory PR title or draft flag cannot enforce server-side approval.
