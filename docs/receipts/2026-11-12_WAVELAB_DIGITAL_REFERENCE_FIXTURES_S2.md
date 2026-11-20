# μRECEIPT successor S2 — digital signal references created (NOT calibrated)

Date: 2026-11-12
Producer: local original synthetic fixture creation; reference-only metadata in EstudioAudio branch feature/wavelab-trial-matrix-20261112.
Parent: docs/receipts/2026-11-12_WAVELAB_UNIFIED_TRIAL_MATRIX_S1.md
Kind: REFERENCE_DIGITAL_GENERATION_ONLY
Physical pressure calibration: TOKEN_VAZIO_NOT_MEASURED
Device playback: NOT_RUN
Independent microphone captures: NOT_RUN
claim_allowed: false for physical/acoustic quality.

A ZIP with original generated WAV signals, source generator, manifest JSON, README and blank experimental worksheet was created in the conversation working container. It is **not committed to GitHub** and must not be inferred to have been installed in the APK.

ZIP SHA-256: cf8962aef6ff49c983ee830635e64580151afa9c9775bcec414d1a2c9de1ce81. ZIP read/CRC passed; same SHA-256 on two local repeated generator executions under the same environment.

All 3 WAV are PCM16 mono 48kHz with digital peak approximately -24 dBFS, and each is a **digital reference**, not a specified sound pressure level.

1. 01_RFA_PINK_PERIODIC_48K_4096x120.wav, length 10.24 s, 4096-sample period x120, nominal band 100..12000Hz, SHA-256 d2a7f569be26004cece109b933ccba953bf59844dffd4b6eaf65040bd86d503e.
2. 02_RFA_LOG_SWEEP_100_12000HZ_10S.wav, length 10 s, SHA-256 f1b42dc1527eac7d4ff0e19010ebf0d1a4a99f5dc0927182ae7422a8b0c42139.
3. 03_RFA_SIX_SINE_BANDS_48K.wav, length 7.5 s, frequencies 125/250/500/1000/2000/4000Hz, SHA-256 23e3f2012315d8929c9750ed8562d4b18000645624f3e7c382e86100f5cf135e.

Local source generator: create_acoustic_reference_v1.py (NumPy; not freestanding). No external copyrighted music was incorporated. No user recording was added.

**Safety:** start with device playback volume very low; -24 dBFS digital does not bound SPL, amplification, driver power or exposure. Never assert hearing safety based only on file amplitude. A standardized source is not a calibrated microphone/room.

**Experimental gate:** save original WAV reference, preserve exact reference SHA; capture three NEW raw PCM recordings with the same mic/speaker/gain/position/stimulus, SHA each PCM, align timestamps, reject clipping; analyze relative spectra without confusing Goertzel tone probes with calibrated RTA/PSD. Different volumes and speakers form different experimental groups.

**F_ok:** files were generated and ZIP digest verified twice in a single local environment, GitHub reference-only receipt written for provenance.
**F_gap:** Android playback, device SPL, acoustics, independent takes, exact app integration, Java host gate remain NOT_RUN.
**F_next:** execute branch test gate; user-controlled playback + capture in a single continuous session and evidence of real microphone timestamps. No PR/merge/publication.
