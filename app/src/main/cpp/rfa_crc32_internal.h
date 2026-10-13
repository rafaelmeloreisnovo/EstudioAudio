/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1 */
#ifndef RFA_CRC32_INTERNAL_H
#define RFA_CRC32_INTERNAL_H
#include "rfa_core_types.h"
static inline rfa_u32 rfa_crc32_bytes(rfa_u32 c,const rfa_u8 *p,rfa_u32 n) {
    rfa_u32 i,k;
    for(i=0;i<n;++i) {c^=p[i];for(k=0;k<8u;++k)c=(c>>1)^(0xedb88320u&(0u-(c&1u)));}
    return c;
}
#endif
