#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
cc="${CC:-clang}"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
flags=(-std=c11 -O2 -nostdinc -ffreestanding -fno-builtin -fno-common
 -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables
 -fno-optimize-sibling-calls -Wall -Wextra -Wshadow -Werror -Iapp/src/main/cpp)
"$cc" "${flags[@]}" native/tests/timbre_core_smoke.c app/src/main/cpp/rfa_timbre_core.c -o "$tmp/test"
"$tmp/test"
"$cc" "${flags[@]}" -c app/src/main/cpp/rfa_timbre_core.c -o "$tmp/core.o"
test -z "$(nm -u "$tmp/core.o")"
test -z "$(nm -a "$tmp/core.o" | awk 'NF>=3 && $2 ~ /^[bBcCdDgGsS]$/ { print }')"
echo 'TIMBRE_FREESTANDING_HOST_AND_CHUNK_PARITY=PASS_SCOPED'
echo 'VOICE_IDENTITY_OR_INTELLIGIBILITY=NOT_TESTED'
