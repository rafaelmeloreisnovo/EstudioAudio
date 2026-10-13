/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
 */
#include "rfa_ptbr_core.h"
typedef struct { rfa_u32 chr, size, nasal; } rfa_letter;
static int rfa_decode(const rfa_u8 *s, rfa_u32 n, rfa_u32 at, rfa_letter *out) {
 rfa_u32 c, d, nasal=0U;
 if (at >= n) return 0;
 c=s[at];
 if (c == 195U) {
  if (at+1U>=n) return 0;
  d=s[at+1U];
  switch (d) {
   case 0x80U: case 0x81U: case 0x82U: case 0xA0U: case 0xA1U: case 0xA2U: c='a'; break;
   case 0x83U: case 0xA3U: c='a'; nasal=1U; break;
   case 0x88U: case 0x89U: case 0x8AU: case 0xA8U: case 0xA9U: case 0xAAU: c='e'; break;
   case 0x8DU: case 0xADU: c='i'; break;
   case 0x92U: case 0x93U: case 0x94U: case 0xB2U: case 0xB3U: case 0xB4U: c='o'; break;
   case 0x95U: case 0xB5U: c='o'; nasal=1U; break;
   case 0x9AU: case 0x9CU: case 0xBAU: case 0xBCU: c='u'; break;
   case 0x87U: case 0xA7U: c='s'; break;
   default: return 0;
  }
  out->size=2U;
 } else {
  if (c >= 'A' && c <= 'Z') c += 32U;
  if (!((c >= 'a' && c <= 'z') || c==' ' || c==9U ||
        c==10U || c==13U || c=='.' || c==',' || c==';' ||
        c=='!' || c=='?' || c==':' || c=='-' || c=='(' ||
        c==')' || c=='/')) return 0;
  out->size=1U;
 }
 out->chr=c; out->nasal=nasal;
 return 1;
}
static int rfa_is_vowel(rfa_u32 c) {
 return c=='a'||c=='e'||c=='i'||c=='o'||c=='u';
}
int rfa_ptbr_next(const rfa_u8 *utf8, rfa_u32 bytes,
                  rfa_u32 *cursor, rfa_ptbr_event *event) {
 rfa_letter first, second, third;
 rfa_u32 at, take, p=RFA_PT_PAUSE, frames=400U;
 int peek=0, next=0;
 if (!utf8 || !cursor || !event || bytes>2048U || *cursor>bytes) return -1;
 at=*cursor;
 if (at==bytes) return 0;
 if (!rfa_decode(utf8,bytes,at,&first)) return -1;
 take=first.size;
 if (at+take<bytes) peek=rfa_decode(utf8,bytes,at+take,&second);
 if (!peek && at+take<bytes) return -1;
 if (peek && at+take+second.size<bytes)
  next=rfa_decode(utf8,bytes,at+take+second.size,&third);
 if (!next && peek && at+take+second.size<bytes) return -1;
 if (first.chr=='a') { p=first.nasal?RFA_PT_AN:RFA_PT_A; frames=1050U; }
 else if (first.chr=='e') { p=RFA_PT_E; frames=940U; }
 else if (first.chr=='i') { p=RFA_PT_I; frames=880U; }
 else if (first.chr=='o') { p=first.nasal?RFA_PT_ON:RFA_PT_O; frames=1000U; }
 else if (first.chr=='u') { p=RFA_PT_U; frames=920U; }
 else if (first.chr=='s' || first.chr=='c' || first.chr=='z') { p=RFA_PT_S; frames=440U; }
 else if (first.chr=='x') { p=RFA_PT_SH; frames=520U; }
 else if (first.chr=='j') { p=RFA_PT_ZH; frames=560U; }
 else if (first.chr=='f') { p=RFA_PT_F; frames=430U; }
 else if (first.chr=='v') { p=RFA_PT_V; frames=480U; }
 else if (first.chr=='r') { p=RFA_PT_R; frames=400U; }
 else if (first.chr=='l') { p=RFA_PT_L; frames=470U; }
 else if (first.chr=='n' || first.chr=='m') { p=RFA_PT_N; frames=540U; }
 else if (first.chr>='a'&&first.chr<='z') {p=RFA_PT_STOP; frames=380U;}
 if (first.chr=='c' && peek && second.chr=='h') {p=RFA_PT_SH;frames=520U;take+=second.size;}
 else if (first.chr=='l' && peek && second.chr=='h') {p=RFA_PT_L;frames=620U;take+=second.size;}
 else if (first.chr=='n' && peek && second.chr=='h') {p=RFA_PT_N;frames=620U;take+=second.size;}
 else if (first.chr=='r' && peek && second.chr=='r') {p=RFA_PT_R;frames=530U;take+=second.size;}
 else if (first.chr=='s' && peek && second.chr=='s') {p=RFA_PT_S;take+=second.size;}
 else if (first.chr=='c' && (!peek || (second.chr!='e' && second.chr!='i'))) {p=RFA_PT_STOP;}
 else if (first.chr=='g' && peek && (second.chr=='e'||second.chr=='i')) {p=RFA_PT_ZH;frames=560U;}
 else if ((first.chr=='q'||first.chr=='g') && peek && second.chr=='u' &&
          next && (third.chr=='e'||third.chr=='i')) {
    p=RFA_PT_STOP;frames=380U;take+=second.size;
 }
 if (first.chr=='.'||first.chr=='!'||first.chr=='?') frames=1200U;
 if (first.chr==' '||first.chr==9U||first.chr==10U||first.chr==13U)
  frames=300U;
 event->start=at;
 event->end=at+take;
 event->phone=p;
 event->frames=frames;
 *cursor=at+take;
 return 1;
}
int rfa_ptbr_validate(const rfa_u8 *utf8, rfa_u32 bytes) {
 rfa_u32 at=0U, count=0U;
 rfa_ptbr_event event;
 int rc;
 if (!utf8 || !bytes || bytes>2048U) return 0;
 while (at<bytes) {
  rc=rfa_ptbr_next(utf8,bytes,&at,&event);
  if (rc!=1 || ++count>2048U) return 0;
 }
 return at==bytes;
}
void rfa_ptbr_reset(rfa_ptbr_state *state) {
 if (!state) return;
 state->index=0U;state->remaining=0U;state->phase=0U;
 state->noise=0x13579BDFU;state->phone=0U;state->step=0U;
}
static rfa_i32 rfa_tri(rfa_u32 phase) {
 rfa_i32 p=(rfa_i32)(phase>>16);
 return p<32768 ? p*2-32767 : 98303-p*2;
}
static rfa_i32 rfa_sample(rfa_ptbr_state *state) {
 rfa_i32 a,b,c,s=0;
 rfa_u32 p=state->phone;
 state->phase+=state->step;
 a=rfa_tri(state->phase);
 b=rfa_tri(state->phase*2U);
 c=rfa_tri(state->phase*3U);
 if (p>=RFA_PT_A && p<=RFA_PT_ON) {
   rfa_i32 w=(p==RFA_PT_A||p==RFA_PT_AN)?5:
             (p==RFA_PT_O||p==RFA_PT_ON)?4:2;
   s=(a*4+b*w+c*2)/24;
 } else if (p==RFA_PT_S||p==RFA_PT_SH||p==RFA_PT_F||p==RFA_PT_ZH||p==RFA_PT_V) {
   state->noise^=state->noise<<13;
   state->noise^=state->noise>>17;
   state->noise^=state->noise<<5;
   s=((rfa_i32)(state->noise>>16)-32768)/10;
   if (p==RFA_PT_ZH||p==RFA_PT_V) s+=a/15;
 } else if (p>=RFA_PT_STOP && p<=RFA_PT_N) s=a/12+b/30;
 if (state->remaining<80U) s=(s*(rfa_i32)state->remaining)/80;
 return s;
}
rfa_u32 rfa_ptbr_render(rfa_ptbr_state *state, const rfa_u8 *utf8,
                        rfa_u32 bytes, rfa_i16 *pcm, rfa_u32 capacity) {
 rfa_u32 used=0U;
 if (!state || !pcm || !capacity || capacity>4096U ||
     state->index>bytes || state->remaining>1200U ||
     state->phone>RFA_PT_N || !rfa_ptbr_validate(utf8,bytes)) return 0U;
 if (!state->noise) state->noise=0x13579BDFU;
 while (used<capacity) {
  rfa_ptbr_event next;
  if (!state->remaining) {
   rfa_u32 at=state->index;
   if (at==bytes) break;
   if (rfa_ptbr_next(utf8,bytes,&at,&next)!=1) return used;
   state->index=at;
   state->remaining=next.frames;
   state->phone=next.phone;
   state->step=32000000U+(next.phone%5U)*720000U;
  }
  pcm[used++]=(rfa_i16)rfa_sample(state);
  --state->remaining;
 }
 return used;
}
