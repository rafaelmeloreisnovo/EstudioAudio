/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Freestanding integer-only profile, no libc/heap/I/O/globals.
 */
#include "rfa_timbre_core.h"
void rfa_timbre_reset(rfa_timbre_stats *state) {
    if (!state) return;
    state->samples=0U; state->abs_sum=0U; state->diff_sum=0U;
    state->zero_crossings=0U; state->last=0; state->seen=0U;
}
int rfa_timbre_accumulate(rfa_timbre_stats *state,
                          const rfa_i16 *pcm, rfa_u32 frames) {
    rfa_u32 i;
    if (!state || !pcm || frames==0U || frames>4096U ||
        state->seen>1U || state->samples>1000000U ||
        (rfa_u64)frames>1000000U-state->samples) return 0;
    for (i=0U;i<frames;++i) {
        rfa_i32 sample=(rfa_i32)pcm[i];
        rfa_i32 amplitude=sample<0?-sample:sample;
        state->abs_sum+=(rfa_u32)amplitude;
        if (state->seen) {
            rfa_i32 delta=sample-state->last;
            state->diff_sum+=(rfa_u32)(delta<0?-delta:delta);
            if ((sample<0 && state->last>=0) ||
                (sample>=0 && state->last<0)) ++state->zero_crossings;
        }
        state->last=sample;
        state->seen=1U;
        ++state->samples;
    }
    return 1;
}
/* Shift-subtract division to keep ARMv7 freestanding with zero
 * implicit __aeabi_uldivmod/RT helpers for 64-bit accumulators. */
static rfa_u64 rfa_timbre_div(rfa_u64 numerator, rfa_u64 denominator) {
    rfa_u64 quotient=0U,remainder=0U;
    rfa_u32 i;
    for(i=64U;i>0U;--i) {
        remainder=(remainder<<1U) | ((numerator>>(i-1U)) & 1U);
        quotient<<=1U;
        if(remainder>=denominator) {
            remainder-=denominator;
            quotient|=1U;
        }
    }
    return quotient;
}
int rfa_timbre_finalize(const rfa_timbre_stats *state,
                         rfa_timbre_profile *out) {
    rfa_u64 v;
    if (!state || !out || state->samples<8000U ||
        state->samples>1000000U || !state->seen ||
        state->abs_sum<state->samples*64U ||
        state->zero_crossings>state->samples ||
        state->diff_sum>state->samples*65535U) return 0;
    /* Observational "brightness" proxy; not a learned phoneme/formant model. */
    v=rfa_timbre_div(state->diff_sum*96U,state->abs_sum+1U);
    if (v>176U) v=176U;
    out->mix_q8=64U+(rfa_u32)v;
    out->mean_abs=(rfa_u32)rfa_timbre_div(state->abs_sum,state->samples);
    out->zero_cross_k=(rfa_u32)rfa_timbre_div(state->zero_crossings*1000U,state->samples);
    return 1;
}
int rfa_timbre_apply(rfa_timbre_filter *state,
                      const rfa_timbre_profile *profile,
                      rfa_i16 *pcm, rfa_u32 frames) {
    rfa_u32 i;
    rfa_i32 previous;
    if (!state || !profile || !pcm || frames>4096U ||
        profile->mix_q8<64U || profile->mix_q8>240U) return 0;
    previous=state->lowpass;
    if (previous<-32768 || previous>32767) return 0;
    for (i=0U;i<frames;++i) {
        rfa_i32 x=pcm[i];
        rfa_i32 mixed;
        previous=(previous*3+x)/4;
        mixed=(x*(rfa_i32)profile->mix_q8+
               previous*(rfa_i32)(256U-profile->mix_q8))/256;
        pcm[i]=(rfa_i16)mixed;
    }
    state->lowpass=previous;
    return 1;
}
