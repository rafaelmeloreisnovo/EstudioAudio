/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * T01 hot loop: fixed-point only, no malloc, libc, floating-point, branches
 * in the per-sample body; compiler's machine code must be inspected per ABI.
 */
#include "rfa_tts_pcm_guard_core.h"
int rfa_tts_pcm_guard_q15(rfa_i16 *pcm, rfa_u32 count,
                          rfa_u32 gain_q15, rfa_u32 alive) {
    rfa_u32 i;
    rfa_i32 gain;
    if (!pcm || count > 4096U || gain_q15 > 32767U || alive > 1U)
        return 0;
    gain = (rfa_i32)(gain_q15 & (0U - alive));
    for (i = 0U; i < count; ++i) {
        /* -32768*32767 fits signed 32 bits. Result fits PCM16. */
        pcm[i] = (rfa_i16)(((rfa_i32)pcm[i] * gain) / 32768);
    }
    return 1;
}
