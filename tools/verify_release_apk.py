#!/usr/bin/env python3
"""Check the compiled Release APK and R8 outputs, without accessing signing keys."""
import argparse
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, default=Path("app/build/outputs/apk/release/app-release-unsigned.apk"))
    parser.add_argument("--mapping", type=Path, default=Path("app/build/outputs/mapping/release/mapping.txt"))
    parser.add_argument("--aapt2", type=Path)
    parser.add_argument("--dexdump", type=Path)
    args = parser.parse_args()
    if not args.apk.is_file() or not args.mapping.is_file():
        raise ValueError("Build assembleRelease first; APK and R8 mapping are required.")
    aapt = args.aapt2
    if aapt is None:
        sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
        if not sdk:
            raise ValueError("Set ANDROID_HOME or pass --aapt2.")
        name = "aapt2.exe" if os.name == "nt" else "aapt2"
        candidates = sorted((Path(sdk) / "build-tools").glob(f"*/{name}"))
        if not candidates:
            raise ValueError("Android SDK build-tools/aapt2 is required.")
        aapt = candidates[-1]
    manifest = subprocess.check_output(
        [str(aapt), "dump", "xmltree", str(args.apk), "--file", "AndroidManifest.xml"],
        text=True, encoding="utf-8",
    )
    for flag in ("debuggable", "testOnly", "shell", "allowBackup", "usesCleartextTraffic"):
        # debuggable=false is normally omitted by AGP; Android defaults it to false.
        if flag == "debuggable" and not re.search(r"android:debuggable\b", manifest):
            continue
        pattern = rf"android:{flag}(?:\(0x[0-9a-fA-F]+\))?=(?:\(type 0x12\)0x0|false)\b"
        if not re.search(pattern, manifest):
            raise ValueError(f"Compiled APK must explicitly disable android:{flag}.")
    mapping = args.mapping.read_text(encoding="utf-8")
    classes = re.findall(r"^(com\.ilia\.advanceclock\.\S+) -> ([^:]+):$", mapping, re.MULTILINE)
    renamed = sum(original != obfuscated for original, obfuscated in classes)
    if not renamed:
        raise ValueError("No obfuscated app classes found in R8 mapping.")
    # R8 omits unchanged fields from mapping.txt. Inspect actual declared DEX
    # members instead, so reflection safety is checked against the packaged code.
    dexdump = args.dexdump or aapt.with_name("dexdump.exe" if os.name == "nt" else "dexdump")
    main_class = None
    with tempfile.TemporaryDirectory(prefix="clock-release-") as directory:
        with zipfile.ZipFile(args.apk) as apk:
            for name in apk.namelist():
                if not re.fullmatch(r"classes(?:\d+)?\.dex", name):
                    continue
                dex = Path(directory) / name
                dex.write_bytes(apk.read(name))
                output = subprocess.check_output([str(dexdump), str(dex)], text=True,
                                                 encoding="utf-8", errors="replace")
                for block in output.split("\nClass #"):
                    if re.search(r"Class descriptor\s*:\s*'Lcom/ilia/advanceclock/MainActivity;'", block):
                        main_class = block
                        break
                if main_class is not None:
                    break
    if main_class is None:
        raise ValueError("MainActivity missing from compiled DEX.")
    for member in ("quickNoteDue", "noteRecurrenceMode", "noteIntervalDays", "noteCustomDates",
                   "updateQuickNoteLabels", "renderNoForget"):
        if not re.search(rf"\bname\s*:\s*'{member}'", main_class):
            raise ValueError(f"Required reflected note member was not preserved: {member}")
    print(f"Release verified: debugging, shell profiling, test-only mode, backup and cleartext traffic disabled; {renamed} app classes obfuscated.")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print(f"Release verification failed: {error}", file=sys.stderr)
        sys.exit(1)
