/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1 */
#include "rfa_digest_core.h"
#include "rfa_zip32_core.h"
#include "rfa_lms_core.h"
int main(void){
    rfa_sha256 sha;
    rfa_u8 digest[32],buffer[1024];
    const rfa_u8 abc[3]={'a','b','c'};
    const rfa_u8 expected[32]={0xba,0x78,0x16,0xbf,0x8f,0x01,0xcf,0xea,0x41,0x41,0x40,0xde,0x5d,0xae,0x22,0x23,0xb0,0x03,0x61,0xa3,0x96,0x17,0x7a,0x9c,0xb4,0x10,0xff,0x61,0xf2,0x00,0x15,0xad};
    rfa_i16 w[1],h[1],p[4096],r[4096],out[4096];
    rfa_nlms_q15 s;rfa_nlms_trial trial;
    rfa_zip32_entry entry={abc,abc,3,3};
    rfa_u32 written=777,i,crc=0xffffffffu;
    rfa_sha256_reset(&sha);
    if(!rfa_sha256_update(&sha,abc,3)||!rfa_sha256_finish(&sha,digest))return 1;
    for(i=0;i<32u;++i)if(digest[i]!=expected[i])return 2;
    if(rfa_sha256_finish(&sha,digest)||rfa_sha256_update(&sha,abc,3))return 3;
    if(!rfa_crc32_update(&crc,(const rfa_u8*)"123456789",9)||~crc!=0xcbf43926u)return 4;
    if(!rfa_zip32_store(&entry,1,buffer,1024,&written))return 5;
    written=777;buffer[0]=0xa5;
    if(rfa_zip32_store(&entry,1,buffer,1,&written)||written!=777||buffer[0]!=0xa5)return 6;
    if(!rfa_nlms_bind_q15(&s,w,h,1,32767,1))return 7;
    for(i=0;i<4096u;++i)p[i]=r[i]=(rfa_i16)((i&1u)?10000:-10000);
    if(rfa_nlms_trial_block_q15(&s,p,r,out,4096,1,&trial)!=RFA_NLMS_ACCEPT_ADAPTED)return 8;
    if(trial.candidate_energy>trial.baseline_energy)return 9;
    s.position=1;out[0]=321;
    if(rfa_nlms_trial_block_q15(&s,p,r,out,1,1,&trial)!=0||out[0]!=321||s.position!=1)return 10;
    s.position=0;
    if(rfa_nlms_trial_block_q15(&s,p,r,out,4097,1,&trial)!=0)return 11;
    return 0;
}
