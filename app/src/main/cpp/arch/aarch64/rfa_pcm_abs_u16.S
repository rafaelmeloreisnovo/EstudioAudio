/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1 */
/* One operation: abs(signed PCM16), including -32768 -> 32768.
 * Input w0 low 16 bits; output w0; scratch w1 caller-saved. */
.text
.p2align 2
.global rfa_pcm_abs_u16
.hidden rfa_pcm_abs_u16
.type rfa_pcm_abs_u16, %function
rfa_pcm_abs_u16:
    sxth    w0, w0
    asr     w1, w0, #31
    eor     w0, w0, w1
    sub     w0, w0, w1
    ret
.size rfa_pcm_abs_u16, .-rfa_pcm_abs_u16
.section .note.GNU-stack,"",%progbits
