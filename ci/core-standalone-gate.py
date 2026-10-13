#!/usr/bin/env python3
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
"""No Gradle/JDK/SDK/NDK/Android. Hosted stdlib oracle tests a freestanding ELF core."""
import ctypes as C
import hashlib
import os
from pathlib import Path
import random
import subprocess as S
import tempfile
import zlib
import zipfile
import io
import struct
ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
CC = os.environ.get('CC', 'clang')
FLAGS = ['-std=c11', '-O2', '-nostdinc', '-ffreestanding', '-fno-builtin',
         '-fno-common', '-fno-stack-protector', '-fno-unwind-tables',
         '-fno-asynchronous-unwind-tables', '-fno-optimize-sibling-calls',
         '-Wall', '-Wextra', '-Wshadow', '-Werror', '-Iapp/src/main/cpp']
class SHA(C.Structure):
    _fields_ = [('h',C.c_uint32*8),('block',C.c_uint8*64),('bytes',C.c_uint64),
                ('used',C.c_uint32),('finished',C.c_uint32)]
class NLMS(C.Structure):
    _fields_ = [('weights',C.POINTER(C.c_int16)),('history',C.POINTER(C.c_int16)),
                ('taps',C.c_int),('position',C.c_int),('mu',C.c_int32),('epsilon',C.c_uint64)]
class Trial(C.Structure):
    _fields_ = [('weights',C.c_int16*256),('history',C.c_int16*256),
                ('baseline',C.c_int16*4096),('baseline_energy',C.c_uint64),('candidate_energy',C.c_uint64)]
def run(*args):
    return S.check_output(list(args),text=True)
def digest_tests(lib):
    lib.rfa_sha256_update.argtypes=[C.POINTER(SHA),C.POINTER(C.c_uint8),C.c_uint32]
    lib.rfa_sha256_reset.argtypes=[C.POINTER(SHA)]
    lib.rfa_sha256_finish.argtypes=[C.POINTER(SHA),C.POINTER(C.c_uint8)]
    lib.rfa_crc32_update.argtypes=[C.POINTER(C.c_uint32),C.POINTER(C.c_uint8),C.c_uint32]
    vectors=0
    for length in list(range(0,130))+[255,256,4096,65536]:
        data=bytes((i*73+19)&255 for i in range(length))
        for step in [1,63,64,65,4096]:
            s=SHA();lib.rfa_sha256_reset(C.byref(s));crc=C.c_uint32(0xffffffff)
            for i in range(0,length,step):
                part=data[i:i+step];buf=(C.c_uint8*len(part)).from_buffer_copy(part)
                assert lib.rfa_sha256_update(C.byref(s),buf,len(part))==1
                assert lib.rfa_crc32_update(C.byref(crc),buf,len(part))==1
            out=(C.c_uint8*32)()
            assert lib.rfa_sha256_finish(C.byref(s),out)==1
            assert bytes(out)==hashlib.sha256(data).digest()
            assert (~crc.value)&0xffffffff==zlib.crc32(data)
            before=bytes(s)
            assert lib.rfa_sha256_finish(C.byref(s),out)==0
            assert lib.rfa_sha256_update(C.byref(s),None,0)==0
            assert bytes(s)==before
            vectors+=1
    s=SHA();lib.rfa_sha256_reset(C.byref(s));s.used=64
    assert lib.rfa_sha256_update(C.byref(s),None,0)==0
    assert lib.rfa_sha256_finish(C.byref(s),(C.c_uint8*32)())==0
    lib.rfa_sha256_reset(C.byref(s));s.bytes=0x1fffffffffffffff
    byte=(C.c_uint8*1)(1);before=bytes(s)
    assert lib.rfa_sha256_update(C.byref(s),byte,1)==0 and bytes(s)==before
    assert lib.rfa_crc32_update(None,byte,1)==0
    print('C_SHA256_CRC32_INDEPENDENT_ORACLE_VECTORS='+str(vectors)+' PASS')
class Entry(C.Structure):
    _fields_=[('name',C.POINTER(C.c_uint8)),('data',C.POINTER(C.c_uint8)),('name_size',C.c_uint32),('data_size',C.c_uint32)]
def zip_tests(lib):
    lib.rfa_zip32_store.argtypes=[C.POINTER(Entry),C.c_uint32,C.POINTER(C.c_uint8),C.c_uint32,C.POINTER(C.c_uint32)]
    names=[b'metadata/proof.bin',b'empty.txt'];payloads=[bytes((i*73+19)&255 for i in range(65)),b'']
    namebuf=[(C.c_uint8*len(n)).from_buffer_copy(n) for n in names]
    databuf=[(C.c_uint8*len(p)).from_buffer_copy(p) for p in payloads]
    entries=(Entry*2)(*[Entry(n,d,len(name),len(data)) for n,d,name,data in zip(namebuf,databuf,names,payloads)])
    out=(C.c_uint8*1024)(*[0xa5]*1024);written=C.c_uint32(777)
    assert lib.rfa_zip32_store(entries,2,out,1024,C.byref(written))==1
    archive=bytes(out[:written.value]);expected=b'';central=b''
    for name,data in zip(names,payloads):
        offset=len(expected);crc=zlib.crc32(data)
        expected+=struct.pack('<IHHHHHIIIHH',0x04034b50,20,0,0,0,33,crc,len(data),len(data),len(name),0)+name+data
        central+=struct.pack('<IHHHHHHIIIHHHHHII',0x02014b50,20,20,0,0,0,33,crc,len(data),len(data),len(name),0,0,0,0,0,offset)+name
    expected+=central+struct.pack('<IHHHHIIH',0x06054b50,0,0,2,2,len(central),len(expected),0)
    assert archive==expected
    with zipfile.ZipFile(io.BytesIO(archive)) as z:
        assert z.testzip() is None
        for name,data in zip(names,payloads):assert z.read(name.decode())==data
    if os.environ.get('RFA_ORACLE_DIR'):
        d=Path(os.environ['RFA_ORACLE_DIR']);d.mkdir(parents=True,exist_ok=True)
        (d/'c.zip').write_bytes(archive)
        (d/'c.sha256').write_text(hashlib.sha256(payloads[0]).hexdigest())
    snapshot=bytes(out);written.value=777
    assert lib.rfa_zip32_store(entries,2,out,22,C.byref(written))==0
    assert bytes(out)==snapshot and written.value==777
    entries[1]=entries[0]
    assert lib.rfa_zip32_store(entries,2,out,1024,C.byref(written))==0
    assert lib.rfa_zip32_store(entries,33,out,1024,C.byref(written))==0
    bad=(C.c_uint8*4).from_buffer_copy(b'../x');entries[0]=Entry(bad,None,4,0)
    assert lib.rfa_zip32_store(entries,1,out,1024,C.byref(written))==0
    assert bytes(out)==snapshot and written.value==777
    assert lib.rfa_zip32_store(None,0,out,1024,C.byref(written))==1 and written.value==22
    print('C_ZIP32_EXACT_LAYOUT_CRC_PAYLOAD_NEGATIVE_BOUNDS=PASS')

def adaptive_tests(lib):
    lib.rfa_nlms_bind_q15.argtypes=[C.POINTER(NLMS),C.POINTER(C.c_int16),C.POINTER(C.c_int16),C.c_int,C.c_int32,C.c_uint64]
    lib.rfa_nlms_trial_block_q15.argtypes=[C.POINTER(NLMS),C.POINTER(C.c_int16),C.POINTER(C.c_int16),C.POINTER(C.c_int16),C.c_int,C.c_int,C.POINTER(Trial)]
    lib.rfa_nlms_cancel_block_q15.argtypes=[C.POINTER(NLMS),C.POINTER(C.c_int16),C.POINTER(C.c_int16),C.POINTER(C.c_int16),C.c_int,C.c_int]
    def execute(primary,reference,weights,allow):
        taps=len(weights);count=len(primary)
        w=(C.c_int16*taps)();h=(C.c_int16*taps)();s=NLMS()
        assert lib.rfa_nlms_bind_q15(C.byref(s),w,h,taps,32767,1)==1
        for i,x in enumerate(weights):w[i]=x
        wb=(C.c_int16*taps)(*weights);hb=(C.c_int16*taps)()
        baseline=NLMS(wb,hb,taps,0,32767,1)
        p=(C.c_int16*count)(*primary);r=(C.c_int16*count)(*reference)
        out=(C.c_int16*count)();expected=(C.c_int16*count)();trial=Trial()
        lib.rfa_nlms_cancel_block_q15(C.byref(baseline),p,r,expected,count,0)
        decision=lib.rfa_nlms_trial_block_q15(C.byref(s),p,r,out,count,allow,C.byref(trial))
        if decision in (2,3):
            assert list(out)==list(expected) and list(w)==list(wb) and list(h)==list(hb) and s.position==baseline.position
        if decision==1:assert trial.candidate_energy<=trial.baseline_energy
        return decision,trial
    signal=[10000 if i&1 else -10000 for i in range(32)]
    assert execute(signal,signal,[0],1)[0]==1
    assert execute(signal,signal,[0],0)[0]==2
    assert execute(signal,[0]*32,[2000],1)[0]==2
    rng=random.Random(4261);rollback=0
    for _ in range(2000):
        p=[rng.randrange(-12000,12001) for _ in range(16)]
        r=[rng.randrange(-12000,12001) for _ in range(16)]
        weights=[rng.randrange(-20000,20001) for _ in range(4)]
        decision,_=execute(p,r,weights,1)
        assert decision in (1,3)
        rollback+=decision==3
    assert rollback>0
    w=(C.c_int16*2)();h=(C.c_int16*2)();s=NLMS();trial=Trial()
    assert lib.rfa_nlms_bind_q15(C.byref(s),w,h,2,1,1)==1
    p=(C.c_int16*1)(100);r=(C.c_int16*1)(100);out=(C.c_int16*1)(321)
    for field,value in [('position',2),('taps',257),('mu',32768),('epsilon',0xffffffffffffffff)]:
        old=getattr(s,field);setattr(s,field,value);snapshot=bytes(s)
        assert lib.rfa_nlms_trial_block_q15(C.byref(s),p,r,out,1,1,C.byref(trial))==0
        assert bytes(s)==snapshot and out[0]==321
        setattr(s,field,old)
    assert lib.rfa_nlms_trial_block_q15(C.byref(s),p,r,p,1,1,C.byref(trial))==0
    assert lib.rfa_nlms_trial_block_q15(C.byref(s),p,r,out,4097,1,C.byref(trial))==0
    print('ADAPTIVE_GENERIC_FREEZE_AND_EXACT_ROLLBACK=PASS; REJECTED_TRIALS='+str(rollback))
with tempfile.TemporaryDirectory() as tmp:
    objs=[];actual=[]
    for src in sorted(Path('app/src/main/cpp').glob('*_core.c')):
        obj=Path(tmp)/(src.stem+'.o')
        run(CC,*FLAGS,'-fPIC','-c',str(src),'-o',str(obj))
        assert not run('nm','-u',str(obj)).strip(),src
        named=run('nm','-a',str(obj)).splitlines()
        assert not [x for x in named if len(x.split())>=3 and x.split()[1] in 'bBcCdDgGsS'],src
        actual += [src.stem+':'+x.split()[-1] for x in run('nm','--defined-only','--extern-only',str(obj)).splitlines()]
        objs.append(str(obj))
    expected=[x for x in Path('native/abi/public_symbols_v1.txt').read_text().splitlines() if x and not x.startswith('#')]
    assert sorted(actual)==sorted(expected),'ABI drift'
    linked=Path(tmp)/'rafaelia_core.elf'
    run(CC,'-nostdlib','-r',*objs,'-o',str(linked))
    assert not run('nm','-u',str(linked)).strip()
    assert '(NEEDED)' not in run('readelf','-d',str(linked))
    shared=Path(tmp)/'oracle.so'
    run(CC,'-nostdlib','-shared',*objs,'-o',str(shared))
    assert '(NEEDED)' not in run('readelf','-d',str(shared))
    lib=C.CDLL(str(shared));digest_tests(lib);zip_tests(lib);adaptive_tests(lib)
    for name in ['dsp','meter','manifold','verbo','audio_format_core','bit_stack']:
        exe=Path(tmp)/name
        run(CC,*FLAGS,'native/tests/'+name+'_smoke.c',*objs,'-o',str(exe));run(str(exe))
    print('STANDALONE_CORE='+str(len(objs))+'_UNITS_PASS_ZERO_UNDEFINED_NO_NEEDED_NO_WRITABLE_GLOBALS')
    print('JDK_GRADLE_SDK_NDK_ART_JNI_REQUIRED_BY_THIS_GATE=NO')
    print('HARNESS=HOSTED_PYTHON_STDLIB_NOT_BARE_METAL_RUNTIME_PROOF')
