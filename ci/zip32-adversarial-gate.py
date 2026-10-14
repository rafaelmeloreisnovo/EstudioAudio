#!/usr/bin/env python3
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
"""Deterministic, synthetic ZIP32 falsifiers. Python is a host-only oracle."""
import ctypes as C
import io
import os
from pathlib import Path
import random
import struct
import subprocess
import tempfile
import zipfile
import zlib

if not __debug__:
    raise RuntimeError('Python -O disables safety assertions')
BASE = Path(__file__).resolve().parents[1] / 'app/src/main/cpp'
SEED = 20261014

class Entry(C.Structure):
    _fields_ = [('name', C.POINTER(C.c_uint8)), ('data', C.POINTER(C.c_uint8)),
                ('name_size', C.c_uint32), ('data_size', C.c_uint32)]

def run(*cmd):
    return subprocess.run([str(x) for x in cmd], check=True, text=True, capture_output=True).stdout

def load(folder, mutant=False):
    source = BASE / 'rfa_zip32_core.c'
    includes = BASE
    if mutant:
        includes = Path(folder) / 'mutant'
        includes.mkdir()
        for name in ('rfa_zip32_core.c', 'rfa_zip32_core.h', 'rfa_crc32_internal.h', 'rfa_core_types.h'):
            content = (BASE / name).read_text()
            if name == 'rfa_crc32_internal.h':
                assert content.count('0xedb88320u') == 1
                content = content.replace('0xedb88320u', '0xedb88321u')
            (includes / name).write_text(content)
        source = includes / 'rfa_zip32_core.c'
    target = Path(folder) / ('mutant.so' if mutant else 'core.so')
    run(os.environ.get('CC', 'clang'), '-std=c11', '-O2', '-ffreestanding',
        '-nostdinc', '-fno-builtin', '-fno-common', '-fno-stack-protector',
        '-fno-unwind-tables', '-fno-asynchronous-unwind-tables', '-fPIC',
        '-Wall', '-Wextra', '-Wshadow', '-Werror', '-nostdlib', '-nodefaultlibs',
        '-fuse-ld=lld', '-shared', '-I' + str(includes), source, '-o', target)
    assert not run('nm', '-u', target).strip()
    assert '(NEEDED)' not in run('readelf', '-d', target)
    lib = C.CDLL(str(target))
    lib.rfa_zip32_store.argtypes = [C.POINTER(Entry), C.c_uint32,
                                     C.POINTER(C.c_uint8), C.c_uint32,
                                     C.POINTER(C.c_uint32)]
    lib.rfa_zip32_store.restype = C.c_int
    return lib

def fixture(name, data, data_length=None):
    n = (C.c_uint8 * max(1, len(name)))()
    d = (C.c_uint8 * max(1, len(data)))()
    n[:len(name)] = name
    d[:len(data)] = data
    return Entry(n, d, len(name), len(data) if data_length is None else data_length), (n, d)

def oracle(records):
    local, central = bytearray(), bytearray()
    for name, payload in records:
        offset, crc, length = len(local), zlib.crc32(payload), len(payload)
        local += struct.pack('<IHHHHHIIIHH', 0x04034b50, 20, 0, 0, 0, 33,
                             crc, length, length, len(name), 0) + name + payload
        central += struct.pack('<IHHHHHHIIIHHHHHII', 0x02014b50, 20, 20,
                               0, 0, 0, 33, crc, length, length, len(name),
                               0, 0, 0, 0, 0, offset) + name
    start = len(local)
    return bytes(local + central + struct.pack('<IHHHHIIH', 0x06054b50, 0, 0,
                 len(records), len(records), len(central), start, 0))

def check(lib, records, success=True, capacity=None):
    held = [fixture(name, data) for name, data in records]
    entries = (Entry * len(held))(*(e for e, _ in held))
    expected = oracle(records) if success else b''
    cap = len(expected) if capacity is None and success else (4096 if capacity is None else capacity)
    output = (C.c_uint8 * (cap + 16))(*([0xa5] * (cap + 16)))
    prior = bytes(output)
    count = C.c_uint32(0xdeadbeef)
    result = lib.rfa_zip32_store(entries if held else None, len(held), output, cap, C.byref(count))
    if not success:
        assert result == 0 and count.value == 0xdeadbeef and bytes(output) == prior
        return
    assert result == 1 and count.value == len(expected)
    assert bytes(output[:count.value]) == expected and bytes(output[count.value:]) == prior[count.value:]
    with zipfile.ZipFile(io.BytesIO(expected)) as z:
        assert z.testzip() is None
        assert z.namelist() == [name.decode('ascii') for name, _ in records]
        for name, data in records:
            assert z.read(name.decode('ascii')) == data

def main():
    rng = random.Random(SEED)
    with tempfile.TemporaryDirectory() as folder:
        lib = load(folder)
        check(lib, [])
        check(lib, [(b'a', b'')])
        check(lib, [(f'max/{i:02d}'.encode(), bytes([i])) for i in range(32)])
        for case in range(96):
            records = []
            for i in range(rng.randint(1, 32)):
                size = rng.choice([0, 1, 2, 7, 31, 63, 64, 65, 255, 256, 257, 1023, 4096])
                records.append((f'case{case:03d}/{i:02d}'.encode(), rng.randbytes(size)))
            check(lib, records)
        check(lib, [(b'e/' + b'x' * 253, b'valid')])
        check(lib, [(b'boundary', bytes(8 * 1024 * 1024))])
        for path in (b'', b'/root', b'abc/', b'../x', b'a..b', b'a//b', b'a b',
                     b'a\\b', b'a\x00b', b'x' * 256):
            bad, keep = fixture(path, b'data')
            buffer = (C.c_uint8 * 512)(*([0xa5] * 512))
            written = C.c_uint32(777)
            assert lib.rfa_zip32_store(C.byref(bad), 1, buffer, 512, C.byref(written)) == 0
            assert written.value == 777 and bytes(buffer) == bytes([0xa5] * 512)
        check(lib, [(b'dup', b'1'), (b'dup', b'2')], success=False)
        check(lib, [(b'ok', b'too large')], success=False, capacity=20)
        bad, keep = fixture(b'valid', b'x', data_length=8 * 1024 * 1024 + 1)
        output = (C.c_uint8 * 128)(*([0xa5] * 128))
        written = C.c_uint32(777)
        assert lib.rfa_zip32_store(C.byref(bad), 1, output, 128, C.byref(written)) == 0
        for count in (33, 0xffffffff):
            assert lib.rfa_zip32_store(None, count, output, 128, C.byref(written)) == 0
        assert bytes(output) == bytes([0xa5] * 128) and written.value == 777
        mutant = load(folder, mutant=True)
        name, payload = b'mutation', b'123456789'
        entry, held = fixture(name, payload)
        expected = oracle([(name, payload)])
        buf = (C.c_uint8 * len(expected))()
        length = C.c_uint32()
        assert mutant.rfa_zip32_store(C.byref(entry), 1, buf, len(buf), C.byref(length)) == 1
        altered = bytes(buf)
        assert length.value == len(expected) and altered != expected
        with zipfile.ZipFile(io.BytesIO(altered)) as z:
            assert z.testzip() == 'mutation', 'CRC mutant escaped independent reader'
        print('ZIP32_FALSIFIERS=PASS random=96 seed=20261014')
        print('ZIP32_NEGATIVES=PASS count/path/capacity/length/duplicate/nonmutation')
        print('ZIP32_LIMITS=PASS exact8MiB/name255/entries32')
        print('ZIP32_ORACLE=PASS byte_layout+zlib_crc32+zipfile')
        print('ZIP32_MUTANT=KILLED polynomial_one_bit')
        print('APPLIED_TO_APP=NO; CI_FULL_APK=NOT_RUN; PHYSICAL=NOT_RUN')

if __name__ == '__main__':
    main()
