#!/usr/bin/env bash
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
RFA_ORACLE_DIR="$tmp" python3 ci/core-standalone-gate.py
javac -d "$tmp/classes" \
  app/src/main/java/io/rafaelia/audiostudio/LowSha256.java \
  app/src/main/java/io/rafaelia/audiostudio/RfaStoredZip.java \
  app/src/main/java/io/rafaelia/audiostudio/NativeDsp.java \
  app/src/main/java/io/rafaelia/audiostudio/NativeByteCore.java \
  ci/CoreByteOracle.java \
  ci/NativeByteCoreFallbackTest.java
java -cp "$tmp/classes" io.rafaelia.audiostudio.CoreByteOracle "$tmp"
java -cp "$tmp/classes" io.rafaelia.audiostudio.NativeByteCoreFallbackTest
cmp "$tmp/c.zip" "$tmp/java.zip"
cmp "$tmp/c.sha256" "$tmp/java.sha256"
echo 'C_JAVA_SHA256_AND_WHOLE_ZIP32_BYTES=PASS'
echo 'NATIVE_BYTE_CORE_FAIL_CLOSED_HOST_FALLBACK=PASS'
echo 'JAVA_ORACLE=HOST_TEST_ONLY_NOT_CORE_DEPENDENCY'
