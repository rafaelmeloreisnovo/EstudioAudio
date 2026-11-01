/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Specialist W01: two independent freshness deadlines; monotonic caller clock.
 */
#ifndef RFA_DUAL_WATCHDOG_CORE_H
#define RFA_DUAL_WATCHDOG_CORE_H
#include "rfa_core_types.h"
#ifdef __cplusplus
extern "C" {
#endif
typedef struct {
    rfa_u64 audio_ms;
    rfa_u64 sensor_ms;
    rfa_u32 audio_seen;
    rfa_u32 sensor_seen;
} rfa_dual_watchdog;
/* reset is deterministic. 1=audio, 2=sensor; timestamp 0 is valid. */
void rfa_dual_watchdog_reset(rfa_dual_watchdog *state);
int rfa_dual_watchdog_mark(rfa_dual_watchdog *state, rfa_u32 lane,
                           rfa_u64 monotonic_ms);
/* Bits: audio=1, sensor=2, eligible=4.
 * Missing/stale/clock regression => corresponding bit false.
 * require_sensor=0 allows voice-only operation; 1 requires both.
 * This is NOT a hardware or independent process watchdog. */
rfa_u32 rfa_dual_watchdog_check(const rfa_dual_watchdog *state,
                                 rfa_u64 now_ms, rfa_u32 max_age_audio_ms,
                                 rfa_u32 max_age_sensor_ms,
                                 rfa_u32 require_sensor);
#ifdef __cplusplus
}
#endif
#endif
