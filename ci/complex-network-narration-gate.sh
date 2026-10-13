#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
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
