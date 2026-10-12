#!/usr/bin/env python3
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
"""Exact permission/attribute surface: reject extras, duplicate and namespace drift."""
import argparse
import xml.etree.ElementTree as ET
from pathlib import Path
A = '{http://schemas.android.com/apk/res/android}'
EXPECTED = {
    'ACCESS_COARSE_LOCATION': {}, 'ACCESS_FINE_LOCATION': {},
    'ACCESS_NETWORK_STATE': {}, 'ACCESS_WIFI_STATE': {},
    'BLUETOOTH': {'maxSdkVersion': '30'}, 'BLUETOOTH_CONNECT': {},
    'RECORD_AUDIO': {},
}
def validate(text):
    root = ET.fromstring(text)
    found = {}
    for node in root:
        if node.tag.startswith('uses-permission'):
            if node.tag != 'uses-permission':
                raise ValueError('unreviewed permission tag')
            attrs = dict(node.attrib)
            name = attrs.pop(A + 'name', '')
            if not name.startswith('android.permission.') or name in found:
                raise ValueError('missing/duplicate permission')
            found[name] = attrs
    expected = {'android.permission.' + k: {A+a: v for a,v in attrs.items()}
                for k, attrs in EXPECTED.items()}
    if found != expected:
        raise ValueError('permission/SDK surface differs from reviewed profile')
    app = root.find('application')
    if app is None or app.get(A+'allowBackup') != 'false':
        raise ValueError('backup protection lost')
    lab = next((a for a in app.findall('activity') if a.get(A+'name') == '.WavePhysicsLabActivity'), None)
    if lab is None or lab.get(A+'exported') != 'false':
        raise ValueError('lab export protection lost')

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--selftest', action='store_true')
    args = parser.parse_args()
    text = Path('app/src/main/AndroidManifest.xml').read_text()
    validate(text)
    if args.selftest:
        mutations = [
            text.replace('</manifest>', '<uses-permission android:name="android.permission.INTERNET" /></manifest>'),
            text.replace(' android:maxSdkVersion="30"', ''),
            text.replace('android:maxSdkVersion="30"', 'android:maxSdkVersion="31"'),
            text.replace('</manifest>', '<uses-permission android:name="android.permission.RECORD_AUDIO" /></manifest>'),
            text.replace('android:allowBackup="false"', 'android:allowBackup="true"'),
            text.replace('android:name=".WavePhysicsLabActivity"\n            android:exported="false"', 'android:name=".WavePhysicsLabActivity" android:exported="true"'),
            text.replace('android.permission.ACCESS_COARSE_LOCATION', 'android.permission.ACCESS_BACKGROUND_LOCATION'),
        ]
        for i, mutation in enumerate(mutations):
            try:
                validate(mutation)
            except (ValueError, ET.ParseError):
                continue
            raise AssertionError('mutation accepted: '+str(i))
        print('PERMISSION_ADVERSARIAL=7_REJECTED')
    print('PERMISSION_XML_EXACT_PROFILE=PASS')
if __name__ == '__main__':
    main()
