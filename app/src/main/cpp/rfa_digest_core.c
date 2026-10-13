/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Port of project LowSha256/RfaStoredZip integer algorithms; standard SHA-256/CRC32.
 * Caller-owned mutable state; no platform/provider/native-library function.
 */
#include "rfa_digest_core.h"
#include "rfa_crc32_internal.h"
static const rfa_u32 rfa_sha256_k[64] = {0x428a2f98u,0x71374491u,0xb5c0fbcfu,0xe9b5dba5u,0x3956c25bu,0x59f111f1u,0x923f82a4u,0xab1c5ed5u,0xd807aa98u,0x12835b01u,0x243185beu,0x550c7dc3u,0x72be5d74u,0x80deb1feu,0x9bdc06a7u,0xc19bf174u,0xe49b69c1u,0xefbe4786u,0x0fc19dc6u,0x240ca1ccu,0x2de92c6fu,0x4a7484aau,0x5cb0a9dcu,0x76f988dau,0x983e5152u,0xa831c66du,0xb00327c8u,0xbf597fc7u,0xc6e00bf3u,0xd5a79147u,0x06ca6351u,0x14292967u,0x27b70a85u,0x2e1b2138u,0x4d2c6dfcu,0x53380d13u,0x650a7354u,0x766a0abbu,0x81c2c92eu,0x92722c85u,0xa2bfe8a1u,0xa81a664bu,0xc24b8b70u,0xc76c51a3u,0xd192e819u,0xd6990624u,0xf40e3585u,0x106aa070u,0x19a4c116u,0x1e376c08u,0x2748774cu,0x34b0bcb5u,0x391c0cb3u,0x4ed8aa4au,0x5b9cca4fu,0x682e6ff3u,0x748f82eeu,0x78a5636fu,0x84c87814u,0x8cc70208u,0x90befffau,0xa4506cebu,0xbef9a3f7u,0xc67178f2u};
static rfa_u32 rfa_rotr32(rfa_u32 x, rfa_u32 n) { return (x >> n) | (x << (32u-n)); }
static void rfa_sha256_transform(rfa_sha256 *s) {
    rfa_u32 w[64], a,b,c,d,e,f,g,h, i, t1,t2;
    for (i=0;i<16u;++i) {
        rfa_u32 p=i<<2;
        w[i]=((rfa_u32)s->block[p]<<24)|((rfa_u32)s->block[p+1u]<<16)|
             ((rfa_u32)s->block[p+2u]<<8)|(rfa_u32)s->block[p+3u];
    }
    for (i=16u;i<64u;++i) {
        rfa_u32 x=w[i-15u], y=w[i-2u];
        w[i]=w[i-16u]+(rfa_rotr32(x,7)^rfa_rotr32(x,18)^(x>>3))+w[i-7u]+
             (rfa_rotr32(y,17)^rfa_rotr32(y,19)^(y>>10));
    }
    a=s->h[0];b=s->h[1];c=s->h[2];d=s->h[3];e=s->h[4];f=s->h[5];g=s->h[6];h=s->h[7];
    for (i=0;i<64u;++i) {
        t1=h+(rfa_rotr32(e,6)^rfa_rotr32(e,11)^rfa_rotr32(e,25))+((e&f)^((~e)&g))+rfa_sha256_k[i]+w[i];
        t2=(rfa_rotr32(a,2)^rfa_rotr32(a,13)^rfa_rotr32(a,22))+((a&b)^(a&c)^(b&c));
        h=g;g=f;f=e;e=d+t1;d=c;c=b;b=a;a=t1+t2;
    }
    s->h[0]+=a;s->h[1]+=b;s->h[2]+=c;s->h[3]+=d;s->h[4]+=e;s->h[5]+=f;s->h[6]+=g;s->h[7]+=h;
}
void rfa_sha256_reset(rfa_sha256 *s) {
    rfa_u32 i;
    if (!s) return;
    s->h[0]=0x6a09e667u;s->h[1]=0xbb67ae85u;s->h[2]=0x3c6ef372u;s->h[3]=0xa54ff53au;
    s->h[4]=0x510e527fu;s->h[5]=0x9b05688cu;s->h[6]=0x1f83d9abu;s->h[7]=0x5be0cd19u;
    for (i=0;i<64u;++i) s->block[i]=0;
    s->bytes=0;s->used=0;s->finished=0;
}
int rfa_sha256_update(rfa_sha256 *s,const rfa_u8 *data,rfa_u32 count) {
    rfa_u32 i;
    if (!s || s->finished || s->used>=64u || (!data && count) ||
        s->bytes > 0x1fffffffffffffffULL ||
        (rfa_u64)count > 0x1fffffffffffffffULL-s->bytes) return 0;
    s->bytes+=count;
    for (i=0;i<count;++i) {
        s->block[s->used++]=data[i];
        if (s->used==64u) { rfa_sha256_transform(s);s->used=0; }
    }
    return 1;
}
int rfa_sha256_finish(rfa_sha256 *s,rfa_u8 digest[32]) {
    rfa_u32 i;
    rfa_u64 bits;
    if (!s || !digest || s->finished || s->used>=64u || s->bytes>0x1fffffffffffffffULL) return 0;
    bits=s->bytes<<3;
    s->block[s->used++]=0x80u;
    if (s->used>56u) {
        while (s->used<64u) s->block[s->used++]=0;
        rfa_sha256_transform(s);s->used=0;
    }
    while (s->used<56u) s->block[s->used++]=0;
    for (i=0;i<8u;++i) s->block[56u+i]=(rfa_u8)(bits>>((7u-i)<<3));
    rfa_sha256_transform(s);
    for (i=0;i<32u;++i) digest[i]=(rfa_u8)(s->h[i>>2]>>((3u-(i&3u))<<3));
    s->used=0;s->finished=1;
    return 1;
}
int rfa_crc32_update(rfa_u32 *accumulator,const rfa_u8 *data,rfa_u32 count) {
    if (!accumulator || (!data && count)) return 0;
    *accumulator=rfa_crc32_bytes(*accumulator,data,count);return 1;
}
