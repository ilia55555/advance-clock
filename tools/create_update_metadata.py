#!/usr/bin/env python3
"""Create update.json from an already signed production APK; never opens signing keys."""
import argparse
import hashlib
import json
import pathlib
import re
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=pathlib.Path)
    parser.add_argument('--aapt', default='aapt', help='Path to Android SDK build-tools aapt/aapt2')
    parser.add_argument('--apksigner', default='apksigner', help='Path to Android SDK build-tools apksigner')
    parser.add_argument('--output', type=pathlib.Path, default=pathlib.Path('update.json'))
    args = parser.parse_args()
    subprocess.run([args.apksigner, 'verify', '--verbose', str(args.apk)], check=True)
    badging = subprocess.check_output([args.aapt, 'dump', 'badging', str(args.apk)], text=True)
    match = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
    if not match or match[1] != 'com.ilia.advanceclock' or not re.fullmatch(r'\d+(?:\.\d+){3}', match[3]):
        parser.error('Expected production InfiniTime APK with a four-part version name')
    if 'application-debuggable' in badging or 'application-testOnly' in badging:
        parser.error('Debug/test APK cannot be published')
    size = args.apk.stat().st_size
    if not 0 < size <= 150 * 1024 * 1024:
        parser.error('APK size exceeds update limits')
    with args.apk.open('rb') as stream:
        digest = hashlib.file_digest(stream, 'sha256').hexdigest()
    metadata = dict(versionCode=int(match[2]), versionName=match[3], tag='v' + match[3],
                    size=size, sha256=digest)
    args.output.write_text(json.dumps(metadata, indent=2) + '\n', encoding='utf-8')
    print(f'Upload {args.apk} as InfiniTime.apk and {args.output} to release {metadata["tag"]}')


if __name__ == '__main__':
    main()
