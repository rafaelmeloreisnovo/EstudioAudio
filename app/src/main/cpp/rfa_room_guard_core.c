/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Pure freestanding bounded fixed-point planner, NOT applied room correction.
 */
#include "rfa_room_guard_core.h"

static int rfa_room_in_range(rfa_i32 v) {
    return v >= -24576 && v <= 24576; /* +-96dB Q8; no overflow in differences */
}

int rfa_room_guard_plan_q8(const rfa_room_band_q8 *bands, int band_count,
                           const rfa_room_guard_policy_q8 *policy,
                           rfa_room_decision_q8 *out) {
    int band;
    if (bands == (const rfa_room_band_q8 *)0 ||
        policy == (const rfa_room_guard_policy_q8 *)0 ||
        out == (rfa_room_decision_q8 *)0 ||
        band_count <= 0 || band_count > RFA_ROOM_GUARD_BANDS ||
        policy->minimum_positions < 2 ||
        policy->minimum_positions > RFA_ROOM_GUARD_POSITIONS ||
        policy->max_cut_db_q8 <= 0 || policy->max_cut_db_q8 > 3072 ||
        policy->max_spread_db_q8 < 0 ||
        policy->max_spread_db_q8 > 24576) return 0;

    /* Reject malformed values before changing ANY output. */
    for (band = 0; band < band_count; ++band) {
        int i;
        if (bands[band].count > RFA_ROOM_GUARD_POSITIONS ||
            !rfa_room_in_range(bands[band].target_relative_db_q8)) return 0;
        for (i = 0; i < bands[band].count; ++i) {
            if ((bands[band].positions[i].flags & RFA_ROOM_OBS_REQUIRED) ==
                  RFA_ROOM_OBS_REQUIRED &&
                !rfa_room_in_range(bands[band].positions[i].relative_db_q8))
                return 0;
        }
    }

    for (band = 0; band < band_count; ++band) {
        rfa_i32 sorted[RFA_ROOM_GUARD_POSITIONS];
        rfa_i32 median = 0;
        rfa_i32 spread = 0;
        int used = 0;
        int i;
        rfa_room_decision_q8 *d = &out[band];
        d->gain_db_q8 = 0;
        d->median_db_q8 = 0;
        d->spread_db_q8 = 0;
        d->used_positions = 0;
        d->reason = RFA_ROOM_DECISION_INSUFFICIENT;
        for (i = 0; i < bands[band].count; ++i) {
            int j;
            rfa_i32 sample;
            if ((bands[band].positions[i].flags & RFA_ROOM_OBS_REQUIRED) !=
                    RFA_ROOM_OBS_REQUIRED) continue;
            sample = bands[band].positions[i].relative_db_q8;
            j = used;
            while (j > 0 && sorted[j - 1] > sample) {
                sorted[j] = sorted[j - 1];
                --j;
            }
            sorted[j] = sample;
            ++used;
        }
        d->used_positions = (rfa_u8)used;
        if (used < policy->minimum_positions) continue;
        /* Deliberately use lower median for an even observation count. */
        median = sorted[(used - 1) >> 1];
        spread = sorted[used - 1] - sorted[0];
        d->median_db_q8 = median;
        d->spread_db_q8 = spread;
        if (spread > policy->max_spread_db_q8) {
            d->reason = RFA_ROOM_DECISION_UNSTABLE;
            continue;
        }
        if (median <= bands[band].target_relative_db_q8) {
            d->reason = RFA_ROOM_DECISION_NO_CUT;
            continue; /* NO boost into dips/nulls. */
        }
        {
            rfa_i32 excess = median - bands[band].target_relative_db_q8;
            if (excess > policy->max_cut_db_q8) excess = policy->max_cut_db_q8;
            d->gain_db_q8 = -excess;
            d->reason = RFA_ROOM_DECISION_CUT;
        }
    }
    return band_count;
}
