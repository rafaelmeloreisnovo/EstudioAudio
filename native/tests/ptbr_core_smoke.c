/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
 */
#include "rfa_ptbr_core.h"
int main(void) {
 const rfa_u8 text[]="chave, ninho, carro, queijo, ação!";
 const rfa_u8 utf8[]={'a',0xc3U,0xa7U,0xc3U,0xa3U,'o'};
 const rfa_u8 invalid[]={0xc3U};
 const rfa_u8 emoji[]={0xf0U,0x9fU,0x8eU,0x99U};
 rfa_ptbr_event e={0U,0U,0U,0U};
 rfa_ptbr_state whole,chunks;
 rfa_i16 one[4096],split[4096],guard[2]={111,222};
 rfa_u32 at=0U,i,a,b,total=0U,active=0U;
 if (rfa_ptbr_next(text,(rfa_u32)sizeof(text)-1U,&at,&e)!=1 ||
     e.phone!=RFA_PT_SH || at!=2U || e.start!=0U || e.end!=2U) return 1;
 if (!rfa_ptbr_validate(text,(rfa_u32)sizeof(text)-1U)) return 2;
 if (!rfa_ptbr_validate(utf8,6U)) return 3;
 if (rfa_ptbr_validate(invalid,1U) || rfa_ptbr_validate(emoji,4U)) return 4;
 at=0U;e.phone=777U;
 if (rfa_ptbr_next(invalid,1U,&at,&e)!=-1 || at || e.phone!=777U) return 5;
 rfa_ptbr_reset(&whole);rfa_ptbr_reset(&chunks);
 a=rfa_ptbr_render(&whole,text,(rfa_u32)sizeof(text)-1U,one,4096U);
 b=rfa_ptbr_render(&chunks,text,(rfa_u32)sizeof(text)-1U,split,257U);
 b+=rfa_ptbr_render(&chunks,text,(rfa_u32)sizeof(text)-1U,split+b,4096U-b);
 if (a!=4096U || a!=b) return 6;
 for (i=0U;i<a;++i) {
   if (one[i]!=split[i]) return 7;
   if (one[i]) ++active;
   if (one[i]>22000 || one[i]<-22000) return 8;
 }
 if (active<100U) return 9;
 if (whole.index!=chunks.index || whole.phase!=chunks.phase ||
     whole.remaining!=chunks.remaining || whole.noise!=chunks.noise) return 10;
 while (total<262144U) {
  rfa_u32 n=rfa_ptbr_render(&whole,text,(rfa_u32)sizeof(text)-1U,one,4096U);
  if (!n) break;
  total+=n;
 }
 if (total>=262144U || whole.index!=(rfa_u32)sizeof(text)-1U) return 11;
 if (rfa_ptbr_render(&whole,invalid,1U,guard,2U) || guard[0]!=111 || guard[1]!=222) return 12;
 whole.remaining=1201U;
 if (rfa_ptbr_render(&whole,utf8,6U,guard,2U) || guard[0]!=111 || guard[1]!=222) return 13;
 rfa_ptbr_reset(&whole);
 if (rfa_ptbr_render(&whole,emoji,4U,guard,2U) || whole.index) return 14;
 return 0;
}
