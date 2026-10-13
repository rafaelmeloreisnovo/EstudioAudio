/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
 */
#include "rfa_tele_tts_core.h"

static int rfa_ws(rfa_u8 value) {
    return value == (rfa_u8)' ' || value == 9U ||
           value == 10U || value == 13U;
}
static rfa_u32 rfa_skip(const rfa_u8 *text, rfa_u32 n, rfa_u32 from) {
    while (from < n && rfa_ws(text[from])) ++from;
    return from;
}
static rfa_u32 rfa_end(const rfa_u8 *text, rfa_u32 n, rfa_u32 from) {
    while (from < n && !rfa_ws(text[from])) ++from;
    return from;
}
int rfa_tele_reset(rfa_tele_state *state, const rfa_u8 *text,
                   rfa_u32 bytes, rfa_u32 ms_per_word) {
    if (!state || !text || bytes > 65536U ||
        ms_per_word < 100U || ms_per_word > 4000U) return 0;
    state->cursor = rfa_skip(text, bytes, 0U);
    state->word_end = rfa_end(text, bytes, state->cursor);
    state->words_finished = 0U;
    state->pending_ms = 0U;
    state->ms_per_word = ms_per_word;
    return 1;
}
int rfa_tele_advance(rfa_tele_state *state, const rfa_u8 *text,
                     rfa_u32 bytes, rfa_u32 elapsed_ms) {
    rfa_u32 count = 0U;
    if (!state || !text || bytes > 65536U ||
        elapsed_ms > 60000U || state->ms_per_word < 100U ||
        state->ms_per_word > 4000U || state->cursor > bytes ||
        state->word_end > bytes || state->pending_ms >= state->ms_per_word)
        return -1;
    if (state->cursor == bytes) return 0;
    state->pending_ms += elapsed_ms; /* <= 63999, no overflow */
    while (state->pending_ms >= state->ms_per_word && state->cursor < bytes) {
        state->pending_ms -= state->ms_per_word;
        state->cursor = rfa_skip(text, bytes, state->word_end);
        state->word_end = rfa_end(text, bytes, state->cursor);
        ++state->words_finished;
        ++count;
    }
    return (int)count;
}
void rfa_tts_reset(rfa_tts_state *state) {
    if (!state) return;
    state->index = 0U;
    state->remaining = 0U;
    state->phase = 0U;
    state->step = 0U;
    state->noise = 0x13579BDFU;
    state->kind = 0U;
}
/* Portuguese Latin-1 common accents (UTF-8 C3 xx), intentionally bounded. */
static rfa_u32 rfa_latin(rfa_u32 chr, rfa_u32 suffix) {
    if (chr != 195U) return chr;
    switch (suffix) {
        case 0xA1U: case 0xA0U: case 0xA2U: case 0xA3U:
        case 0x81U: case 0x80U: case 0x82U: case 0x83U: return (rfa_u32)'a';
        case 0xA9U: case 0xAAU: case 0x89U: case 0x8AU: return (rfa_u32)'e';
        case 0xADU: case 0x8DU: return (rfa_u32)'i';
        case 0xB3U: case 0xB4U: case 0xB5U:
        case 0x93U: case 0x94U: case 0x95U: return (rfa_u32)'o';
        case 0xBAU: case 0x9AU: return (rfa_u32)'u';
        case 0xA7U: case 0x87U: return (rfa_u32)'s';
        default: return 0U;
    }
}
static void rfa_load_symbol(rfa_tts_state *state, const rfa_u8 *text,
                            rfa_u32 bytes) {
    rfa_u32 chr = text[state->index++];
    if (chr == 195U && state->index < bytes) {
        chr = rfa_latin(chr, text[state->index]);
        ++state->index;
    }
    if (chr >= 65U && chr <= 90U) chr += 32U;
    state->phase = 0U;
    state->step = 31500000U + (chr & 7U) * 800000U;
    state->kind = 0U;
    state->remaining = 320U;
    if (chr == (rfa_u32)'a') state->kind = 1U;
    else if (chr == (rfa_u32)'e') state->kind = 2U;
    else if (chr == (rfa_u32)'i') state->kind = 3U;
    else if (chr == (rfa_u32)'o') state->kind = 4U;
    else if (chr == (rfa_u32)'u') state->kind = 5U;
    else if (chr == (rfa_u32)'s' || chr == (rfa_u32)'f' ||
             chr == (rfa_u32)'x' || chr == (rfa_u32)'z') state->kind = 6U;
    else if (chr >= 97U && chr <= 122U) state->kind = 7U;
    if (state->kind >= 1U && state->kind <= 5U) state->remaining = 1280U;
    else if (state->kind == 6U || state->kind == 7U) state->remaining = 640U;
    else if (chr == (rfa_u32)'.' || chr == (rfa_u32)'!' ||
             chr == (rfa_u32)'?') state->remaining = 800U;
}
static rfa_i32 rfa_triangle(rfa_u32 phase) {
    rfa_i32 p = (rfa_i32)(phase >> 16);
    return p < 32768 ? p * 2 - 32767 : 98303 - p * 2;
}
rfa_u32 rfa_tts_render(rfa_tts_state *state, const rfa_u8 *utf8,
                       rfa_u32 bytes, rfa_i16 *pcm, rfa_u32 capacity) {
    rfa_u32 written = 0U;
    if (!state || !utf8 || !pcm || bytes > 4096U ||
        capacity > 4096U || state->index > bytes || !capacity) return 0U;
    if (state->noise == 0U) state->noise = 0x13579BDFU;
    while (written < capacity) {
        rfa_i32 sample = 0;
        if (!state->remaining) {
            if (state->index >= bytes) break;
            rfa_load_symbol(state, utf8, bytes);
        }
        if (state->kind >= 1U && state->kind <= 5U) {
            rfa_i32 first, second;
            state->phase += state->step;
            first = rfa_triangle(state->phase);
            second = rfa_triangle(state->phase << 1);
            sample = (first * 3 + second * (rfa_i32)state->kind) / 16;
        } else if (state->kind == 6U) {
            state->noise ^= state->noise << 13;
            state->noise ^= state->noise >> 17;
            state->noise ^= state->noise << 5;
            sample = ((rfa_i32)(state->noise >> 16) - 32768) / 10;
        } else if (state->kind == 7U) {
            state->phase += state->step;
            sample = rfa_triangle(state->phase) / 10;
        }
        pcm[written++] = (rfa_i16)sample; /* bounded within +/- 16384 */
        --state->remaining;
    }
    return written;
}
