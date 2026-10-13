#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
cc="${CC:-clang}"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
"$cc" -std=c11 -O2 -ffreestanding -fno-builtin -nostdinc -fno-common \
  -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables \
  -fno-optimize-sibling-calls -Wall -Wextra -Wshadow -Werror \
  -Iapp/src/main/cpp native/tests/tele_tts_smoke.c \
  app/src/main/cpp/rfa_tele_tts_core.c -o "$tmp/tele-tts-smoke"
"$tmp/tele-tts-smoke"
echo 'TELE_TTS_HOST_SMOKE=PASS_SYNTHETIC_GUIDE_ONLY'
echo 'PHYSICAL_SPEECH_INTELLIGIBILITY=NOT_RUN'
