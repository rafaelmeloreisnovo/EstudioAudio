# Roteiro para ouvir — redes complexas e retroalimentação

Data: 2026-10-13.
Status: SOURCE_PROPOSED; full APK/device auditory intelligibility NOT_RUN.
Source: app/src/main/java/io/rafaelia/audiostudio/ComplexNetworkNarrationPreset.java.
License: LicenseRef-RAFCODE-Research-Commercial-0.1.

## Intent
The prior preset was only an eight-sentence summary. The requested accessibility-oriented successor is a chronological **spoken explanation**, not academic jargon or a visually dense list. The source uses short utterances and period-delimited pauses for an auditory learner who prefers spoken context.

## Pedagogical structure
- Physical chain-reaction demonstration to introduce a new path caused by interaction.
- Real mapping to apps, people, agents and mutable network links.
- Information feedback loop, cause/event order and cross-component combinations.
- Correlation-versus-causation warning and explicit missing-knowledge boundary: TOKEN_VAZIO != ZERO.
- Concrete RAFAELIA promotion review: source, evidence, other consumers, provider permissions, delayed impacts and rollback.

## Exact constraints
- Java source is a **constant UTF-8 monologue of ~1.6 kB**, smaller than the existing 2048-byte core input limit.
- Existing `native/tests/ComplexNetworkNarrationPresetSmoke.java` now falsifies silent truncation, removal of causality/unknown safeguards or unsupported characters.
- **No change to freestanding C/PT-BR phonetic model, JNI, APK permissions, Android AudioTrack or recorded samples.**
- The original English-language app button remains `Rede complexa · roteiro` and fills an editable field only if pressed; playback requires the existing Voice guide action.
- This text is original educational prose; Arruda/DOCES scholarship is background reference, not implementation of a network simulation, causal discovery, or voice model.
- Its prose is written for listening, but the current **phonetic guide is not validated as clear PT-BR speech**, particularly for actual auditory accessibility. That requires listening by an independent user on real ARM32/ARM64 hardware, and shall not be inferred from a successful script check.

## Falsifiers
1. Exact HEAD CI fast script, byte limits and speaker-bound tests must pass.
2. A very short or overlong script, unsupported character, dropped unknown/cause segment must fail.
3. Full Android APK build and actual audible recording/listening must be separately validated.
4. Voice-guide session interruption, stop, recording and user-controlled opt-in must not regress.
5. Require independent review and main branch provider ruleset enforcement before merge. No auto-promotion.

R3=<F_ok: improved source narrative, F_gap: exact-head host run, actual listening and independent review, F_next: CI then auditory device tests>.
