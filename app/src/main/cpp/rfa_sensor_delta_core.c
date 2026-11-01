/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * S01: 3-axis arithmetic only. No libc, heap, globals, syscalls or I/O.
 * Source data is from the caller, NOT from physical registers.
 */
#include "rfa_sensor_delta_core.h"
int rfa_sensor_delta3_energy(const rfa_i16 *before,
                             const rfa_i16 *after, rfa_u32 *energy) {
    rfa_i32 x, y, z;
    rfa_u64 square;
    rfa_u32 scaled, overflow_mask;
    if (!before || !after || !energy) return 0;
    x = (rfa_i32)after[0] - (rfa_i32)before[0];
    y = (rfa_i32)after[1] - (rfa_i32)before[1];
    z = (rfa_i32)after[2] - (rfa_i32)before[2];
    square = (rfa_u64)((rfa_i64)x * x) +
             (rfa_u64)((rfa_i64)y * y) +
             (rfa_u64)((rfa_i64)z * z);
    /* 3 * 65535^2 < 2^34; high bits after >>16 fit in u32. */
    scaled = (rfa_u32)(square >> 16);
    overflow_mask = 0U - (rfa_u32)(scaled > 65535U);
    *energy = (scaled & ~overflow_mask) | (65535U & overflow_mask);
    return 1;
}
