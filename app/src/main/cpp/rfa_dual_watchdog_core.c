/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * W01: pure caller-owned freshness test; no clocks, threads or timers inside.
 */
#include "rfa_dual_watchdog_core.h"
void rfa_dual_watchdog_reset(rfa_dual_watchdog *state) {
    if (!state) return;
    state->audio_ms = 0U;
    state->sensor_ms = 0U;
    state->audio_seen = 0U;
    state->sensor_seen = 0U;
}
int rfa_dual_watchdog_mark(rfa_dual_watchdog *state, rfa_u32 lane,
                           rfa_u64 monotonic_ms) {
    if (!state || (lane != 1U && lane != 2U)) return 0;
    if (lane == 1U) {
        if (state->audio_seen && monotonic_ms < state->audio_ms) return 0;
        state->audio_ms = monotonic_ms;
        state->audio_seen = 1U;
    } else {
        if (state->sensor_seen && monotonic_ms < state->sensor_ms) return 0;
        state->sensor_ms = monotonic_ms;
        state->sensor_seen = 1U;
    }
    return 1;
}
rfa_u32 rfa_dual_watchdog_check(const rfa_dual_watchdog *state,
                                 rfa_u64 now_ms, rfa_u32 max_age_audio_ms,
                                 rfa_u32 max_age_sensor_ms,
                                 rfa_u32 require_sensor) {
    rfa_u32 a, b, ready;
    if (!state || require_sensor > 1U ||
        max_age_audio_ms == 0U || max_age_sensor_ms == 0U)
        return 0U;
    /* Short-circuit guards prevent u64 unsigned underflow on clock rollback. */
    a = (rfa_u32)(state->audio_seen != 0U && now_ms >= state->audio_ms &&
                  now_ms - state->audio_ms <= max_age_audio_ms);
    b = (rfa_u32)(state->sensor_seen != 0U && now_ms >= state->sensor_ms &&
                  now_ms - state->sensor_ms <= max_age_sensor_ms);
    ready = a & (b | (require_sensor ^ 1U));
    return a | (b << 1U) | (ready << 2U);
}
