# Physical playback observer — V0

status: SOURCE_IMPLEMENTED_LOCAL_TESTED; remote CI and physical replay still TOKEN_VAZIO.
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

## Boundary

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.

- Source: freestanding C core + smoke test.
- Artifact: future APK/native build output.
- Execution: actual Android device recording a played external TTS source.
- Evidence: hash-bound PCM/ZIPRAF receipt plus observer report.
- Claim: only after physical replay and independent review.

## Physical gate required next

1. Paste the same text into the app script field.
2. Play the external TTS source from GPT, DeepSeek, Claude or another explicit provider.
3. Record through EstudioAudio using the physical device path.
4. Store raw PCM/ZIPRAF with SHA-256.
5. Run observer and write receipt with source text hash, audio hash, app APK hash and device label.

Until that exists: NATURAL_TTS, HUMAN_LISTENING, PROVIDER_IDENTITY, SPEAKER_IDENTITY, SEX_OF_READER and PHYSICAL_REPLAY remain TOKEN_VAZIO.
