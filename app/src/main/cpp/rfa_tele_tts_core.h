/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
 */
#ifndef RFA_TELE_TTS_CORE_H
#define RFA_TELE_TTS_CORE_H
#include "rfa_core_types.h"
#ifdef __cplusplus
extern "C" {
#endif

/* No external clock: caller supplies bounded elapsed milliseconds.
 * Bytes are UTF-8; whitespace controls word boundaries. No heap or I/O. */
typedef struct {
    rfa_u32 cursor;
    rfa_u32 word_end;
    rfa_u32 words_finished;
    rfa_u32 pending_ms;
    rfa_u32 ms_per_word;
} rfa_tele_state;
int rfa_tele_reset(rfa_tele_state *state, const rfa_u8 *text,
                   rfa_u32 bytes, rfa_u32 ms_per_word);
int rfa_tele_advance(rfa_tele_state *state, const rfa_u8 *text,
                     rfa_u32 bytes, rfa_u32 elapsed_ms);

/* Experimental phonetic GUIDE synthesis at exactly 16000 Hz mono PCM16.
 * This is NOT validated intelligible / natural-language TTS.
 * State is six 32-bit words and fully caller-owned, even across JNI calls.
 * Mutable global state, syscalls, library calls and dynamic memory: NONE. */
typedef struct {
    rfa_u32 index;
    rfa_u32 remaining;
    rfa_u32 phase;
    rfa_u32 step;
    rfa_u32 noise;
    rfa_u32 kind;
} rfa_tts_state;
void rfa_tts_reset(rfa_tts_state *state);
rfa_u32 rfa_tts_render(rfa_tts_state *state, const rfa_u8 *utf8,
                       rfa_u32 bytes, rfa_i16 *pcm, rfa_u32 capacity);
#ifdef __cplusplus
}
#endif
#endif
