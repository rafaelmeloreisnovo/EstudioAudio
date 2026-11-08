#!/usr/bin/env bash
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
# Cross-object source gate only: no device, APK, performance or acoustic claim.
set -euo pipefail
cd "$(dirname "$0")/.."
for x in clang readelf awk python3; do
  command -v "$x" >/dev/null || { echo "MISSING_TOOL=$x" >&2; exit 1; }
done
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
mapfile -t manifest < <(grep -v '^[[:space:]]*#' native/abi/asm_public_symbols_v1.txt | sed '/^[[:space:]]*$/d' | sort)
test "${#manifest[@]}" -eq 2
test "${manifest[0]}" = rfa_pcm_abs_u16
test "${manifest[1]}" = rfa_pcm_sat_add_s16
for abi in armv7 aarch64; do
  if test "$abi" = armv7; then
    target=armv7a-linux-androideabi29
    machine='Machine: *ARM$'
    flags=(-marm)
  else
    target=aarch64-linux-android29
    machine='Machine: *AArch64$'
    flags=()
  fi
  for op in "${manifest[@]}"; do
    file="app/src/main/cpp/arch/$abi/$op.S"
    out="$tmp/${abi}_${op}.o"
    clang "--target=$target" "${flags[@]}" -ffreestanding -fno-builtin -nostdinc \
        -fno-stack-protector -c "$file" -o "$out"
    readelf -h "$out" | grep -Eq "$machine"
    actual="$(readelf -Ws "$out" | awk '$5=="GLOBAL" && $7!="UND" { print $8 }')"
    test "$actual" = "$op" || { echo "ABI_SYMBOL_DRIFT:$file:$actual" >&2; exit 1; }
    readelf -r "$out" | grep -q 'There are no relocations'
    test "$(readelf -Ws "$out" | awk '$7=="UND" && $8!="" {print $8}' | wc -l)" -eq 0
    readelf -Ws "$out" | grep -q 'GLOBAL HIDDEN'
    printf 'ASM_PCM_OBJECT=%s ABI=%s ELF=PASS ZERO_UNDEFINED=PASS RELOC=ZERO\n' "$op" "$abi"
  done
done
python3 - <<'PY'
# Math oracle only; does not execute the ARM objects.
from itertools import product
v=(-32768,-32767,-16384,-1,0,1,16384,32766,32767)
for a,b in product(v,repeat=2):
    r=max(-32768,min(32767,a+b))
    assert -32768<=r<=32767
for a in range(-32768,32768):
    r=abs(a)
    assert 0<=r<=32768
print('ASM_PCM_REFERENCE_ORACLE=PASS_SCOPED_NOT_ARM_RUNTIME')
PY
printf 'ASM_PCM_PHYSICAL_DEVICE=NOT_RUN\nASM_PCM_APK_INTEGRATION=NOT_RUN\nASM_PCM_SPEEDUP=TOKEN_VAZIO\n'
