/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1 */
#ifndef RFA_ZIP32_CORE_H
#define RFA_ZIP32_CORE_H
#include "rfa_core_types.h"
typedef struct {
    const rfa_u8 *name;
    const rfa_u8 *data;
    rfa_u32 name_size;
    rfa_u32 data_size;
} rfa_zip32_entry;
/* Up to 32 entries, 8 MiB each, archive <=64 MiB. Borrowed immutable inputs.
 * All buffers must be disjoint. Failure leaves output and written unchanged.
 * No ZIP64/compression/encryption or filesystem API. Metadata matches RfaStoredZip.
 */
int rfa_zip32_store(const rfa_zip32_entry *entries,rfa_u32 count,
    rfa_u8 *output,rfa_u32 capacity,rfa_u32 *written);
#endif
