/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#include "rfa_lms_core.h"
#include "rfa_int_math.h"

static rfa_i16 rfa_lms_clip_i16(rfa_i64 value) {
    if (value > 32767) return (rfa_i16)32767;
    if (value < -32768) return (rfa_i16)-32768;
    return (rfa_i16)value;
}

int rfa_lms_bind_q15(rfa_lms_q15 *state, rfa_i16 *weights_q15,
                     rfa_i16 *history_q15, int taps, rfa_i32 mu_q15) {
    if (state == (rfa_lms_q15 *)0 || weights_q15 == (rfa_i16 *)0 ||
        history_q15 == (rfa_i16 *)0) return 0;
    if (taps <= 0 || taps > 256) return 0;
    if (mu_q15 < 0) mu_q15 = 0;
    if (mu_q15 > 32767) mu_q15 = 32767;
    state->weights_q15 = weights_q15;
    state->history_q15 = history_q15;
    state->taps = taps;
    state->mu_q15 = mu_q15;
    state->position = 0;
    rfa_lms_reset_q15(state);
    return 1;
}

void rfa_lms_reset_q15(rfa_lms_q15 *state) {
    int i;
    if (state == (rfa_lms_q15 *)0) return;
    for (i = 0; i < state->taps; ++i) {
        state->weights_q15[i] = 0;
        state->history_q15[i] = 0;
    }
    state->position = 0;
}

rfa_i16 rfa_lms_cancel_sample_q15(rfa_lms_q15 *state,
                                  rfa_i16 primary, rfa_i16 reference) {
    rfa_i64 estimate_acc = 0;
    rfa_i32 estimate;
    rfa_i32 error;
    int tap;
    int index;

    if (state == (rfa_lms_q15 *)0 || state->weights_q15 == (rfa_i16 *)0 ||
        state->history_q15 == (rfa_i16 *)0 || state->taps <= 0) return primary;

    state->history_q15[state->position] = reference;
    index = state->position;
    for (tap = 0; tap < state->taps; ++tap) {
        estimate_acc += (rfa_i64)state->weights_q15[tap] *
                        (rfa_i64)state->history_q15[index];
        --index;
        if (index < 0) index = state->taps - 1;
    }
    estimate = (rfa_i32)(estimate_acc >> 15);
    error = (rfa_i32)primary - estimate;

    index = state->position;
    for (tap = 0; tap < state->taps; ++tap) {
        rfa_i64 delta = (rfa_i64)state->mu_q15 *
                        (rfa_i64)error *
                        (rfa_i64)state->history_q15[index];
        rfa_i64 updated = (rfa_i64)state->weights_q15[tap] + (delta >> 30);
        state->weights_q15[tap] = rfa_lms_clip_i16(updated);
        --index;
        if (index < 0) index = state->taps - 1;
    }

    ++state->position;
    if (state->position >= state->taps) state->position = 0;
    return rfa_lms_clip_i16(error);
}

void rfa_lms_cancel_block_q15(rfa_lms_q15 *state,
                              const rfa_i16 *primary,
                              const rfa_i16 *reference,
                              rfa_i16 *output, int count) {
    int i;
    if (state == (rfa_lms_q15 *)0 || primary == (const rfa_i16 *)0 ||
        reference == (const rfa_i16 *)0 || output == (rfa_i16 *)0 ||
        count <= 0) return;
    for (i = 0; i < count; ++i) {
        output[i] = rfa_lms_cancel_sample_q15(state, primary[i], reference[i]);
    }
}

int rfa_nlms_bind_q15(rfa_nlms_q15 *state, rfa_i16 *weights_q15,
                      rfa_i16 *history_q15, int taps, rfa_i32 mu_q15,
                      rfa_u64 epsilon_q30) {
    if (state == (rfa_nlms_q15 *)0 || weights_q15 == (rfa_i16 *)0 ||
        history_q15 == (rfa_i16 *)0) return 0;
    if (taps <= 0 || taps > 256) return 0;
    if (mu_q15 < 0) mu_q15 = 0;
    if (mu_q15 > 32767) mu_q15 = 32767;
    if (epsilon_q30 == 0ULL) epsilon_q30 = 1ULL;
    state->weights_q15 = weights_q15;
    state->history_q15 = history_q15;
    state->taps = taps;
    state->mu_q15 = mu_q15;
    state->epsilon_q30 = epsilon_q30;
    state->position = 0;
    rfa_nlms_reset_q15(state);
    return 1;
}

void rfa_nlms_reset_q15(rfa_nlms_q15 *state) {
    int i;
    if (state == (rfa_nlms_q15 *)0) return;
    for (i = 0; i < state->taps; ++i) {
        state->weights_q15[i] = 0;
        state->history_q15[i] = 0;
    }
    state->position = 0;
}

rfa_i16 rfa_nlms_cancel_sample_q15(rfa_nlms_q15 *state,
                                   rfa_i16 primary, rfa_i16 reference,
                                   int adapt) {
    rfa_i64 estimate_acc = 0LL;
    rfa_i32 estimate;
    rfa_i32 error;
    rfa_u64 energy_q30;
    int tap;
    int index;

    if (state == (rfa_nlms_q15 *)0 || state->weights_q15 == (rfa_i16 *)0 ||
        state->history_q15 == (rfa_i16 *)0 || state->taps <= 0) return primary;

    state->history_q15[state->position] = reference;
    index = state->position;
    for (tap = 0; tap < state->taps; ++tap) {
        estimate_acc += (rfa_i64)state->weights_q15[tap] *
                        (rfa_i64)state->history_q15[index];
        --index;
        if (index < 0) index = state->taps - 1;
    }
    estimate = (rfa_i32)(estimate_acc >> 15);
    error = (rfa_i32)primary - estimate;

    if (adapt != 0 && state->mu_q15 != 0) {
        energy_q30 = state->epsilon_q30;
        index = state->position;
        for (tap = 0; tap < state->taps; ++tap) {
            rfa_i64 x = (rfa_i64)state->history_q15[index];
            energy_q30 += (rfa_u64)(x * x);
            --index;
            if (index < 0) index = state->taps - 1;
        }

        index = state->position;
        for (tap = 0; tap < state->taps; ++tap) {
            rfa_i64 numerator = (rfa_i64)state->mu_q15 *
                                (rfa_i64)error *
                                (rfa_i64)state->history_q15[index];
            rfa_i64 delta = rfa_div_i64_u64_shift(numerator, energy_q30);
            rfa_i64 updated = (rfa_i64)state->weights_q15[tap] + delta;
            state->weights_q15[tap] = rfa_lms_clip_i16(updated);
            --index;
            if (index < 0) index = state->taps - 1;
        }
    }

    ++state->position;
    if (state->position >= state->taps) state->position = 0;
    return rfa_lms_clip_i16(error);
}

void rfa_nlms_cancel_block_q15(rfa_nlms_q15 *state,
                               const rfa_i16 *primary,
                               const rfa_i16 *reference,
                               rfa_i16 *output, int count, int adapt) {
    int i;
    if (state == (rfa_nlms_q15 *)0 || primary == (const rfa_i16 *)0 ||
        reference == (const rfa_i16 *)0 || output == (rfa_i16 *)0 ||
        count <= 0) return;
    for (i = 0; i < count; ++i) {
        output[i] = rfa_nlms_cancel_sample_q15(
                state, primary[i], reference[i], adapt);
    }
}

int rfa_nlms_trial_block_q15(rfa_nlms_q15 *state,
    const rfa_i16 *primary,const rfa_i16 *reference,rfa_i16 *output,
    int count,int allow_adapt,rfa_nlms_trial *trial) {
    int i, saved_position;
    rfa_u64 reference_energy=0;
    if (!state || !primary || !reference || !output || !trial ||
        !state->weights_q15 || !state->history_q15 ||
        state->taps<1 || state->taps>256 || count<1 || count>4096 ||
        state->position<0 || state->position>=state->taps ||
        state->mu_q15<0 || state->mu_q15>32767 || !state->epsilon_q30 ||
        state->epsilon_q30>0xffffffffffffffffULL-274877906944ULL ||
        output==primary || output==reference || output==trial->baseline_q15 ||
        state->weights_q15==state->history_q15) return RFA_NLMS_REJECT_INVALID;
    saved_position=state->position;
    for (i=0;i<state->taps;++i) {
        trial->weights_saved[i]=state->weights_q15[i];
        trial->history_saved[i]=state->history_q15[i];
    }
    rfa_nlms_cancel_block_q15(state,primary,reference,trial->baseline_q15,count,0);
    trial->baseline_energy=0;trial->candidate_energy=0;
    for (i=0;i<count;++i) {
        rfa_i64 x=trial->baseline_q15[i], r=reference[i];
        trial->baseline_energy+=(rfa_u64)(x*x);
        reference_energy+=(rfa_u64)(r*r);
    }
    if (!allow_adapt || !reference_energy || !state->mu_q15) {
        for (i=0;i<count;++i) output[i]=trial->baseline_q15[i];
        return RFA_NLMS_FROZEN_GENERIC;
    }
    for (i=0;i<state->taps;++i) {
        state->weights_q15[i]=trial->weights_saved[i];
        state->history_q15[i]=trial->history_saved[i];
    }
    state->position=saved_position;
    rfa_nlms_cancel_block_q15(state,primary,reference,output,count,1);
    for (i=0;i<count;++i) {
        rfa_i64 x=output[i];trial->candidate_energy+=(rfa_u64)(x*x);
    }
    if (trial->candidate_energy<=trial->baseline_energy) return RFA_NLMS_ACCEPT_ADAPTED;
    for (i=0;i<state->taps;++i) {
        state->weights_q15[i]=trial->weights_saved[i];
        state->history_q15[i]=trial->history_saved[i];
    }
    state->position=saved_position;
    rfa_nlms_cancel_block_q15(state,primary,reference,output,count,0);
    return RFA_NLMS_FALLBACK_ROLLBACK;
}
