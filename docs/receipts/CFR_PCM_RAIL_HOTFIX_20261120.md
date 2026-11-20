# μRECEIPT — PCM16 rail observation / EstudioAudio — 2026-11-20

- Kind: `BRANCH_ONLY_HOTFIX / IMPLEMENTED_TESTED_LOCAL_SCOPED`.
- Production main: **unchanged by this change**. Branch: `hotfix/cfr-pcm-rail-quality-20261120`.
- Parent/source commit: `58d45fe2c8b17575ea13fafa8cf4bde54b691851`.
- Baseline device evidence: user-provided `archive_audio.zip` (not committed).
- Change: bounded, allocation-free PCM16 digital-rail sample counter; typed missing
  input; non-promoting quality label in console and ZIPRAF; host-only regression vectors.
- Java source git blob verified: `b6d0dd1c8ebe883d04d6ed62e4eff82beff6bd74`.
- Local execution: `javac -Xlint:all` compiled the standalone gate and host test,
  followed by `java ...PcmRailGateTest` => `PASS_SCOPED` on 2026-11-20.
- Local real CFR replay using **same source blob** and a disposable host harness:
  20261119_190242 => **679** PCM16 rail samples;
  20261119_191345 => **655**;
  20261119_191658 => **655**. The latter two CFRs have identical byte SHA-256,
  therefore only **two distinct CFR byte sequences** among three exported files.
- Fidelity interpretation: rail samples are a possible clipping/review indicator,
  **not** proof of physical analogue saturation, lost frames or microphone malfunction.
  Zero observed PCM16 rail samples does not prove an unclipped analogue acquisition.
- Source and documentation readback: seven changed files checked on the branch;
  comparison to base: `ahead_by=7`, `behind_by=0`, `main` not updated.
- CI of exact hotfix HEAD: `TOKEN_VAZIO_NOT_OBSERVED`; the connected GitHub commit
  status returned no statuses and PR-triggered run lookup returned none.
  **Absence of a returned run does not prove no run exists in other providers/events.**
- Android APK build, ARMv7/ARM64 linkage, physical Android 10/16 replay:
  `TOKEN_VAZIO_NOT_RUN`.
- Absolute SPL/True RTA/ISO conformance: `TOKEN_VAZIO_REFERENCE_NOT_PROVIDED`.
- Provenance/privacy: original CFR, audio and device/location exports not committed.
  No credential or raw personal audio copied to GitHub.
- Claim gate: `claim_allowed=false`; tests verify only the new host logic.
- Rollback: abandon/revert this review-only branch; do not delete historical source,
  CFR/ZIPRAF receipts or rewrite any old hash. Correct via a successor receipt.

## R3

- F_ok: branch delta applied; source blob and seven GitHub readbacks observed;
  local host Java vectors and 3 exported CFR fixture checks pass in scope.
- F_gap: Android compilation, CI exact HEAD, sign/release, physical replay,
  clipping cause and metrology require independent evidence.
- F_next: run exact-head CI with JDK + APK gate, inspect provider protection,
  then repeat the physical sweep at safe low volume with controlled gain and
  compare a distinct CFR with predeclared acceptance criteria.
