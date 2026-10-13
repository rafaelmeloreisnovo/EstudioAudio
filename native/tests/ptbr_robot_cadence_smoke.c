/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Scoped deterministic robotic-cadence vectors, NOT human intelligibility.
 */
#include "rfa_ptbr_core.h"
int main(void) {
 const rfa_u8 text[]="A, E. I?";
 const rfa_u8 invalid[]={0xc3U};
 const rfa_u32 bytes=(rfa_u32)sizeof(text)-1U;
 rfa_ptbr_robot_state whole,chunked;
 rfa_ptbr_event e;
 rfa_i16 a[20000],b[20000],guard[2]={123,456};
 rfa_u32 first=0U,second=0U,cursor=0U,base=0U,active=0U,i;
 rfa_ptbr_robot_reset(&whole);
 rfa_ptbr_robot_reset(&chunked);
 while (cursor<bytes) {
  rfa_u32 previous=cursor;
  if (rfa_ptbr_next(text,bytes,&cursor,&e)!=1 ||
      cursor<=previous || e.frames==0U) return 1;
  base+=e.frames;
 }
 while (first<20000U) {
  rfa_u32 capacity=1024U,got;
  if (capacity>20000U-first) capacity=20000U-first;
  got=rfa_ptbr_robot_render(&whole,text,bytes,a+first,capacity);
  if (!got) break;
  first+=got;
 }
 while (second<20000U) {
  rfa_u32 capacity=37U,got;
  if (capacity>20000U-second) capacity=20000U-second;
  got=rfa_ptbr_robot_render(&chunked,text,bytes,b+second,capacity);
  if (!got) break;
  second+=got;
 }
 /* Explicit robotic punctuation silence: , 960 + . 2400 + ? 2400.
  * Underlying legacy glyph durations are retained. */
 if (first!=base+5760U || first!=second || first>=20000U) return 2;
 for (i=0U;i<first;++i) {
  if (a[i]!=b[i]) return 3;
  if (a[i]) ++active;
  if (a[i]>22000 || a[i]<-22000) return 4;
 }
 if (active<200U) return 5;
 if (whole.voice.index!=bytes || chunked.voice.index!=bytes ||
     whole.voice.remaining || chunked.voice.remaining ||
     whole.silence || chunked.silence ||
     whole.pending || chunked.pending ||
     whole.smooth!=chunked.smooth ||
     whole.voice.noise!=chunked.voice.noise) return 6;
 if (rfa_ptbr_robot_render(&whole,text,bytes,guard,2U)!=0U) return 7;
 rfa_ptbr_robot_reset(&whole);
 if (rfa_ptbr_robot_render(&whole,invalid,1U,guard,2U)!=0U ||
     guard[0]!=123 || guard[1]!=456 || whole.voice.index!=0U) return 8;
 whole.silence=2401U;
 if (rfa_ptbr_robot_render(&whole,text,bytes,guard,2U)!=0U ||
     guard[0]!=123 || guard[1]!=456) return 9;
 rfa_ptbr_robot_reset(&whole);
 whole.smooth=32768;
 if (rfa_ptbr_robot_render(&whole,text,bytes,guard,2U)!=0U ||
     guard[0]!=123 || guard[1]!=456) return 10;
 rfa_ptbr_robot_reset(&whole);
 if (rfa_ptbr_robot_render(&whole,text,bytes,guard,4097U)!=0U ||
     guard[0]!=123 || guard[1]!=456) return 11;
 return 0;
}
