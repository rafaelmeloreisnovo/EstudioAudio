// SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
// Run in labs/custody70: node test.mjs (no npm dependencies).
import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
import { webcrypto } from 'node:crypto';

const elements = new Map();
const document = {
  getElementById(id) {
    if (!elements.has(id)) elements.set(id, {
      value: id === 'password' ? 'DemoPass!2026' : id === 'validity' ? '1440' : id === 'effect' ? 'rain' : '',
      textContent: '', disabled: false,
      getContext: () => ({
        clearRect() {}, beginPath() {}, moveTo() {},
        lineTo() {}, stroke() {}, fillRect() {}
      })
    });
    return elements.get(id);
  }
};
const sandbox = {
  document, crypto: webcrypto, TextEncoder, TextDecoder, Uint8Array, Uint32Array, DataView,
  Float32Array, ArrayBuffer, Math, Date, Number, Set, Object, Error, String,
  Boolean, RegExp, JSON, Map, Promise, URL, Blob, navigator: {},
  Audio: class {}, setTimeout, clearTimeout, btoa, atob
};
vm.createContext(sandbox);
const html = fs.readFileSync(new URL('./%23%23%23_AUDIO_CUSTODY70_V1_2.html', import.meta.url), 'utf8');
vm.runInContext(html.split('<script>')[1].split('</script>')[0], sandbox);
const core = vm.runInContext('({wav,checkWav,derivative,pcmFromFloat,sha,zipRead,keys,utf})', sandbox);
const samples = new Float32Array(16000);
for (let i = 0; i < samples.length; i++) samples[i] = .25 * Math.sin(2 * Math.PI * 260 * i / 8000);
const source = core.wav(core.pcmFromFloat(samples, 8000));
assert.equal(source.length, 16044);
core.checkWav(source);
for (const effect of ['rain', 'falls', 'crackle']) {
  const output = core.derivative(source, effect, 1);
  core.checkWav(output);
  assert.notEqual(await core.sha(output), await core.sha(source));
}
vm.runInContext('download = (bytes) => { globalThis.capturedZip = bytes }', sandbox);
document.getElementById('synthetic').onclick();
await document.getElementById('export').onclick();
const zip = sandbox.capturedZip;
assert.ok(zip && zip.length < 70 * 1024);
assert.equal(Object.keys(core.zipRead(zip)).length, 4);
const file = { size: zip.length, arrayBuffer: async () => zip.buffer.slice(zip.byteOffset, zip.byteOffset + zip.length) };
await document.getElementById('zipInput').onchange({ target: { files: [file] } });
await document.getElementById('verify').onclick();
assert.match(document.getElementById('receipt').textContent, /PUBLIC_DIGESTS_MATCH_NOT_AUTHENTICATED/);
await document.getElementById('unlock').onclick();
assert.match(document.getElementById('receipt').textContent, /OWNER_AUTHENTICATED_BY_SHARED_PASSWORD/);
document.getElementById('password').value = 'WrongPassword';
await document.getElementById('unlock').onclick();
assert.match(document.getElementById('receipt').textContent, /UNLOCK_FAIL/);
const altered = zip.slice(); altered[100] ^= 1;
assert.throws(() => core.zipRead(altered));
console.log('PASS scoped: 8-bit mono 8 kHz WAV, three noise modes, ZIP under 70 KiB, import, SHA, owner HMAC/AES, wrong-pass and tamper reject');
console.log('sample_zip_bytes=' + zip.length);

// V1.2 specific: seed requires password, is normalized to unsigned uint32;
// the known synthetic source demonstrates that seed-obfuscation is not secrecy.
const pack = core.zipRead(zip);
const m = JSON.parse(new TextDecoder().decode(pack['manifest.json']));
assert.equal(m.schema, 'rafe.audio-custody70.v1.2');
const salted = Uint8Array.from(Buffer.from(m.crypto.salt,'base64'));
const ownerKeys = await core.keys('DemoPass!2026', salted);
assert(Number.isInteger(ownerKeys.seed) && ownerKeys.seed >= 0 && ownerKeys.seed <= 0xffffffff);
assert.throws(() => core.derivative(source,'rain',undefined), /SEED_REQUIRED/);
const knownFloat = new Float32Array(16000);
for(let i=0;i<16000;i++)knownFloat[i]=.24*Math.sin(2*Math.PI*(i<8000?260:390)*i/8000);
const knownWav = core.wav(core.pcmFromFloat(knownFloat,8000));
const reDerived = core.derivative(knownWav,m.effect,ownerKeys.seed);
assert.equal(await core.sha(reDerived), m.demo_sha256, 'V1.2 password seed must reconstruct demo');
const original = new Uint8Array(await webcrypto.subtle.decrypt({
  name:'AES-GCM',iv:Buffer.from(m.crypto.iv,'base64'),
  additionalData:core.utf('C70-V1.2:'+m.demo_sha256)
},ownerKeys.aes,pack['original.enc']));
assert.deepEqual(original, knownWav);
console.log('PASS: V1.2 seed verified, explicit seed enforced, known synthetic source 16000/16000 samples reproduced without password-derived estimate');
console.log('SECURITY_CAVEAT: public known synthetic tone is 100% reproducible; noise is not encryption or DRM');
