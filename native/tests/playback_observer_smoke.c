/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
 */
#include "rfa_playback_observer_core.h"

static rfa_i16 pcm[4096];

int main(void) {
    const rfa_u8 text[] = "Primeira frase. Segunda frase, com pausa? Final!";
    rfa_playback_observation obs;
    int i;
    for (i = 0; i < 4096; ++i) pcm[i] = 0;
    for (i = 0; i < 700; ++i) pcm[i] = (rfa_i16)((i & 1) ? 900 : -900);
    for (i = 1700; i < 2400; ++i) pcm[i] = (rfa_i16)((i & 1) ? 1800 : -1600);
    pcm[2400] = 32767;
    pcm[2401] = -32768;
    for (i = 3000; i < 4096; ++i) pcm[i] = (rfa_i16)((i & 3) ? 2600 : -2600);

    if (!rfa_playback_observe_mono(pcm, 4096, 48000, text, (rfa_u32)sizeof(text) - 1U, &obs)) return 1;
    if (obs.text_words < 6U) return 2;
    if (obs.text_pause_marks < 4U) return 3;
    if (obs.silence_runs < 1U) return 4;
    if ((obs.flags & RFA_PLAYBACK_FLAG_AUDIO_HAS_SILENCE) == 0U) return 5;
    if ((obs.flags & RFA_PLAYBACK_FLAG_GLITCH_CANDIDATE) == 0U) return 6;
    if (obs.glitch_candidates < 1U) return 7;
    if (obs.non_silent_samples < 1000U) return 8;
    if (obs.energy == 0ULL || obs.peak_abs < 32767ULL) return 9;
    if (obs.acoustic_shift_candidates == 0U) return 10;

    if (rfa_playback_observe_mono((const rfa_i16 *)0, 1, 48000, text, 2U, &obs)) return 11;
    if (rfa_playback_observe_mono(pcm, 1, 0, text, 2U, &obs)) return 12;
    if (!rfa_playback_observe_mono(pcm, 128, 16000, (const rfa_u8 *)0, 0U, &obs)) return 13;
    if (obs.text_words != 0U || obs.text_pause_marks != 0U) return 14;
    return 0;
}
