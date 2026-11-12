#!/usr/bin/env bash
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
set -euo pipefail
cd "$(dirname "$0")/.."
activity='app/src/main/java/io/rafaelia/audiostudio/InstrumentSurveyActivity.java'
collector='app/src/main/java/io/rafaelia/audiostudio/InstrumentSurveyCollector.java'
model='app/src/main/java/io/rafaelia/audiostudio/InstrumentSurveyModel.java'
text='app/src/main/java/io/rafaelia/audiostudio/InstrumentSurveyText.java'
test -s "$activity"
test -s "$collector"
test -s "$model"
test -s "$text"
grep -Fq 'finishSession("LEFT_FOREGROUND")' "$activity"
grep -Fq 'collector.stop()' "$activity"
grep -Fq 'sensitive_location_radio=' "$activity"
grep -Fq 'missing("capability.servicemode", "NOT_PUBLIC_API")' "$collector"
grep -Fq 'missing("capability.tower_database", "DATABASE_NOT_CONNECTED")' "$collector"
grep -Fq 'getScanResults()' "$collector"
for language in 'pt-BR' 'en' 'es' 'fr'; do
  grep -Fq "\"$language\"" "$text"
done
if grep -Fq 'startScan()' "$collector"; then
  echo 'INSTRUMENT_SURVEY_ACTIVE_WIFI_SCAN=FAIL' >&2
  exit 1
fi
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -Xlint:all -Werror -d "$tmp" \
  "$text" \
  "$model" \
  ci/InstrumentSurveyModelTest.java
java -cp "$tmp" io.rafaelia.audiostudio.InstrumentSurveyModelTest
echo 'INSTRUMENT_SURVEY_PLATFORM_BOUNDARY=PASS_SOURCE_SCOPE'
