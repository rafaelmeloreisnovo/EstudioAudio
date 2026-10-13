/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
#include "rfa_timbre_core.h"
int main(void) {
    rfa_i16 pcm[4096],a[4096],b[4096],s[4096];
    rfa_timbre_stats one,two,quiet;
    rfa_timbre_profile p={0U,0U,0U},q={0U,0U,0U};
    rfa_timbre_filter f1={0},f2={0};
    rfa_u32 i;
    for(i=0U;i<4096U;++i) {
        pcm[i]=(i&1U)?1400:-1400;
        a[i]=pcm[i]; b[i]=pcm[i];s[i]=0;
    }
    rfa_timbre_reset(&one);rfa_timbre_reset(&two);rfa_timbre_reset(&quiet);
    if(rfa_timbre_finalize(&one,&p)) return 1;
    if(!rfa_timbre_accumulate(&one,pcm,4096U) ||
       !rfa_timbre_accumulate(&one,pcm,4096U)) return 2;
    for(i=0U;i<8192U/256U;++i)
        if(!rfa_timbre_accumulate(&two,pcm+(i%16U)*256U,256U)) return 3;
    if(!rfa_timbre_finalize(&one,&p) || !rfa_timbre_finalize(&two,&q) ||
       p.mix_q8!=q.mix_q8 || p.mean_abs!=q.mean_abs ||
       p.zero_cross_k!=q.zero_cross_k) return 4;
    if(p.mix_q8<64U || p.mix_q8>240U) return 5;
    if(!rfa_timbre_accumulate(&quiet,s,4096U) ||
       !rfa_timbre_accumulate(&quiet,s,4096U) ||
       rfa_timbre_finalize(&quiet,&q)) return 6;
    if(!rfa_timbre_apply(&f1,&p,a,4096U) ||
       !rfa_timbre_apply(&f2,&p,b,256U) ||
       !rfa_timbre_apply(&f2,&p,b+256U,3840U)) return 7;
    for(i=0U;i<4096U;++i) if(a[i]!=b[i]) return 8;
    p.mix_q8=300U; a[0]=123;
    if(rfa_timbre_apply(&f1,&p,a,4096U) || a[0]!=123) return 9;
    if(rfa_timbre_accumulate(&one,pcm,4097U)) return 10;
    return 0;
}
