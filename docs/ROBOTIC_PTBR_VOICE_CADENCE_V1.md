# EstudioAudio — Voz robótica articulada, sem imitar humanos (V1)

Date: 2026-10-13 | status SOURCE_CANDIDATE / Android speaker NOT_RUN.
Copyright (c) 2026 Rafael Melo Reis.
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

## Intent

Acceptance criterion from user: the synthetic voice should speak **adequately as a robot**, not copy a natural human. Articulation, sequence, consistency, audibly distinct words and oral comprehension are more important than realistic timbre or speaker imitation. Distinguish **articulatory intelligibility** (human listening test not run) from **deterministic acoustic output** (host tests only).

## Implemented V1 — additive, A/B and revertible

- Existing freestanding `rfa_ptbr_render` is **bitwise unchanged**, preserving the previous `Voice guide · experimental` playback for A/B comparison. Grapheme-to-phone engine still has a deliberately limited phoneme inventory. Do not claim fluent human-grade PT-BR.
- New `rfa_ptbr_robot_render` + `rfa_ptbr_robot_reset`, both in the same `rfa_ptbr_core.c/.h`, use 9 caller-owned 32-bit state words. Pure integer core, 16,000 Hz mono PCM16, no libc, libm, heap, file I/O, network, external voice dataset, third-party TTS, mutable file static, or Java in core. The module remains standalone in ARMv7/AArch64 symbol/ABI tests.
- **Phrase rhythm:** comma adds 960 silent frames (~60ms), semicolon/colon adds 1,600 (~100ms), sentence-final period/question/exclamation adds 2,400 (~150ms), in addition to existing grapheme timing. These are robotic *deliberate pauses* rather than realistic breath imitation.
- **Articulatory continuity:** one-pole integer smoothing `smooth = (smooth*3 + x)/4` avoids some discontinuities between pulse/noise components; no pitch/formant-learning claim. Output is tested for no clipping and arbitrary block boundary equality.
- `NativeDsp.nativeRobotPtbrRender` is an independent JNI method with 9-word serialized state; original `nativePtbrRender` still accepts exactly 6 words. Caller-owned state is read, validated and written through JNI. AudioTrack remains Android platform boundary. The pre-existing opt-in `TimbreProfileStore` filter is applied after either renderer.
- New narration UI button `Voz robótica · cadência experimental`, explicitly starts only after click, rejects active microphone/DSP sessions, and can be stopped. Old voice-guide button remains independent. The `Rede complexa · roteiro` approved V2 content blob is untouched.
- Android permissions, storage, voice recording, cloud provider, APK publishing and dependency manifests unchanged.

## Falsification

- `native/tests/ptbr_robot_cadence_smoke.c` validates total PCM frame budget against `rfa_ptbr_next` event durations + exact punctuation budgets, 1024 vs 37 sample block equivalence, bounded samples, no silent output, complete clean state after EOF, invalid UTF-8 and illegal serialized state must fail without modifying PCM.
- `ci/ptbr-core-gate.sh` runs both original PTBR test and robotic A/B test, then compiles with `-ffreestanding -nostdinc -fno-builtin -Wshadow -Werror` and asserts no unresolved symbols or mutable globals. All exact-head platform ABI checks must also pass before APK claims.
- `ci/robotic-ptbr-routing-gate.py` checks opt-in UI, Java JNI routing, unchanged legacy JNI symbol, no auto playback on loading preset, old 6-word state and new 9-word state, and absence of new permissions.

## P0 evidence / risk

**NOT_RUN**: full APK build at this PR HEAD, ARM hardware test, AudioTrack verified speaker output, listening-based Portuguese intelligibility on the full approved explanation, continuity under pause/stop/interruption, hearing/accessibility evaluation or lower-end device profiling. A successful host gate cannot prove audio is understandable. Acoustic smoothing can remove high-frequency consonant detail; require adversarial listening, comparing robot and baseline, before claiming quality or promoting.

Repository main observed unprotected at start; owner-side issue [#66](https://github.com/rafaelmeloreisnovo/EstudioAudio/issues/66) tracks actual GitHub merge barrier. A source-code gate alone does not prevent an owner merge. No automatic merge, publishing or release on this draft.

R3=<F_ok: candidate low-level robotic cadence + reproducible source falsifiers, F_gap: provider/review/physical, F_next: exact-head CI, independent acoustic review, enforced server rules>.

Rollback: revert only this PR and retain the original PT-BR guide implementation and V2 educational script identity.
