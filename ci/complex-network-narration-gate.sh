#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Explicit V2 source freeze: prevent a silent reversion to an eight-sentence
# summary. A deliberate V3 revision must update provenance and review this gate.
# Git blob identity, NOT a digital signature or provider branch protection.
locked_path="app/src/main/java/io/rafaelia/audiostudio/ComplexNetworkNarrationPreset.java"
locked_blob="0f48f96e5d6512282fa859c122bbf96887aadb67"
actual_blob="$(git hash-object -- "$locked_path")"
if test "$actual_blob" != "$locked_blob"; then
  printf '[NARRATION_V2_LOCK][FAIL] source blob changed: expected=%s actual=%s\n' "$locked_blob" "$actual_blob" >&2
  exit 1
fi
echo "AUDIO_FIRST_SCRIPT_V2_GIT_BLOB_LOCK=PASS_SOURCE_SCOPED"
python3 ci/complex-network-narration-lock-negative.py
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
command -v javac >/dev/null || { echo "JAVAC=TOKEN_VAZIO" >&2; exit 1; }
command -v java >/dev/null || { echo "JAVA=TOKEN_VAZIO" >&2; exit 1; }
javac -encoding UTF-8 -d "$tmp" \
  app/src/main/java/io/rafaelia/audiostudio/ComplexNetworkNarrationPreset.java \
  native/tests/ComplexNetworkNarrationPresetSmoke.java
java -cp "$tmp" io.rafaelia.audiostudio.ComplexNetworkNarrationPresetSmoke
grep -Fq 'ComplexNetworkNarrationPreset.ptBrScript()' \
  app/src/main/java/io/rafaelia/audiostudio/MainActivity.java
echo "NETWORK_NARRATION_ROUTING=PASS_SOURCE_SCOPED"
