/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1 */
#ifndef RFA_BIT_STACK_CORE_H
#define RFA_BIT_STACK_CORE_H
#include "rfa_core_types.h"
/* Caller-owned LIFO symbol bits. Width 1..32; no allocation, I/O or encoding claim.
 * Lowest bit first in storage; pop requires the same width as the last push.
 * Storage must cover ceil(capacity_bits/8) bytes and not alias state/output.
 * Failed operations leave state/storage/output unchanged. Not thread-safe.
 */
typedef struct {
    rfa_u8 *storage;
    rfa_u32 capacity_bits;
    rfa_u32 used_bits;
} rfa_bit_stack;
int rfa_bit_stack_bind(rfa_bit_stack *stack, rfa_u8 *storage, rfa_u32 capacity_bits);
int rfa_bit_stack_push(rfa_bit_stack *stack, rfa_u32 symbol, rfa_u32 width);
int rfa_bit_stack_pop(rfa_bit_stack *stack, rfa_u32 width, rfa_u32 *symbol);
#endif
