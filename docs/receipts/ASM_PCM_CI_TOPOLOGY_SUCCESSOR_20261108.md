# μRECEIPT successor — CI topology restored — 2026-11-08

μID: MU-ESTUDIOAUDIO-CI-ONE-WORKFLOW-20261108-S2
parent: MU-ESTUDIOAUDIO-ASM-PCM-20261108-S1
producer: rafaelmeloreisnovo/EstudioAudio PR#78
trigger: exact-head quick run 42442534401 FAILURE
root_cause: `ACTIVE_WORKFLOW_COUNT=2 expected=1` from merged UI workflow on main; unrelated to ASM object gate (4/4 passed).
action: archive second workflow byte-for-byte under `ci/archive/workflows/studio-discover-ui-20261107.yml.disabled` using same original Git blob 1d7911b6a88565c241126f1550ef0d1b15e8aee4. Remove only its active .github/workflows route; retain START.yml as sole executable workflow.
behavior_preservation: Run `ci/studio-discover-ui-gate.py` inside fast and full quality modes. Never weaken topology count, external-action allowlist or source safety check.
EVIDENCE: first CI run reported ARM objects PASS_SCOPED, topology FAIL; successor CI pending.
TOKEN_VAZIO: exact-head successor terminal CI, physical Android, APK and performance.
claim_allowed: false
rollback: reverse archive path only if a reviewed multi-workflow policy supersedes the single-workflow invariant; preserve archive and receipt.
