/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Strict host-only falsifiers. One module per job; no native OS dependencies.
 */
#include "rfa_sensor_delta_core.h"
#include "rfa_tts_pcm_guard_core.h"
#include "rfa_dual_watchdog_core.h"

int main(void) {
    const rfa_i16 origin[3] = {0, 0, 0};
    const rfa_i16 endpoint[3] = {32767, 0, 0};
    const rfa_i16 extreme_lo[3] = {-32768, -32768, -32768};
    const rfa_i16 extreme_hi[3] = {32767, 32767, 32767};
    rfa_u32 energy = 991U;
    rfa_i16 pcm[5] = {101, -32768, 32767, -101, 4321};
    rfa_dual_watchdog watch;
    if (!rfa_sensor_delta3_energy(origin, endpoint, &energy) || energy != 16383U)
        return 1;
    if (!rfa_sensor_delta3_energy(extreme_lo, extreme_hi, &energy) ||
        energy != 65535U) return 2;
    energy = 123U;
    if (rfa_sensor_delta3_energy((const rfa_i16 *)0, origin, &energy) ||
        energy != 123U) return 3;

    if (rfa_tts_pcm_guard_q15(pcm, 5U, 32768U, 1U)) return 4;
    if (pcm[0] != 101 || pcm[1] != -32768) return 5;
    if (!rfa_tts_pcm_guard_q15(pcm, 5U, 16384U, 1U) ||
        pcm[0] != 50 || pcm[1] != -16384 || pcm[2] != 16383 ||
        pcm[3] != -50 || pcm[4] != 2160) return 6;
    if (!rfa_tts_pcm_guard_q15(pcm, 5U, 32767U, 0U)) return 7;
    if (pcm[0] || pcm[1] || pcm[2] || pcm[3] || pcm[4]) return 8;
    if (!rfa_tts_pcm_guard_q15(pcm, 0U, 0U, 1U)) return 9;
    if (rfa_tts_pcm_guard_q15((rfa_i16 *)0, 5U, 0U, 1U)) return 10;
    if (rfa_tts_pcm_guard_q15(pcm, 4097U, 1U, 1U)) return 11;
    if (rfa_tts_pcm_guard_q15(pcm, 5U, 1000U, 2U)) return 12;
    rfa_dual_watchdog_reset(&watch);
    if (rfa_dual_watchdog_check(&watch, 0U, 100U, 100U, 0U)) return 13;
    if (!rfa_dual_watchdog_mark(&watch, 1U, 1000U)) return 14;
    if (rfa_dual_watchdog_check(&watch, 1010U, 20U, 20U, 0U) != 5U)
        return 15; /* audio observed, voice-only eligibility */
    if (rfa_dual_watchdog_check(&watch, 1010U, 20U, 20U, 1U) != 1U)
        return 16;
    if (!rfa_dual_watchdog_mark(&watch, 2U, 1009U)) return 17;
    if (rfa_dual_watchdog_check(&watch, 1010U, 20U, 20U, 1U) != 7U)
        return 18;
    if (rfa_dual_watchdog_check(&watch, 2000U, 20U, 20U, 1U) != 0U)
        return 19; /* stale both, FAILSAFE mute */
    if (rfa_dual_watchdog_mark(&watch, 1U, 999U)) return 20; /* rollback */
    if (rfa_dual_watchdog_check(&watch, 998U, 20U, 20U, 1U)) return 21;
    if (rfa_dual_watchdog_mark(&watch, 3U, 3000U)) return 22;
    if (rfa_dual_watchdog_check(&watch, 1010U, 0U, 20U, 1U)) return 23;
    return 0;
}
