#!/usr/bin/env bash
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
# Hosted verifier, not part of the freestanding audio runtime.

rfa_assert_elf_header() {
  local reader="$1" object="$2" target="$3"
  local expected_class expected_machine header
  case "$target" in
    armv7a-linux-androideabi29|armv7a-none-eabi)
      expected_class=ELF32
      expected_machine=ARM
      ;;
    aarch64-linux-android29|aarch64-none-elf)
      expected_class=ELF64
      expected_machine=AArch64
      ;;
    *) printf 'ELF_HEADER_REJECT=UNKNOWN_TARGET:%s\n' "$target" >&2; return 1 ;;
  esac
  if ! test -s "$object" || ! test -x "$reader"; then
    printf 'ELF_HEADER_REJECT=OBJECT_OR_READER_MISSING:%s\n' "$target" >&2
    return 1
  fi
  header="$("$reader" -h "$object" 2>/dev/null)" || {
    printf 'ELF_HEADER_REJECT=INVALID_FILE:%s\n' "$target" >&2
    return 1
  }
  if ! printf '%s\n' "$header" | grep -Eq "^[[:space:]]*Class:[[:space:]]*$expected_class([[:space:]]|$)"; then
    printf 'ELF_HEADER_REJECT=CLASS:%s\n' "$target" >&2
    return 1
  fi
  if ! printf '%s\n' "$header" | grep -Eq '^[[:space:]]*Data:[[:space:]]*.*little endian'; then
    printf 'ELF_HEADER_REJECT=ENDIAN:%s\n' "$target" >&2
    return 1
  fi
  if ! printf '%s\n' "$header" | grep -Eq '^[[:space:]]*Type:[[:space:]]*REL[[:space:]]'; then
    printf 'ELF_HEADER_REJECT=NOT_RELOCATABLE:%s\n' "$target" >&2
    return 1
  fi
  if ! printf '%s\n' "$header" | grep -Eq "^[[:space:]]*Machine:[[:space:]]*$expected_machine([[:space:]]|$)"; then
    printf 'ELF_HEADER_REJECT=MACHINE:%s\n' "$target" >&2
    return 1
  fi
}
