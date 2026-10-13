#!/usr/bin/env python3
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
# Source routing guard only; not a physical speech/intelligibility result.
from pathlib import Path
root=Path(__file__).resolve().parents[1]
ui=(root/"app/src/main/java/io/rafaelia/audiostudio/MainActivity.java").read_text(encoding="utf-8")
player=(root/"app/src/main/java/io/rafaelia/audiostudio/AuthorialGuidePlayer.java").read_text(encoding="utf-8")
native=(root/"app/src/main/java/io/rafaelia/audiostudio/NativeDsp.java").read_text(encoding="utf-8")
bridge=(root/"app/src/main/cpp/jni_bridge.c").read_text(encoding="utf-8")
manifest=(root/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
header=(root/"app/src/main/cpp/rfa_ptbr_core.h").read_text(encoding="utf-8")
assert 'Button robotGuide = button("Voz robótica · cadência experimental")' in ui
assert 'guide.playRobotic(scriptEdit.getText().toString())' in ui
assert 'recorder != null || processingGate.busy() || calibrationRunning' in ui
assert 'Button voiceGuide = button("Voice guide · experimental")' in ui
assert 'guide.play(text)' in ui
assert 'ComplexNetworkNarrationPreset.ptBrScript()' in ui
assert 'private synchronized boolean playInternal(String text, boolean robotic)' in player
assert 'int[] state = new int[robotic ? 9 : 6]' in player
assert 'NativeDsp.nativeRobotPtbrRender(text, state, block)' in player
assert 'NativeDsp.nativePtbrRender(text, state, block)' in player
assert 'static native int nativeRobotPtbrRender(byte[] utf8, int[] state, short[] pcm)' in native
assert 'Java_io_rafaelia_audiostudio_NativeDsp_nativeRobotPtbrRender' in bridge
assert 'rfa_ptbr_robot_render(&state' in bridge
assert 'rfa_ptbr_robot_render(' in header
assert 'android.permission.INTERNET' not in manifest
assert 'WRITE_EXTERNAL_STORAGE' not in manifest
assert 'android.permission.CAPTURE_AUDIO_OUTPUT' not in manifest
print("ROBOTIC_PTBR_OPTIN_JNI_LEGACY_AND_PERMISSION_ROUTING=PASS_SOURCE_SCOPED")
print("ROBOTIC_PTBR_SPEAKER_INTELLIGIBILITY=NOT_RUN")
