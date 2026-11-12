#!/usr/bin/env bash
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
set -euo pipefail
cd "$(dirname "$0")/.."
activity='app/src/main/java/io/rafaelia/audiostudio/InstrumentSurveyActivity.java'
collector='app/src/main/java/io/rafaelia/audiostudio/InstrumentSurveyCollector.java'
model='app/src/main/java/io/rafaelia/audiostudio/InstrumentSurveyModel.java'
test -s "$activity"
test -s "$collector"
test -s "$model"
grep -Fq 'finishSession("Tela saiu do primeiro plano")' "$activity"
grep -Fq 'collector.stop()' "$activity"
grep -Fq 'sensitive_location_radio=' "$activity"
grep -Fq 'service_mode=NOT_PUBLIC_API' "$collector"
grep -Fq 'geospatial_database=NOT_CONNECTED' "$collector"
grep -Fq 'getScanResults()' "$collector"
if grep -Fq 'startScan()' "$collector"; then
  echo 'INSTRUMENT_SURVEY_ACTIVE_WIFI_SCAN=FAIL' >&2
  exit 1
fi
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -Xlint:all -Werror -d "$tmp" \
  "$model" \
  ci/InstrumentSurveyModelTest.java
java -cp "$tmp" io.rafaelia.audiostudio.InstrumentSurveyModelTest
echo 'INSTRUMENT_SURVEY_PLATFORM_BOUNDARY=PASS_SOURCE_SCOPE'
