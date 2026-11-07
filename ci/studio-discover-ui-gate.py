#!/usr/bin/env python3
"""Scoped source-only guard; NOT an Android build or physical UX test."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
java = (root / "app/src/main/java/io/rafaelia/audiostudio/StudioDiscoverActivity.java").read_text()
main = (root / "app/src/main/java/io/rafaelia/audiostudio/MainActivity.java").read_text()
manifest = (root / "app/src/main/AndroidManifest.xml").read_text()
gradle = (root / "app/build.gradle").read_text()

def must(condition, message):
    if not condition:
        raise SystemExit("FAIL: " + message)

must('android:name=".StudioDiscoverActivity"' in manifest, "internal activity missing")
must('android:exported="false"' in manifest, "nonexported boundary missing")
must('new Intent(this, StudioDiscoverActivity.class)' in main, "explicit main entry missing")
must('STUDIO_DISCOVER_BLOCKED' in main, "audio session guard missing")
for page in ["Visão geral", "Recursos", "Confiança", "Links"]:
    must(page in java, "page missing: " + page)
for label in ["EXPERIMENTAL", "NÃO VALIDADA", "EVIDÊNCIA PENDENTE",
              "C FREESTANDING", "SOMENTE POR TOQUE"]:
    must(label in java, "claim label missing: " + label)
must('setMinHeight(dp(48))' in java, "minimum touch target absent")
must('ACTION_VIEW' in java and 'ActivityNotFoundException' in java,
     "external link behavior/guard absent")
must('requestPermissions(' not in java and 'AudioRecord' not in java,
     "informational screen must not capture/request audio")
must("minSdk 29" in gradle, "known Android 10 baseline moved")
routes = [line.strip().strip('",') for line in java.splitlines()
          if line.strip().startswith('"https://')]
must(len(routes) == 6, "route allowlist changed")
must(all(route.startswith('https://github.com/rafaelmeloreisnovo/EstudioAudio')
         for route in routes), "unsafe or out-of-scope URL")
print("PASS_SCOPED: StudioDiscover source routing and claim safety")
print("NOT_RUN: Java compile / APK / hardware UX / physical audio")
