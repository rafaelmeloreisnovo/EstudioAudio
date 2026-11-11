/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 *
 * ROOM GUARD V1: deterministic relative-only room-EQ proposal.
 * No microphone I/O, filter synthesis, SPL calibration or automatic playback.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */
#ifndef RFA_ROOM_GUARD_CORE_H
#define RFA_ROOM_GUARD_CORE_H
#include "rfa_core_types.h"
#ifdef __cplusplus
extern "C" {
#endif

enum {
    RFA_ROOM_GUARD_BANDS = 16,
    RFA_ROOM_GUARD_POSITIONS = 8,
    RFA_ROOM_OBS_VALID = 1,
    RFA_ROOM_OBS_SYNC_OK = 2,
    RFA_ROOM_OBS_UNCLIPPED = 4,
    RFA_ROOM_OBS_REQUIRED = 7
};
enum {
    RFA_ROOM_DECISION_CUT = 1,
    RFA_ROOM_DECISION_NO_CUT = 2,
    RFA_ROOM_DECISION_INSUFFICIENT = 3,
    RFA_ROOM_DECISION_UNSTABLE = 4
};

typedef struct {
    rfa_i32 relative_db_q8;
    rfa_u8 flags;
} rfa_room_observation_q8;

typedef struct {
    rfa_room_observation_q8 positions[RFA_ROOM_GUARD_POSITIONS];
    rfa_i32 target_relative_db_q8;
    rfa_u8 count;
} rfa_room_band_q8;

typedef struct {
    rfa_u8 minimum_positions; /* [2,8]; must be predeclared */
    rfa_i32 max_spread_db_q8; /* [0,96dB] */
    rfa_i32 max_cut_db_q8; /* (0,12dB] */
} rfa_room_guard_policy_q8;

typedef struct {
    rfa_i32 gain_db_q8; /* NEVER positive: attenuation only, no IIR/FIR coefficients */
    rfa_i32 median_db_q8;
    rfa_i32 spread_db_q8;
    rfa_u8 used_positions;
    rfa_u8 reason;
} rfa_room_decision_q8;

/* returns number of bands evaluated, or 0 on invalid arguments; on invalid
 * arguments output is untouched. The input dB quantities are RELATIVE only.
 * Caller must separately substantiate VALID/SYNC_OK/UNCLIPPED flags.
 */
int rfa_room_guard_plan_q8(const rfa_room_band_q8 *bands, int band_count,
                           const rfa_room_guard_policy_q8 *policy,
                           rfa_room_decision_q8 *out);
#ifdef __cplusplus
}
#endif
#endif
