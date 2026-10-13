/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
 */
#include "rfa_playback_observer_core.h"

static rfa_u32 rfa_abs_i16(rfa_i16 value) {
    rfa_i32 v = (rfa_i32)value;
    if (v < 0) v = -v;
    return (rfa_u32)v;
}

static int rfa_is_space(rfa_u8 value) {
    return value == (rfa_u8)' ' || value == 9U || value == 10U || value == 13U;
}

static int rfa_is_pause_mark(rfa_u8 value) {
    return value == (rfa_u8)'.' || value == (rfa_u8)',' ||
           value == (rfa_u8)';' || value == (rfa_u8)':' ||
           value == (rfa_u8)'!' || value == (rfa_u8)'?';
}

static rfa_u32 rfa_silence_floor_for_rate(int sample_rate) {
    if (sample_rate <= 16000) return 320U;
    if (sample_rate <= 24000) return 480U;
    if (sample_rate <= 44100) return 882U;
    return 960U;
}

static void rfa_scan_text(const rfa_u8 *text, rfa_u32 text_len,
                          rfa_playback_observation *out) {
    rfa_u32 i;
    int in_word = 0;
    for (i = 0U; i < text_len; ++i) {
        rfa_u8 value = text[i];
        if (rfa_is_pause_mark(value)) out->text_pause_marks++;
        if (rfa_is_space(value) || rfa_is_pause_mark(value)) {
            in_word = 0;
        } else if (!in_word) {
            out->text_words++;
            in_word = 1;
        }
    }
    out->text_bytes = text_len;
    if (out->text_pause_marks > 0U) out->flags |= RFA_PLAYBACK_FLAG_TEXT_HAS_PAUSE;
}

int rfa_playback_observe_mono(const rfa_i16 *samples, int count,
                              int sample_rate,
                              const rfa_u8 *text, rfa_u32 text_len,
                              rfa_playback_observation *out) {
    rfa_u32 silence_floor;
    rfa_u32 silence_run = 0U;
    rfa_u32 bucket_energy[8];
    rfa_u32 bucket_cross[8];
    rfa_u32 bucket_non_silent[8];
    rfa_i16 previous = 0;
    int i;
    int bucket;
    if (out == (rfa_playback_observation *)0) return 0;
    *out = (rfa_playback_observation){0U,0U,0U,0U,0U,0U,0U,0U,0U,0U,0U,0ULL,0ULL};
    if (samples == (const rfa_i16 *)0 || count <= 0 || sample_rate <= 0) return 0;
    if (text != (const rfa_u8 *)0 && text_len > 0U) rfa_scan_text(text, text_len, out);

    for (bucket = 0; bucket < 8; ++bucket) {
        bucket_energy[bucket] = 0U;
        bucket_cross[bucket] = 0U;
        bucket_non_silent[bucket] = 0U;
    }

    silence_floor = rfa_silence_floor_for_rate(sample_rate);
    out->analyzed_samples = (rfa_u32)count;

    for (i = 0; i < count; ++i) {
        rfa_u32 abs_now = rfa_abs_i16(samples[i]);
        rfa_u32 delta = 0U;
        bucket = (int)(((rfa_u32)i * 8U) / (rfa_u32)count);
        if (bucket > 7) bucket = 7;
        out->energy += (rfa_u64)abs_now * (rfa_u64)abs_now;
        if ((rfa_u64)abs_now > out->peak_abs) out->peak_abs = (rfa_u64)abs_now;
        bucket_energy[bucket] += abs_now >> 4;
        if (abs_now > 384U) {
            out->non_silent_samples++;
            bucket_non_silent[bucket]++;
            if (silence_run >= silence_floor) {
                out->silence_runs++;
                if (silence_run > out->max_silence_run) out->max_silence_run = silence_run;
            }
            silence_run = 0U;
        } else {
            silence_run++;
        }
        if (i > 0) {
            if ((samples[i] < 0 && previous >= 0) || (samples[i] >= 0 && previous < 0)) {
                out->zero_crossings++;
                bucket_cross[bucket]++;
            }
            delta = samples[i] > previous ?
                    (rfa_u32)((rfa_i32)samples[i] - (rfa_i32)previous) :
                    (rfa_u32)((rfa_i32)previous - (rfa_i32)samples[i]);
            if (delta > 24000U || abs_now > 32000U) out->glitch_candidates++;
        }
        previous = samples[i];
    }
    if (silence_run >= silence_floor) {
        out->silence_runs++;
        if (silence_run > out->max_silence_run) out->max_silence_run = silence_run;
    }
    if (out->silence_runs > 0U) out->flags |= RFA_PLAYBACK_FLAG_AUDIO_HAS_SILENCE;
    if (out->glitch_candidates > 0U) out->flags |= RFA_PLAYBACK_FLAG_GLITCH_CANDIDATE;

    for (bucket = 1; bucket < 8; ++bucket) {
        rfa_u32 prev_energy = bucket_energy[bucket - 1];
        rfa_u32 now_energy = bucket_energy[bucket];
        rfa_u32 prev_cross = bucket_cross[bucket - 1];
        rfa_u32 now_cross = bucket_cross[bucket];
        if (bucket_non_silent[bucket - 1] == 0U || bucket_non_silent[bucket] == 0U) continue;
        if ((prev_energy > (now_energy << 2)) || (now_energy > (prev_energy << 2))) {
            out->acoustic_shift_candidates++;
        } else if (prev_cross > now_cross + 32U || now_cross > prev_cross + 32U) {
            out->acoustic_shift_candidates++;
        }
    }
    if (out->acoustic_shift_candidates > 0U) out->flags |= RFA_PLAYBACK_FLAG_ACOUSTIC_SHIFT_CANDIDATE;
    if (out->text_pause_marks > out->silence_runs + 1U && out->non_silent_samples > 0U) {
        out->flags |= RFA_PLAYBACK_FLAG_PAUSE_MISMATCH_CANDIDATE;
    }
    return 1;
}
