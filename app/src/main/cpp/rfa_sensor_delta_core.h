/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Specialist S01: integer sensor-delta energy only. No Android access.
 */
#ifndef RFA_SENSOR_DELTA_CORE_H
#define RFA_SENSOR_DELTA_CORE_H
#include "rfa_core_types.h"
#ifdef __cplusplus
extern "C" {
#endif
/* Caller supplies two 3-axis signed Q15 samples.
 * Output Q16-scaled squared delta, saturated to 65535.
 * Null pointers reject without changing output. Units are NOT m/s^2. */
int rfa_sensor_delta3_energy(const rfa_i16 *before,
                             const rfa_i16 *after, rfa_u32 *energy);
#ifdef __cplusplus
}
#endif
#endif
