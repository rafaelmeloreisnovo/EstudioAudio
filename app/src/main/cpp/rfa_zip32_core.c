/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1 */
#include "rfa_zip32_core.h"
#include "rfa_crc32_internal.h"
static void rfa_zip16(rfa_u8 *p,rfa_u32 v){p[0]=(rfa_u8)v;p[1]=(rfa_u8)(v>>8);}
static void rfa_zip32(rfa_u8 *p,rfa_u32 v){rfa_zip16(p,v);rfa_zip16(p+2,v>>16);}
static int rfa_zip_path(const rfa_zip32_entry *e) {
    rfa_u32 j;
    if(!e->name || !e->name_size || e->name_size>255u || (!e->data && e->data_size) ||
       e->data_size>8388608u || e->name[0]=='/' || e->name[e->name_size-1u]=='/')return 0;
    for(j=0;j<e->name_size;++j){
        rfa_u8 c=e->name[j];
        if(!((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='_'||c=='-'||c=='.'||c=='/'))return 0;
        if(j && ((c=='.'&&e->name[j-1u]=='.')||(c=='/'&&e->name[j-1u]=='/')))return 0;
    }
    return 1;
}
int rfa_zip32_store(const rfa_zip32_entry *entries,rfa_u32 count,
    rfa_u8 *output,rfa_u32 capacity,rfa_u32 *written) {
    rfa_u32 i,j,k,at=0,start,size,crc[32],offset[32];rfa_u64 total=22u;
    if(count>32u || (!entries&&count) || !output || !written)return 0;
    for(i=0;i<count;++i){
        if(!rfa_zip_path(entries+i))return 0;
        for(j=0;j<i;++j){
            if(entries[i].name_size!=entries[j].name_size)continue;
            for(k=0;k<entries[i].name_size;++k)if(entries[i].name[k]!=entries[j].name[k])break;
            if(k==entries[i].name_size)return 0;
        }
        total+=76u+(rfa_u64)entries[i].name_size*2u+entries[i].data_size;
    }
    if(total>67108864u || total>capacity)return 0;
    for(i=0;i<count;++i){
        const rfa_zip32_entry *e=entries+i;
        offset[i]=at;crc[i]=~rfa_crc32_bytes(0xffffffffu,e->data,e->data_size);
        for(j=0;j<30u;++j)output[at+j]=0;
        rfa_zip32(output+at,0x04034b50u);rfa_zip16(output+at+4u,20u);rfa_zip16(output+at+12u,33u);
        rfa_zip32(output+at+14u,crc[i]);rfa_zip32(output+at+18u,e->data_size);rfa_zip32(output+at+22u,e->data_size);
        rfa_zip16(output+at+26u,e->name_size);at+=30u;
        for(j=0;j<e->name_size;++j)output[at++]=e->name[j];
        for(j=0;j<e->data_size;++j)output[at++]=e->data[j];
    }
    start=at;
    for(i=0;i<count;++i){
        const rfa_zip32_entry *e=entries+i;
        for(j=0;j<46u;++j)output[at+j]=0;
        rfa_zip32(output+at,0x02014b50u);rfa_zip16(output+at+4u,20u);rfa_zip16(output+at+6u,20u);
        rfa_zip16(output+at+14u,33u);rfa_zip32(output+at+16u,crc[i]);rfa_zip32(output+at+20u,e->data_size);
        rfa_zip32(output+at+24u,e->data_size);rfa_zip16(output+at+28u,e->name_size);rfa_zip32(output+at+42u,offset[i]);at+=46u;
        for(j=0;j<e->name_size;++j)output[at++]=e->name[j];
    }
    size=at-start;
    for(j=0;j<22u;++j)output[at+j]=0;
    rfa_zip32(output+at,0x06054b50u);rfa_zip16(output+at+8u,count);rfa_zip16(output+at+10u,count);
    rfa_zip32(output+at+12u,size);rfa_zip32(output+at+16u,start);at+=22u;
    *written=at;return 1;
}
