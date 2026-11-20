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
rail=app/src/main/java/io/rafaelia/audiostudio/PcmRailGate.java
rail_test=ci/java/PcmRailGateTest.java
for file in "$jni" "$native" "$cal" "$ui" "$zip" "$cfr" "$rail" "$rail_test"; do test -s "$file" || fail "SOURCE_MISSING=$file"; done
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
must "$cal" 'PcmRailGate.countFullScaleSamples(response, captured)'
must "$ui" 'ensurePermissionAndCalibrate();'
must "$ui" 'showComparison(result);'
must "$ui" 'PIPELINE=CAPTURED_AND_ANALYZED_RELATIVE'
must "$ui" 'PCM_FULL_SCALE_SAMPLES='
must "$zip" 'reference_capture_comparison'
must "$zip" 'TIME_SLICED_SWEEP_NOT_FFT'
must "$zip" 'independent_replay'
must "$zip" 'pcm_full_scale_samples'
must "$cfr" 'NativeDsp.CHUNK_FLAG_REFERENCE'
must "$cfr" 'NativeDsp.CHUNK_FLAG_RESPONSE'
test "$(find .github/workflows -maxdepth 1 -type f \( -name '*.yml' -o -name '*.yaml' \) | wc -l | tr -d ' ')" -eq 1 || fail 'WORKFLOW_SINGLE_ROOT'
# Java/JDK is a hosted test tool, not a new runtime dependency.
# If unavailable in a source-only host lane, type the unexecuted test explicitly.
if command -v javac >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
    rail_tmp="$(mktemp -d)"
    trap 'rm -rf -- "$rail_tmp"' EXIT
    javac -Xlint:all -d "$rail_tmp" "$rail" "$rail_test"
    java -cp "$rail_tmp" io.rafaelia.audiostudio.PcmRailGateTest
    printf 'PCM_RAIL_GATE_HOST=PASS_SCOPED\n'
else
    printf 'PCM_RAIL_GATE_HOST=TOKEN_VAZIO_JDK_UNAVAILABLE_NOT_RUN\n'
fi
printf 'LOOPBACK_CONTRACT_SOURCE=PASS\n'
printf 'LOOPBACK_JNI_BRIDGE=DECLARED_AND_BOUNDED\n'
printf 'LOOPBACK_CFR_REFERENCE_RESPONSE=SOURCE_PRESENT\n'
printf 'LOOPBACK_NDK_APK=NOT_RUN_IN_THIS_GATE\n'
printf 'LOOPBACK_PHYSICAL_TEST=NOT_RUN_IN_CI\n'
printf 'LOOPBACK_ABS_SPL=TOKEN_VAZIO_PHYSICAL_REFERENCE\n'
printf 'LOOPBACK_CLAIM_ALLOWED=false\n'
