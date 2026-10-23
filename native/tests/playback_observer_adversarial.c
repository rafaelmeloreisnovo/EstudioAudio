/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Pure host falsifiers; no physical acoustic evidence is claimed.
 */
#include "rfa_playback_observer_core.h"

#define LONG_COUNT 17600000
#define HOT_BUCKET_FRAMES 2200000
static rfa_i16 long_pcm[LONG_COUNT];
static rfa_i16 short_pcm[2300];

int main(void) {
    rfa_playback_observation obs;
    int i;
    /* 2.2M * (32767 >> 4) overflows the former 32-bit energy bucket.
     * Other buckets are non-silent; a large level transition MUST remain.
     */
    for (i = 0; i < LONG_COUNT; ++i)
        long_pcm[i] = (rfa_i16)(i < HOT_BUCKET_FRAMES ? 32767 : 400);
    if (!rfa_playback_observe_mono(long_pcm, LONG_COUNT, 48000,
                                    (const rfa_u8 *)0, 0U, &obs)) return 1;
    if (obs.acoustic_shift_candidates == 0U) return 2;

    /* 1100 / 96000 = 11.46ms: former fixed 960 frame floor was wrong.
     * Required threshold is ceil(sample_rate/50) = 1920 frames at 96 kHz.
     */
    for (i = 0; i < 2300; ++i) short_pcm[i] = 0;
    short_pcm[0] = 400;
    short_pcm[1101] = 400;
    if (!rfa_playback_observe_mono(short_pcm, 1102, 96000,
                                    (const rfa_u8 *)0, 0U, &obs)) return 3;
    if (obs.silence_runs != 0U) return 4;

    /* Tiny and nonmultiple-of-eight buffers must not be misbucketed or OOB. */
    for (i = 1; i <= 31; ++i) {
        if (!rfa_playback_observe_mono(short_pcm, i, 48000,
                                       (const rfa_u8 *)0, 0U, &obs)) return 5;
        if (obs.analyzed_samples != (rfa_u32)i) return 6;
    }
    return 0;
}
