#!/usr/bin/env bash
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
set -euo pipefail
cd "$(dirname "$0")/.."
cc="${CC:-clang}"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
flags=(-std=c11 -O2 -nostdinc -ffreestanding -fno-builtin -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables -Wall -Wextra -Wshadow -Werror -Iapp/src/main/cpp)
"$cc" "${flags[@]}" -c app/src/main/cpp/rfa_bit_stack_core.c -o "$tmp/core.o"
test -z "$(nm -u "$tmp/core.o")"
"$cc" "${flags[@]}" native/tests/bit_stack_smoke.c app/src/main/cpp/rfa_bit_stack_core.c -o "$tmp/smoke"
"$tmp/smoke"
"$cc" "${flags[@]}" -fsanitize=address,undefined native/tests/bit_stack_smoke.c app/src/main/cpp/rfa_bit_stack_core.c -o "$tmp/sanitized"
"$tmp/sanitized"
echo BIT_STACK_CORE=PASS_HOST_AND_SANITIZERS
