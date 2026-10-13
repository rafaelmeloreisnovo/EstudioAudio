#!/usr/bin/env bash
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
"${CC:-clang}" -std=c11 -O1 -g -ffreestanding -fno-builtin -nostdinc -fno-optimize-sibling-calls \
 -Wall -Wextra -Wshadow -Werror -fsanitize=address,undefined -Iapp/src/main/cpp \
 native/tests/core_adaptive_smoke.c app/src/main/cpp/{rfa_digest_core,rfa_zip32_core,rfa_lms_core}.c -o "$tmp/smoke"
"$tmp/smoke"
echo 'CORE_DIGEST_ZIP_ADAPTIVE_ASAN_UBSAN=PASS'
