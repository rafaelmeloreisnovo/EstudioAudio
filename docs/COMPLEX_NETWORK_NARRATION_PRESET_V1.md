# Complex-network narration preset V1 — EstudioAudio

Status: SOURCE_PROPOSED; full exact-HEAD CI=NOT_RUN; Android physical=NOT_RUN; human speech intelligibility=TOKEN_VAZIO.
Date: 2026-10-13.
Owner: RAFAELIA / EstudioAudio. License for NEW source: LicenseRef-RAFCODE-Research-Commercial-0.1.
Boundary: SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.

## Why

Add an entirely opt-in, bounded Portuguese explanation of **feedback, temporally changing connections, combined triggers and unknown causal links** to the existing experimental PT-BR phonetic guide. The preset merely fills the editable teleprompter text. A separate, existing Voice guide button still controls speech playback. No microphone permission, network request, voiceprint, model weight or third-party implementation.

## Scholarly provenance, carefully delimited

- Henrique Ferraz de Arruda, Kleber Andrade Oliveira and Yamir Moreno, "Dynamical opinion clusters exploration suite: Modeling social media opinion dynamics", *SoftwareX* 30 (2025) 102136. DOI: https://doi.org/10.1016/j.softx.2025.102136 . DOCES models social opinion interaction, user prioritization and adaptive friendships. **Conceptual citation only**, no DOCES source copied or dependency installed.
- Kleber A. Oliveira, Henrique F. de Arruda and Yamir Moreno, "Mechanistic interplay between information spreading and opinion polarization", arXiv:2410.17151 (posted 2024), later *PNAS Nexus* (published 30 Dec 2025, issue Jan 2026). https://arxiv.org/abs/2410.17151 . Studied reciprocal feedback, limited attention/memory and rewiring in *simulated social networks*. This does not certify a software-federation risk model or TTS naturalness.
- Correct person: Henrique Ferraz de Arruda (past IFSC-USP; ARAID/BIFI, University of Zaragoza since May 2025); **not** Guilherme Ferraz de Arruda, a distinct researcher associated with higher-order interactions.
- The phrase "special form" here means **an authorial pedagogical rhythm/script**, NOT imitation of Henrique's voice or endorsement by him.

## Mechanism and limits

1. New pure Java `ComplexNetworkNarrationPreset.ptBrScript()` returns a fixed, deterministic, authored PT-BR script of at most 2048 UTF-8 bytes.
2. Optional "Rede complexa · roteiro" button explicitly copies the example into the existing editable narration field. It stops an already active guide and never plays sound automatically.
3. The existing opt-in Voice guide renders text via the current freestanding C PT-BR PCM model, Android JNI and AudioTrack boundaries. No changes to the core, ABI manifest, native state or audio permissions.
4. The preset illustrates a feedback loop but **does not** calculate a graph, detect cascades, implement DOCES, infer causation or reveal all unknown unknowns.
5. `ci/complex-network-narration-gate.sh` checks script bounds, character allowlist, determinism, semantic safeguards, and opt-in UI routing. Source gate cannot certify human pronunciation.

## Negative falsifiers and rollback

- Unexpected characters (unsupported by bounded PT-BR) or overlong text fail the hosted text gate.
- No natural speech/acoustics/accessibility claim before independent blind listening plus physical ARM32/ARM64 recordings.
- Existing Stop/Record/Calibrate behavior, app build, source/ABI, speaker concurrency and existing guide must not regress.
- To undo: revert this isolated PR; no stored user script, runtime model or provider settings are changed by the module.
- Do not merge unless CI exact HEAD, review, provenance/rights and provider protection pass. Parent EstudioAudio#61 itself remains P0 HOLD.

R3=<F_ok: isolated source integration designed, F_gap: current-head build/device/reviewer/provider, F_next: exact-head CI and adversarial device tests>.
