/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
 */
#ifndef RFA_PLAYBACK_OBSERVER_CORE_H
#define RFA_PLAYBACK_OBSERVER_CORE_H
#include "rfa_core_types.h"
#ifdef __cplusplus
extern "C" {
#endif

enum {
    RFA_PLAYBACK_FLAG_TEXT_HAS_PAUSE = 1,
    RFA_PLAYBACK_FLAG_AUDIO_HAS_SILENCE = 2,
    RFA_PLAYBACK_FLAG_GLITCH_CANDIDATE = 4,
    RFA_PLAYBACK_FLAG_ACOUSTIC_SHIFT_CANDIDATE = 8,
    RFA_PLAYBACK_FLAG_PAUSE_MISMATCH_CANDIDATE = 16
};

typedef struct {
    rfa_u32 text_bytes;
    rfa_u32 text_words;
    rfa_u32 text_pause_marks;
    rfa_u32 analyzed_samples;
    rfa_u32 non_silent_samples;
    rfa_u32 zero_crossings;
    rfa_u32 silence_runs;
    rfa_u32 max_silence_run;
    rfa_u32 glitch_candidates;
    rfa_u32 acoustic_shift_candidates;
    rfa_u32 flags;
    rfa_u64 energy;
    rfa_u64 peak_abs;
} rfa_playback_observation;

int rfa_playback_observe_mono(const rfa_i16 *samples, int count,
                              int sample_rate,
                              const rfa_u8 *text, rfa_u32 text_len,
                              rfa_playback_observation *out);

#ifdef __cplusplus
}
#endif
#endif
