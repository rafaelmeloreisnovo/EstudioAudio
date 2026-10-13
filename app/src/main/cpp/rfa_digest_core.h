/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1 */
#ifndef RFA_DIGEST_CORE_H
#define RFA_DIGEST_CORE_H
#include "rfa_core_types.h"
typedef struct {
    rfa_u32 h[8];
    rfa_u8 block[64];
    rfa_u64 bytes;
    rfa_u32 used;
    rfa_u32 finished;
} rfa_sha256;
void rfa_sha256_reset(rfa_sha256 *s);
int rfa_sha256_update(rfa_sha256 *s, const rfa_u8 *data, rfa_u32 count);
int rfa_sha256_finish(rfa_sha256 *s, rfa_u8 digest[32]);
/* Initial accumulator = 0xffffffff, final value = ~accumulator. */
int rfa_crc32_update(rfa_u32 *accumulator, const rfa_u8 *data, rfa_u32 count);
#endif
