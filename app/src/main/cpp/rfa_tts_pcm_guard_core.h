/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Specialist T01: bounded PCM16 gain and fail-closed mute only.
 */
#ifndef RFA_TTS_PCM_GUARD_CORE_H
#define RFA_TTS_PCM_GUARD_CORE_H
#include "rfa_core_types.h"
#ifdef __cplusplus
extern "C" {
#endif
/* PCM16 in-place, capacity <=4096 samples; no extra output allocation.
 * alive is a gate flag (0 or 1), NOT inferred sensor authority.
 * Returns 0 on invalid input without modifying PCM.
 * Integer division truncates toward zero (ISO C); no signed shift UB. */
int rfa_tts_pcm_guard_q15(rfa_i16 *pcm, rfa_u32 count,
                          rfa_u32 gain_q15, rfa_u32 alive);
#ifdef __cplusplus
}
#endif
#endif
