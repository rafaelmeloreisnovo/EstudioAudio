#!/usr/bin/env bash
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
set -euo pipefail
cd "$(dirname "$0")/.."
ndk_tools="${ANDROID_SDK_ROOT:-}/ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/bin"
cc="${RFA_PORTABILITY_CC:-$ndk_tools/clang}"
nm_bin="${RFA_PORTABILITY_NM:-$ndk_tools/llvm-nm}"
command -v "$cc" >/dev/null || { echo "PORTABILITY_CLANG_MISSING=$cc" >&2; exit 1; }
command -v "$nm_bin" >/dev/null || { echo "PORTABILITY_NM_MISSING=$nm_bin" >&2; exit 1; }
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
flags=(-std=c11 -O3 -nostdinc -ffreestanding -fno-builtin -fno-common -fno-optimize-sibling-calls -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables -Wall -Wextra -Wshadow -Werror -Iapp/src/main/cpp)
# Object portability only, not Android packaging or physical execution on these hosts.
for target in x86_64-unknown-linux-gnu riscv64-unknown-elf; do
  for src in app/src/main/cpp/*_core.c; do
    obj="$tmp/$(basename "$src").o"
    "$cc" --target="$target" "${flags[@]}" -c "$src" -o "$obj"
    test -z "$("$nm_bin" -u "$obj")" || { "$nm_bin" -u "$obj"; exit 1; }
    writable="$("$nm_bin" -a "$obj" | awk 'NF>=3 && $2 ~ /^[bBcCdDgGsS]$/ {print}')"
    test -z "$writable" || { echo "PORTABILITY_WRITABLE_GLOBAL[$target,$src]=$writable" >&2; exit 1; }
  done
  echo "CORE_OBJECT_PORTABILITY[$target]=PASS_ZERO_UNDEFINED_NO_WRITABLE_GLOBALS"
done
echo 'FOREIGN_TARGET_RUNTIME=NOT_RUN'
echo 'FOREIGN_PLATFORM_APK=NOT_SUPPORTED_BY_THIS_OBJECT_GATE'
