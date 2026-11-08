/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1 */
/* One operation: saturated sum of two signed PCM16 values.
 * Inputs w0/w1 sign-extended PCM16; result w0.
 * AAPCS64 x18 reserved, w2 caller-saved, no stack/syscall/heap. */
.text
.p2align 2
.global rfa_pcm_sat_add_s16
.hidden rfa_pcm_sat_add_s16
.type rfa_pcm_sat_add_s16, %function
rfa_pcm_sat_add_s16:
    add     w0, w0, w1
    mov     w2, #32767
    cmp     w0, w2
    csel    w0, w0, w2, le
    mov     w2, #-32768
    cmp     w0, w2
    csel    w0, w0, w2, ge
    ret
.size rfa_pcm_sat_add_s16, .-rfa_pcm_sat_add_s16
.section .note.GNU-stack,"",%progbits
