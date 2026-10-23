/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
 */
#ifndef RFA_PTBR_CORE_H
#define RFA_PTBR_CORE_H
#include "rfa_core_types.h"
#ifdef __cplusplus
extern "C" {
#endif
/* Intentionally limited, deterministic PT-BR grapheme-to-phone model.
 * This is NOT validated intelligible, natural-language or accessible TTS. */
enum {
 RFA_PT_PAUSE=1, RFA_PT_A, RFA_PT_E, RFA_PT_I, RFA_PT_O, RFA_PT_U,
 RFA_PT_AN, RFA_PT_EN, RFA_PT_ON, RFA_PT_S, RFA_PT_SH, RFA_PT_ZH,
 RFA_PT_F, RFA_PT_V, RFA_PT_STOP, RFA_PT_R, RFA_PT_L, RFA_PT_N
};
typedef struct {
 rfa_u32 start, end, phone, frames;
} rfa_ptbr_event;
typedef struct {
 rfa_u32 index, remaining, phase, noise, phone, step;
} rfa_ptbr_state;
int rfa_ptbr_next(const rfa_u8 *utf8, rfa_u32 bytes,
                  rfa_u32 *cursor, rfa_ptbr_event *event);
int rfa_ptbr_validate(const rfa_u8 *utf8, rfa_u32 bytes);
void rfa_ptbr_reset(rfa_ptbr_state *state);
rfa_u32 rfa_ptbr_render(rfa_ptbr_state *state, const rfa_u8 *utf8,
                        rfa_u32 bytes, rfa_i16 *pcm, rfa_u32 capacity);
/* V2 robotic cadence: caller-owned state, 16-kHz mono, integer-only.
 * The existing rfa_ptbr_render() remains unchanged for A/B comparison.
 * Distinct punctuation silence + mild deterministic smoothing are NOT proof
 * of intelligible, accessible, natural, or speaker-specific TTS. */
typedef struct {
 rfa_ptbr_state voice;
 rfa_u32 silence;
 rfa_u32 pending;
 rfa_i32 smooth;
} rfa_ptbr_robot_state;
void rfa_ptbr_robot_reset(rfa_ptbr_robot_state *state);
rfa_u32 rfa_ptbr_robot_render(rfa_ptbr_robot_state *state,
                            const rfa_u8 *utf8, rfa_u32 bytes,
                            rfa_i16 *pcm, rfa_u32 capacity);
#ifdef __cplusplus
}
#endif
#endif
