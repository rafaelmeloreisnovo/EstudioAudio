/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
 */
#include "rfa_tele_tts_core.h"
static rfa_i16 reference[4096];
static rfa_i16 chunks[4096];
static rfa_i16 guard[2];
int main(void) {
    const rfa_u8 words[] = "um dois tres";
    const rfa_u8 voice[] = "Ola, voce!\n";
    rfa_tele_state tele;
    rfa_tts_state first, second;
    rfa_u32 a, b, i, total, nonzero = 0U;
    if (!rfa_tele_reset(&tele, words, 12U, 500U)) return 1;
    if (tele.cursor != 0U || tele.word_end != 2U) return 2;
    if (rfa_tele_advance(&tele, words, 12U, 499U) != 0) return 3;
    if (rfa_tele_advance(&tele, words, 12U, 1U) != 1) return 4;
    if (tele.cursor != 3U || tele.word_end != 7U) return 5;
    if (rfa_tele_advance(&tele, words, 12U, 1000U) != 2) return 6;
    if (tele.cursor != 12U || tele.words_finished != 3U) return 7;
    if (rfa_tele_advance(&tele, words, 12U, 60001U) != -1) return 8;
    if (rfa_tele_reset(&tele, words, 12U, 0U)) return 9;
    rfa_tts_reset(&first);
    rfa_tts_reset(&second);
    a = rfa_tts_render(&first, voice, 11U, reference, 4096U);
    b = rfa_tts_render(&second, voice, 11U, chunks, 2000U);
    b += rfa_tts_render(&second, voice, 11U, chunks + b, 4096U - b);
    if (a != 4096U || b != a) return 10;
    for (i = 0U; i < a; ++i) {
        if (reference[i] != chunks[i]) return 11;
        if (reference[i]) ++nonzero;
    }
    if (nonzero < 500U) return 12;
    if (first.index != second.index || first.remaining != second.remaining ||
        first.phase != second.phase || first.noise != second.noise) return 13;
    total = a;
    for (i = 0U; i < 100U; ++i) {
        rfa_u32 n = rfa_tts_render(&first, voice, 11U, reference, 4096U);
        total += n;
        if (!n) break;
    }
    if (i == 100U || total < 4096U || first.index != 11U) return 14;
    guard[0] = 101; guard[1] = 202;
    if (rfa_tts_render(&first, voice, 4097U, guard, 2U) != 0U) return 15;
    if (guard[0] != 101 || guard[1] != 202) return 16;
    if (rfa_tts_render((rfa_tts_state*)0, voice, 11U, guard, 2U) != 0U) return 17;
    return 0;
}
