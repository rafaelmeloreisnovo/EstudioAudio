/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1 */
/* One operation: abs(signed PCM16), including -32768 -> 32768.
 * Input r0 low 16 bits; output r0; scratch r1 caller-saved. */
.syntax unified
.arm
.text
.p2align 2
.global rfa_pcm_abs_u16
.hidden rfa_pcm_abs_u16
.type rfa_pcm_abs_u16, %function
rfa_pcm_abs_u16:
    sxth    r0, r0
    asr     r1, r0, #31
    eor     r0, r0, r1
    sub     r0, r0, r1
    bx      lr
.size rfa_pcm_abs_u16, .-rfa_pcm_abs_u16
.section .note.GNU-stack,"",%progbits
