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
  document, crypto: webcrypto, TextEncoder, TextDecoder, Uint8Array, DataView,
  Float32Array, ArrayBuffer, Math, Date, Number, Set, Object, Error, String,
  Boolean, RegExp, JSON, Map, Promise, URL, Blob, navigator: {},
  Audio: class {}, setTimeout, clearTimeout, btoa, atob
};
vm.createContext(sandbox);
const html = fs.readFileSync(new URL('./###_AUDIO_CUSTODY70_V1.html', import.meta.url), 'utf8');
vm.runInContext(html.split('<script>')[1].split('</script>')[0], sandbox);
const core = vm.runInContext('({wav,checkWav,derivative,pcmFromFloat,sha,zipRead})', sandbox);
const samples = new Float32Array(16000);
for (let i = 0; i < samples.length; i++) samples[i] = .25 * Math.sin(2 * Math.PI * 260 * i / 8000);
const source = core.wav(core.pcmFromFloat(samples, 8000));
assert.equal(source.length, 16044);
core.checkWav(source);
for (const effect of ['rain', 'falls', 'crackle']) {
  const output = core.derivative(source, effect);
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
