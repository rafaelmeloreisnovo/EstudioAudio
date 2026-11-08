/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1 */
/* One operation: saturated sum of two signed PCM16 values.
 * Inputs r0/r1 sign-extended PCM16; result r0.
 * AAPCS32 caller-saved only; no stack, syscall, heap or global state. */
.syntax unified
.arm
.text
.p2align 2
.global rfa_pcm_sat_add_s16
.hidden rfa_pcm_sat_add_s16
.type rfa_pcm_sat_add_s16, %function
rfa_pcm_sat_add_s16:
    add     r0, r0, r1
    ssat    r0, #16, r0
    bx      lr
.size rfa_pcm_sat_add_s16, .-rfa_pcm_sat_add_s16
.section .note.GNU-stack,"",%progbits
