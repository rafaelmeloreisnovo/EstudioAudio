#!/usr/bin/env python3
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
# Static contract only: cannot assert Android intent delivery or physical speaker.
from pathlib import Path
m = Path("app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
lab = Path("app/src/main/java/io/rafaelia/audiostudio/TimbreLearningLabActivity.java").read_text(encoding="utf-8")
player = Path("app/src/main/java/io/rafaelia/audiostudio/AuthorialGuidePlayer.java").read_text(encoding="utf-8")
native = Path("app/src/main/cpp/rfa_timbre_core.c").read_text(encoding="utf-8")
zipper = Path("app/src/main/java/io/rafaelia/audiostudio/RfaStoredZip.java").read_text(encoding="utf-8")
assert 'android:name=".TimbreLearningLabActivity"' in m
assert '<action android:name="android.intent.action.SEND"' in m
assert 'android:mimeType="audio/*"' in m
assert 'getContentResolver().openInputStream(uri)' in lab
assert '"content".equals(uri.getScheme())' in lab
assert 'MAX_DATA = 2000000' in lab
assert 'u16(fmt, 0) != 1' in lab and 'u16(fmt, 2) != 1' in lab
assert 'NativeDsp.nativeTimbreAccumulate' in lab
assert 'TimbreProfileStore.put' in lab
assert 'RfaStoredZip zip = new RfaStoredZip(out)' in lab
assert 'Downloads.EXTERNAL_CONTENT_URI' in lab
assert 'RAFAELIA_TTS_LAB' in lab
assert 'MediaStore.MediaColumns.IS_PENDING, 1' in lab
assert 'getContentResolver().delete(target, null, null)' in lab
assert 'original_audio=NOT_STORED' in lab
assert 'profile.v1.txt' in lab and 'receipt.v1.txt' in lab
assert 'zip.add("audio' not in lab and 'java.util.zip' not in lab and 'java.util.zip' not in zipper
assert 'NativeDsp.nativeTimbreApply' in player
assert 'TimbreProfileStore.snapshot' in player
assert 'static rfa_u64 rfa_timbre_div' in native
for name in ('malloc(', 'free(', 'memcpy(', 'printf(', '#include <'):
    assert name not in native, name
print("SHARE_WAV_BOUNDARY_ZIPRAF_PRIVACY_STATIC=PASS_SCOPED")
print("ANDROID_URI_LIFECYCLE_AND_SPEAKER_PHYSICAL=NOT_RUN")
