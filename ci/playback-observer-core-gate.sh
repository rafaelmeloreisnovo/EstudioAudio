#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
cc="${CC:-clang}"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
"$cc" -std=c11 -O2 -ffreestanding -fno-builtin -nostdinc -fno-common \
  -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables \
  -fno-optimize-sibling-calls -Wall -Wextra -Wshadow -Werror \
  -Iapp/src/main/cpp native/tests/playback_observer_smoke.c \
  app/src/main/cpp/rfa_playback_observer_core.c -o "$tmp/playback-observer-smoke"
"$tmp/playback-observer-smoke"
"$cc" -std=c11 -O2 -ffreestanding -fno-builtin -nostdinc -fno-common \
  -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables \
  -fno-optimize-sibling-calls -Wall -Wextra -Wshadow -Werror \
  -Iapp/src/main/cpp native/tests/playback_observer_adversarial.c \
  app/src/main/cpp/rfa_playback_observer_core.c -o "$tmp/playback-observer-adversarial"
"$tmp/playback-observer-adversarial"
echo 'PLAYBACK_OBSERVER_ADVERSARIAL=PASS_LONG_CAPTURE_96KHZ_SMALL_BUFFER'
"$cc" -std=c11 -O2 -ffreestanding -fno-builtin -nostdinc -fno-common \
  -Wall -Wextra -Wshadow -Werror -Iapp/src/main/cpp \
  -c app/src/main/cpp/rfa_playback_observer_core.c -o "$tmp/playback-observer-core.o"
test -z "$(nm -u "$tmp/playback-observer-core.o")"
echo 'PLAYBACK_OBSERVER_HOST_SMOKE=PASS_SYNTHETIC_PAUSE_GLITCH_ONLY'
echo 'PLAYBACK_OBSERVER_PHYSICAL_REPLAY=NOT_RUN'
echo 'SPEAKER_IDENTITY=TOKEN_VAZIO_NOT_CLAIMED'
