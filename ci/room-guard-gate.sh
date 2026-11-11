#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"
command -v clang >/dev/null 2>&1 || { echo "ROOM_GUARD_CLANG_MISSING" >&2; exit 1; }
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
flags=(-std=c11 -O2 -ffreestanding -fno-builtin -nostdinc
       -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables
       -fno-common -Wshadow -Wall -Wextra -Werror -Iapp/src/main/cpp)
clang "${flags[@]}" native/tests/room_guard_smoke.c \
    app/src/main/cpp/rfa_room_guard_core.c -o "$tmp/room_guard"
"$tmp/room_guard"
printf 'ROOM_GUARD_HOST_ADVERSARIAL=PASS_SCOPED\n'
printf 'ROOM_GUARD_APPLIED_EQ=NOT_RUN\n'
printf 'ROOM_GUARD_PHYSICAL_CALIBRATION=NOT_RUN\n'
