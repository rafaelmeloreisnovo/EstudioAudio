/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Acoustical descriptors are NOT voice cloning, identity authentication,
 * intelligibility validation or an intelligible TTS model.
 */
#ifndef RFA_TIMBRE_CORE_H
#define RFA_TIMBRE_CORE_H
#include "rfa_core_types.h"
typedef struct {
    rfa_u64 samples;
    rfa_u64 abs_sum;
    rfa_u64 diff_sum;
    rfa_u64 zero_crossings;
    rfa_i32 last;
    rfa_u32 seen;
} rfa_timbre_stats;
typedef struct {
    rfa_u32 mix_q8;         /* [64,240], amount of unfiltered PCM */
    rfa_u32 mean_abs;       /* observational amplitude only */
    rfa_u32 zero_cross_k;   /* rate per 1000 samples */
} rfa_timbre_profile;
typedef struct { rfa_i32 lowpass; } rfa_timbre_filter;
void rfa_timbre_reset(rfa_timbre_stats *state);
int rfa_timbre_accumulate(rfa_timbre_stats *state,
                          const rfa_i16 *pcm, rfa_u32 frames);
int rfa_timbre_finalize(const rfa_timbre_stats *state,
                         rfa_timbre_profile *out);
int rfa_timbre_apply(rfa_timbre_filter *state,
                      const rfa_timbre_profile *profile,
                      rfa_i16 *pcm, rfa_u32 frames);
#endif
