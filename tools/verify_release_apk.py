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
    declared_classes = set()
    class_bodies = {}
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
                    descriptor = re.search(r"Class descriptor\s*:\s*'L([^;]+);'", block)
                    if descriptor:
                        class_name = descriptor.group(1).replace("/", ".")
                        declared_classes.add(class_name)
                        class_bodies[class_name] = block
                    if re.search(r"Class descriptor\s*:\s*'Lcom/ilia/advanceclock/MainActivity;'", block):
                        main_class = block
    if main_class is None:
        raise ValueError("MainActivity missing from compiled DEX.")
    for member in ("quickNoteDue", "noteRecurrenceMode", "noteIntervalDays", "noteCustomDates",
                   "updateQuickNoteLabels", "renderNoForget"):
        if not re.search(rf"\bname\s*:\s*'{member}'", main_class):
            raise ValueError(f"Required reflected note member was not preserved: {member}")
    # Components are loaded by the OS from names in the compiled manifest.
    components = set()
    element = None
    for line in manifest.splitlines():
        tag = re.search(r"\bE: ([\w-]+)", line)
        if tag:
            element = tag.group(1)
        if element in {"application", "activity", "service", "receiver", "provider"}:
            name = re.search(r'android:name(?:\(0x[0-9a-fA-F]+\))?="(com\.ilia\.advanceclock\.[^"]+)"', line)
            if name:
                components.add(name.group(1))
    missing = components - declared_classes
    if missing:
        raise ValueError(f"Manifest components missing from DEX: {sorted(missing)}")
    def require_constructor(class_name, signature):
        pattern = rf"\bname\s*:\s*'<init>'\s+type\s*:\s*'{re.escape(signature)}'\s+access\s*:[^\n]*\bPUBLIC\b"
        if not re.search(pattern, class_bodies[class_name]):
            raise ValueError(f"Required public constructor missing: {class_name}{signature}")
    for name in components:
        require_constructor(name, "()V")
    resource_dump = subprocess.check_output([str(aapt), "dump", "resources", str(args.apk)],
                                            text=True, encoding="utf-8")
    resources = {}
    for block in re.split(r"\n\s+resource ", resource_dump)[1:]:
        header = re.match(r"0x[0-9a-fA-F]+ (\S+)", block)
        if header:
            resources[header.group(1)] = re.findall(r"\(file\) (\S+)", block)
    required = {"raw/rawadhan_1", "raw/rawadhan_2"} | {
        "drawable/prayer_" + name for name in
        ("morning", "sunrise", "noon", "afternoon", "sunset", "maghrib", "isha", "midnight")
    }
    layouts = 0
    with zipfile.ZipFile(args.apk) as apk:
        for name in required:
            files = resources.get(name, [])
            if not files or not all(path in apk.namelist() and apk.getinfo(path).file_size for path in files):
                raise ValueError(f"Dynamically referenced resource missing or empty: {name}")
        for name, files in resources.items():
            if not name.startswith("layout/"):
                continue
            for path in files:
                tree = subprocess.check_output([str(aapt), "dump", "xmltree", str(args.apk), "--file", path],
                                               text=True, encoding="utf-8")
                views = set(re.findall(r"\bE: (com\.ilia\.advanceclock\.[\w.$]+)", tree))
                if not views <= declared_classes:
                    raise ValueError(f"Custom views missing from DEX in {name}: {sorted(views - declared_classes)}")
                for view in views:
                    require_constructor(view, "(Landroid/content/Context;Landroid/util/AttributeSet;)V")
                layouts += 1
    print(f"Release verified: debugging, shell profiling, test-only mode, backup and cleartext traffic disabled; {renamed} app classes obfuscated.")
    print(f"Compatibility verified: {len(components)} manifest components, {layouts} packaged layouts, reflected note members and {len(required)} named sound/widget resources retained.")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print(f"Release verification failed: {error}", file=sys.stderr)
        sys.exit(1)
