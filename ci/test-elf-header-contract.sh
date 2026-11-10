#!/usr/bin/env bash
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
set -euo pipefail
cd "$(dirname "$0")/.."
source ci/elf-header-contract.sh
cc="${RFA_HEADER_TEST_CC:-clang}"
reader="${RFA_HEADER_TEST_READELF:-readelf}"
command -v "$cc" >/dev/null || { echo 'ELF_TEST_CLANG=TOKEN_VAZIO'; exit 1; }
command -v "$reader" >/dev/null || { echo 'ELF_TEST_READELF=TOKEN_VAZIO'; exit 1; }
reader="$(command -v "$reader")"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
printf '%s\n' 'unsigned rfa_probe(unsigned x) { return x ^ 0x5a5aU; }' > "$tmp/probe.c"
"$cc" --target=armv7a-none-eabi -std=c11 -ffreestanding -nostdinc -fno-builtin -c "$tmp/probe.c" -o "$tmp/arm32.o"
"$cc" --target=aarch64-none-elf -std=c11 -ffreestanding -nostdinc -fno-builtin -c "$tmp/probe.c" -o "$tmp/arm64.o"
"$cc" -std=c11 -ffreestanding -nostdinc -fno-builtin -c "$tmp/probe.c" -o "$tmp/host.o"
rfa_assert_elf_header "$reader" "$tmp/arm32.o" armv7a-none-eabi
rfa_assert_elf_header "$reader" "$tmp/arm64.o" aarch64-none-elf
reject() { if rfa_assert_elf_header "$reader" "$1" "$2" >/dev/null 2>&1; then echo "ELF_FALSIFIER_ACCEPTED=$3" >&2; exit 1; fi; }
reject "$tmp/arm32.o" aarch64-none-elf WRONG_TARGET_32_AS_64
reject "$tmp/arm64.o" armv7a-none-eabi WRONG_TARGET_64_AS_32
reject "$tmp/host.o" aarch64-none-elf HOST_NOT_ARM64
reject "$tmp/arm32.o" unrecognized-abi UNKNOWN_TARGET
: > "$tmp/empty.o"
reject "$tmp/empty.o" armv7a-none-eabi EMPTY
cp "$tmp/arm32.o" "$tmp/endian-corrupted.o"
printf '\002' | dd of="$tmp/endian-corrupted.o" bs=1 seek=5 conv=notrunc status=none
reject "$tmp/endian-corrupted.o" armv7a-none-eabi ENDIAN_CORRUPTION
printf 'ELF_ABI_HEADER_FALSIFIERS=6_REJECTED\n'
printf 'ELF_ABI_HEADER_VALID=ARMV7,AARCH64\n'
printf 'ELF_HEADER_TEST_SCOPE=HOSTED_OBJECT_INSPECTION_ONLY\n'
