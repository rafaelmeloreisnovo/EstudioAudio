#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
fail() { printf '[LOOPBACK_AB_GATE][FAIL] %s\n' "$*" >&2; exit 1; }
must() { grep -Fq "$2" "$1" || fail "$1 :: $2"; }
jni=app/src/main/cpp/jni_bridge.c
native=app/src/main/java/io/rafaelia/audiostudio/NativeDsp.java
cal=app/src/main/java/io/rafaelia/audiostudio/RelativeCalibrationEngine.java
ui=app/src/main/java/io/rafaelia/audiostudio/MainActivity.java
zip=app/src/main/java/io/rafaelia/audiostudio/AssuranceZiprafWriter.java
cfr=app/src/main/java/io/rafaelia/audiostudio/CalibrationContainerWriter.java
for file in "$jni" "$native" "$cal" "$ui" "$zip" "$cfr"; do test -s "$file" || fail "SOURCE_MISSING=$file"; done
must "$jni" 'Java_io_rafaelia_audiostudio_NativeDsp_nativePlaybackObserve'
must "$jni" 'rfa_playback_observe_mono('
must "$jni" 'if (count > length || (*env)->GetArrayLength(env, output) != 13) return 0;'
must "$jni" 'ReleaseShortArrayElements(env, input, data, JNI_ABORT);'
must "$native" 'static final int OBSERVER_OUTPUT_LONGS = 13;'
must "$native" 'static native int nativePlaybackObserve('
must "$cal" 'NativeDsp.nativePlaybackObserve('
must "$cal" 'observerMetrics[3] != captured'
must "$cal" 'CalibrationContainerWriter.writeRelativeCfr('
must "$cal" 'interface StageListener { void phase(String phase); }'
must "$cal" 'sweep time-slices'
must "$ui" 'ensurePermissionAndCalibrate();'
must "$ui" 'showComparison(result);'
must "$ui" 'PIPELINE=CAPTURED_AND_ANALYZED_RELATIVE'
must "$zip" 'reference_capture_comparison'
must "$zip" 'TIME_SLICED_SWEEP_NOT_FFT'
must "$zip" 'independent_replay'
must "$cfr" 'NativeDsp.CHUNK_FLAG_REFERENCE'
must "$cfr" 'NativeDsp.CHUNK_FLAG_RESPONSE'
test "$(find .github/workflows -maxdepth 1 -type f \( -name '*.yml' -o -name '*.yaml' \) | wc -l | tr -d ' ')" -eq 1 || fail 'WORKFLOW_SINGLE_ROOT'
printf 'LOOPBACK_CONTRACT_SOURCE=PASS\n'
printf 'LOOPBACK_JNI_BRIDGE=DECLARED_AND_BOUNDED\n'
printf 'LOOPBACK_CFR_REFERENCE_RESPONSE=SOURCE_PRESENT\n'
printf 'LOOPBACK_NDK_APK=NOT_RUN_IN_THIS_GATE\n'
printf 'LOOPBACK_PHYSICAL_TEST=NOT_RUN_IN_CI\n'
printf 'LOOPBACK_ABS_SPL=TOKEN_VAZIO_PHYSICAL_REFERENCE\n'
printf 'LOOPBACK_CLAIM_ALLOWED=false\n'
