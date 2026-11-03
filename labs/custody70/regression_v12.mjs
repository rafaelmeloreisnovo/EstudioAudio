// SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
// Isolated browser-code test: node regression.mjs --strict
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import {webcrypto} from 'node:crypto';

const strict=process.argv.includes('--strict');
const els=new Map();
const clock=class Clock extends Date {static offset=0; static now(){return Date.now()+Clock.offset}};
const audioStats={plays:0,created:0,revoked:0,reject:false,createdUrls:[],revokedUrls:[]};
class FakeAudio {constructor(url){this.url=url} play(){audioStats.plays++;return audioStats.reject?Promise.reject(new Error('browser denied')):Promise.resolve()}}
const FakeURL={createObjectURL(){audioStats.created++;const url='blob:test-'+audioStats.created;audioStats.createdUrls.push(url);return url},revokeObjectURL(url){audioStats.revoked++;audioStats.revokedUrls.push(url)}};
const document={getElementById(id){if(!els.has(id))els.set(id,{
  value:id==='password'?'DemoPass!2026':id==='validity'?'15':id==='effect'?'rain':'',
  disabled:false,textContent:'',getContext(){return {clearRect(){},beginPath(){},moveTo(){},lineTo(){},stroke(){},fillRect(){}}}
});return els.get(id)}};
const env={document,crypto:webcrypto,TextEncoder,TextDecoder,Uint8Array,Uint32Array,DataView,Float32Array,ArrayBuffer,Math,Date:clock,Number,Set,Object,Error,String,Boolean,RegExp,JSON,Map,Promise,URL:FakeURL,Blob,navigator:{},Audio:FakeAudio,setTimeout,clearTimeout,btoa,atob,console};
vm.createContext(env);
const html=fs.readFileSync(new URL('./%23%23%23_AUDIO_CUSTODY70_V1_2.html',import.meta.url),'utf8');
vm.runInContext(html.split('<script>')[1].split('</script>')[0],env);
const api=vm.runInContext('({zipRead,zipWrite,sha,verifyPublic,checkWav,audio,draw})',env);
vm.runInContext('download = (data) => { globalThis.capturedZip = data }',env);
const el=id=>document.getElementById(id), result=[];
const check=(name,ok,detail='')=>{result.push({name,ok,detail});console.log(`${ok?'PASS':'FAIL'} ${name}${detail?' :: '+detail:''}`)};
const copyFile=bytes=>({size:bytes.length,arrayBuffer:async()=>bytes.buffer.slice(bytes.byteOffset,bytes.byteOffset+bytes.length)});
const load=async bytes=>{let input={files:[copyFile(bytes)],value:'C:\\fakepath\\same.zip'};await el('zipInput').onchange({target:input});return input};
const verify=async()=>{await el('verify').onclick();return el('receipt').textContent};
const denyMsg=(msg)=>el('receipt').textContent.includes(msg);

el('synthetic').onclick(); await el('export').onclick();const original=env.capturedZip;
assert(original instanceof Uint8Array);assert(original.length<=71680);
check('within_70KiB',original.length<=71680,`bytes=${original.length}`);
let input=await load(original);await el('verify').onclick();await el('unlock').onclick();
check('baseline_roundtrip',denyMsg('OWNER_AUTHENTICATED_BY_SHARED_PASSWORD'));
check('same_file_input_reset',input.value==='','input.value='+JSON.stringify(input.value));
let second=await load(original);await el('verify').onclick();await el('unlock').onclick();check('same_file_double_import',second.value===''&&denyMsg('OWNER_AUTHENTICATED_BY_SHARED_PASSWORD'));
const local=api.zipRead(original);

// 1. Expiry must gate both buttons in LOCAL exported (raw and unlocked) and imported sessions.
el('synthetic').onclick();await el('export').onclick();
clock.offset=16*60000;
let before=audioStats.plays;
await el('listenDemo').onclick(); const demoPlayed=audioStats.plays>before;
before=audioStats.plays;await el('listenOriginal').onclick(); const ownerPlayed=audioStats.plays>before;
check('expiry_both_local_playbacks',!demoPlayed&&!ownerPlayed,`demo_play=${demoPlayed},owner_play=${ownerPlayed}`);
clock.offset=0;
await load(original);await verify();await el('unlock').onclick();
clock.offset=16*60000;before=audioStats.plays;await el('listenDemo').onclick();await el('listenOriginal').onclick();
check('expiry_both_imported_playbacks',before===audioStats.plays);clock.offset=0;

// 2. Data mutations with recalculated ZIP CRC still fail SHA; guest digest cannot authenticate a malicious rebuild.
let altered=Object.fromEntries(Object.entries(local).map(([n,data])=>[n,data.slice()]));
altered['demo.wav'][70]^=1;
let z=api.zipWrite(Object.entries(altered)); await load(z);let receipt=await verify();
check('demo_payload_crc_recalculated_sha_reject',receipt.includes('VERIFY_FAIL'));
altered=Object.fromEntries(Object.entries(local).map(([n,data])=>[n,data.slice()]));
altered['original.enc'][150]^=1;z=api.zipWrite(Object.entries(altered));
await load(z);receipt=await verify();
check('cipher_payload_crc_recalculated_sha_reject',receipt.includes('VERIFY_FAIL'));

// 2b. A malicious guest can rebuild public SHA without the owner's HMAC.
altered=Object.fromEntries(Object.entries(local).map(([n,data])=>[n,data.slice()]));
altered['demo.wav'][90]^=1;
let forged=JSON.parse(new TextDecoder().decode(altered['manifest.json']));
forged.demo_sha256=await api.sha(altered['demo.wav']);
forged.chain_sha256=await api.sha(new TextEncoder().encode('RAFCODE-CUSTODY70-V1.2|'+forged.source_sha256+'|'+forged.demo_sha256+'|'+forged.created_utc+'|'+forged.expires_utc));
altered['manifest.json']=new TextEncoder().encode(JSON.stringify(forged));z=api.zipWrite(Object.entries(altered));
await load(z);receipt=await verify();
check('forged_public_hash_not_authenticity',receipt.includes('PUBLIC_DIGESTS_MATCH_NOT_AUTHENTICATED'));
el('password').value='DemoPass!2026';await el('unlock').onclick();
check('owner_hmac_rejects_forged_public_hash',denyMsg('UNLOCK_FAIL'));

// V1.2 must reject a V1.1 wire schema, never silently mix derived-noise algorithms.
{
  const oldEntries=Object.fromEntries(Object.entries(local).map(([name,data])=>[name,data.slice()]));
  const oldManifest=JSON.parse(new TextDecoder().decode(oldEntries['manifest.json']));
  oldManifest.schema='rafe.audio-custody70.v1';
  oldEntries['manifest.json']=new TextEncoder().encode(JSON.stringify(oldManifest));
  const oldZip=api.zipWrite(Object.entries(oldEntries));
  await load(oldZip);
  const receipt=await verify();
  check('v1_1_wire_schema_rejected',receipt.includes('VERIFY_FAIL'));
}

// 3. Public schema is closed in this tiny version; strict rejects changes, before owner HMAC.
altered=Object.fromEntries(Object.entries(local).map(([n,data])=>[n,data.slice()]));
const extra=JSON.parse(new TextDecoder().decode(altered['manifest.json']));extra.x=1;altered['manifest.json']=new TextEncoder().encode(JSON.stringify(extra));
z=api.zipWrite(Object.entries(altered));await load(z);receipt=await verify();
check('closed_manifest_extra_field_rejected',receipt.includes('VERIFY_FAIL'));

// 4. Targeted bitflip sweep of ZIP metadata, grouped by field; NO CRC update.
const metaFields=[
  ['local.extract_version',4,2],['local.DOS_time',10,2],['local.DOS_date',12,2],
  ['central.made_by',-1,2],['central.extract_version',-2,2],
  ['central.DOS_time',-3,2],['central.DOS_date',-4,2],
  ['central.disk_start',-5,2],['central.internal_attributes',-6,2],
  ['central.external_attributes',-7,4],['EOCD.disk',-8,2],['EOCD.central_disk',-9,2]
];
const cv=new DataView(original.buffer,original.byteOffset,original.byteLength);
const central=cv.getUint32(original.length-22+16,true);
const lookup=new Map([[-1,central+4],[-2,central+6],[-3,central+12],[-4,central+14],[-5,central+34],[-6,central+36],[-7,central+38],[-8,original.length-22+4],[-9,original.length-22+6]]);
let rejectedBits=0,allBits=0;
for(const [field,at,size] of metaFields){
  const offset=at<0?lookup.get(at):at;let rejects=0;
  for(let i=0;i<size;i++)for(let bit=0;bit<8;bit++){
    const flipped=original.slice();flipped[offset+i]^=1<<bit;allBits++;
    try{api.zipRead(flipped)}catch{rejects++;rejectedBits++}
  }
  check('canonical_zip_rejects_'+field,rejects===size*8,`${rejects}/${size*8} bits`);
}
check('canonical_metadata_bitflip_total',rejectedBits===allBits,`${rejectedBits}/${allBits} bits`);
// CRC itself is not an exception when the mutator does not overwrite it.
for(const [n,off] of [['crc_local',14],['crc_central',central+16]]){
  let rejects=0;for(let bit=0;bit<32;bit++){
    const bytes=original.slice();bytes[off+(bit>>3)]^=1<<(bit&7);
    try{api.zipRead(bytes)}catch{rejects++}
  }
  check(n+'_bitflip_rejected',rejects===32,`${rejects}/32`);
}

// 5. audio.play() reject: object URL must be revoked without onended.
audioStats.reject=true;let created=audioStats.created,revoked=audioStats.revoked;
api.audio(local['demo.wav']);await Promise.resolve();await Promise.resolve();
check('play_rejection_revokes_url',audioStats.created===created+1&&audioStats.revokedUrls.includes(audioStats.createdUrls.at(-1)),`new_url=${audioStats.createdUrls.at(-1)} revoked=${audioStats.revokedUrls.includes(audioStats.createdUrls.at(-1))}`);
audioStats.reject=false;

// 6. Strict base64 rejects missing canonical padding. It used to decode permissively.
altered=Object.fromEntries(Object.entries(local).map(([n,data])=>[n,data.slice()]));
let manifest=JSON.parse(new TextDecoder().decode(altered['manifest.json']));manifest.crypto.salt=manifest.crypto.salt.replace(/=+$/,'');
altered['manifest.json']=new TextEncoder().encode(JSON.stringify(manifest));
z=api.zipWrite(Object.entries(altered));await load(z);receipt=await verify();
check('noncanonical_base64_rejected',receipt.includes('VERIFY_FAIL'));

// 7. Graphical mismatch is rejected instead of plotting incompatible lengths.
vm.runInContext('raw=wav(new Uint8Array(1000));demo=wav(new Uint8Array(900));draw()',env);
check('draw_length_mismatch_visible',el('counters').textContent==='WAVE_LENGTH_MISMATCH');

// Existing behavior: wrong password cannot open, max still enforced.
await load(original); await verify();el('password').value='invalid-but-long-enough';await el('unlock').onclick();
check('wrong_password_rejected',denyMsg('UNLOCK_FAIL'));
const oversize=new Uint8Array(71681);let limitRejected=false;try{api.zipRead(oversize)}catch{limitRejected=true}
check('oversize_rejected',limitRejected);

console.log(`SUMMARY ${result.filter(r=>r.ok).length}/${result.length}`);
if(strict) assert.equal(result.filter(r=>!r.ok).length,0,'regressions remain');
