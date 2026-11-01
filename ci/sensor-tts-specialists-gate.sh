#!/usr/bin/env bash
# Copyright (c) 2026 Rafael Melo Reis.
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
# Specialist host proof only; device HAL, voice intelligibility and GPU not tested.
set -euo pipefail
cd "$(dirname "$0")/.."
cc="${CC:-clang}"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
flags=(-std=c11 -ffreestanding -nostdinc -fno-builtin -fno-common
  -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables
  -Wall -Wextra -Wshadow -Werror -Iapp/src/main/cpp)
sources=(app/src/main/cpp/rfa_sensor_delta_core.c
  app/src/main/cpp/rfa_tts_pcm_guard_core.c
  app/src/main/cpp/rfa_dual_watchdog_core.c)
for opt in -O0 -O3; do
  "$cc" "${flags[@]}" "$opt" native/tests/sensor_tts_guard_smoke.c "${sources[@]}" -o "$tmp/specialists"
  "$tmp/specialists"
done
for source in "${sources[@]}"; do
  obj="$tmp/$(basename "$source").o"
  "$cc" "${flags[@]}" -O3 -c "$source" -o "$obj"
  if nm -u "$obj" | grep -q .; then
    nm -u "$obj" >&2
    echo 'SENSOR_TTS_UNDEFINED_SYMBOLS=FAIL' >&2
    exit 1
  fi
  if nm -a "$obj" | awk 'NF >= 3 && $2 ~ /^[bBcCdDgGsS]$/ { print }' | grep -q .; then
    echo 'SENSOR_TTS_WRITABLE_GLOBALS=FAIL' >&2
    exit 1
  fi
done
echo 'SENSOR_TTS_SPECIALISTS=PASS_HOST_O0_O3_ZERO_UNDEFINED_ZERO_MUTABLE_GLOBALS'
echo 'SENSOR_TTS_ANDROID_DEVICE=TOKEN_VAZIO_NOT_RUN'
echo 'SENSOR_TTS_NEON_GPU_NUMA=TOKEN_VAZIO_NOT_BENCHMARKED'
echo 'SENSOR_TTS_RELEASE_ALLOWED=false'
