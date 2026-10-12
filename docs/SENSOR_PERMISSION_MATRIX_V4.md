# Permission matrix V4 — Wave Lab successor

Supersedes V3 operational profile; V3 remains historical. Source state until exact-head CI: LOCAL_SCOPED_PASS, not physical PASS.

| Permission | Purpose and boundary |
|---|---|
| RECORD_AUDIO | explicit microphone action only |
| ACCESS_NETWORK_STATE | passive network metadata |
| ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION | paired foreground request on explicit Wave Lab snapshot; approximate-only grant does not authorize precise GPS/Wi-Fi/cell observations |
| ACCESS_WIFI_STATE | cached scan results only; no active scan |
| BLUETOOTH | legacy normal permission, maxSdkVersion=30 |
| BLUETOOTH_CONNECT | SDK >=31 checked before adapter read, not automatically requested; denial remains TOKEN_VAZIO |

No INTERNET, background location, active Bluetooth scan/advertise, Wi-Fi mutation, activity/body sensor, camera or high-rate permission. No new radio-control API. Backup remains disabled; Wave Lab remains unexported. The seven-permission reviewed profile replaces the stale two-permission profile, not a general allowlist relaxation. XML parsing rejects additional tags, attributes, duplicates and altered legacy SDK boundary. Sensor rates/disclosure gates remain mandatory.

Android primary reference: https://developer.android.com/develop/sensors-and-location/location/permissions/runtime (fine and coarse request together on Android 12+).

Tests: `bash ci/sensor-permission-gate.sh`; `python3 ci/test-permission-policy.py --selftest`. Negative fixtures: INTERNET, missing/changed legacy maxSdk, duplicate microphone, backup enabled, exported lab, background location substitution. No actual location, radio or microphone access occurs in these fixtures.

Physical permission denial/approximate/grant flows: NOT_RUN_BY_AGENT. User declares execution on Moto E7 ARM32 and Samsung A1 ARM64; exact APK/device log linkage remains TOKEN_VAZIO.
