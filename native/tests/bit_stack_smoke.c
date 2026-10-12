/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1 */
#include "rfa_bit_stack_core.h"
int main(void) {
    rfa_u8 memory[10] = {0xa5,0,0,0,0,0,0,0,0,0x5a};
    rfa_bit_stack s;
    rfa_u32 out = 777, width, v;
    if (!rfa_bit_stack_bind(&s, memory+1, 64)) return 1;
    for (width=1; width<=32; ++width) {
        v = width == 32 ? 0xffffffffu : (1u << width)-1u;
        if (!rfa_bit_stack_push(&s, 5, 3) || !rfa_bit_stack_push(&s, v, width)) return 2;
        if (!rfa_bit_stack_pop(&s,width,&out) || out!=v) return 3;
        if (!rfa_bit_stack_pop(&s,3,&out) || out!=5 || s.used_bits) return 4;
    }
    if (!rfa_bit_stack_push(&s,0xffffffffu,32) || !rfa_bit_stack_push(&s,0x12345678u,32)) return 5;
    if (rfa_bit_stack_push(&s,1,1) || s.used_bits!=64 || memory[0]!=0xa5 || memory[9]!=0x5a) return 6;
    if (!rfa_bit_stack_pop(&s,32,&out) || out!=0x12345678u) return 7;
    if (!rfa_bit_stack_pop(&s,32,&out) || out!=0xffffffffu) return 8;
    out=777;
    if (rfa_bit_stack_pop(&s,1,&out) || out!=777) return 9;
    if (rfa_bit_stack_push(&s,8,3) || rfa_bit_stack_push(&s,0,0) || rfa_bit_stack_push(&s,0,33)) return 10;
    if (rfa_bit_stack_push(0,0,1) || rfa_bit_stack_pop(0,1,&out) || rfa_bit_stack_pop(&s,1,0)) return 11;
    s.used_bits=65;
    if (rfa_bit_stack_push(&s,0,1) || rfa_bit_stack_pop(&s,1,&out) || s.used_bits!=65) return 12;
    return 0;
}
