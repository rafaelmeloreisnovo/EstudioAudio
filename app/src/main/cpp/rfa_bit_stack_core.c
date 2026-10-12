/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1 */
#include "rfa_bit_stack_core.h"
int rfa_bit_stack_bind(rfa_bit_stack *stack, rfa_u8 *storage, rfa_u32 capacity_bits) {
    if (!stack || !storage || !capacity_bits) return 0;
    stack->storage = storage;
    stack->capacity_bits = capacity_bits;
    stack->used_bits = 0;
    return 1;
}
int rfa_bit_stack_push(rfa_bit_stack *stack, rfa_u32 symbol, rfa_u32 width) {
    rfa_u32 i;
    if (!stack || !stack->storage || !width || width > 32u ||
        stack->used_bits > stack->capacity_bits ||
        width > stack->capacity_bits - stack->used_bits ||
        (width < 32u && (symbol >> width))) return 0;
    for (i = 0; i < width; ++i) {
        rfa_u32 bit = stack->used_bits + i;
        rfa_u8 mask = (rfa_u8)(1u << (bit & 7u));
        rfa_u8 old = stack->storage[bit >> 3];
        stack->storage[bit >> 3] = (rfa_u8)((old & (rfa_u8)~mask) |
            (((symbol >> i) & 1u) ? mask : 0u));
    }
    stack->used_bits += width;
    return 1;
}
int rfa_bit_stack_pop(rfa_bit_stack *stack, rfa_u32 width, rfa_u32 *symbol) {
    rfa_u32 i, value = 0, start;
    if (!stack || !stack->storage || !symbol || !width || width > 32u ||
        stack->used_bits > stack->capacity_bits || width > stack->used_bits) return 0;
    start = stack->used_bits - width;
    for (i = 0; i < width; ++i) {
        rfa_u32 bit = start + i;
        value |= ((rfa_u32)((stack->storage[bit >> 3] >> (bit & 7u)) & 1u)) << i;
    }
    stack->used_bits = start;
    *symbol = value;
    return 1;
}
