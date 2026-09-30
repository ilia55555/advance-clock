#!/usr/bin/env python3
import pathlib
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]

FOLDERS = {
    "fa": "values-fa",
    "en": "values-en",
    "fr": "values-fr",
    "de": "values-de",
    "es": "values-es",
    "ru": "values-ru",
    "tr": "values-tr",
    "pt": "values-pt",
    "hi": "values-hi",
    "ja": "values-ja",
    "zh-CN": "values-zh-rCN",
    "ar": "values-ar",
}

res = ROOT / "app" / "src" / "main" / "res"
base = ET.parse(res / "values" / "strings.xml").getroot()
expected_ids = {
    item.attrib["name"]
    for item in base
    if item.tag in {"string", "string-array"}
}
assert expected_ids, "The central runtime string catalog is empty"

for folder in FOLDERS.values():
    target = res / folder / "strings.xml"
    assert target.is_file(), f"Missing central locale file: {target}"
    content = ET.parse(target).getroot()
    actual_ids = {
        item.attrib["name"]
        for item in content
        if item.tag in {"string", "string-array"}
    }
    assert actual_ids == expected_ids, f"String IDs are incomplete in {target}"

print(f"Verified {len(FOLDERS)} central locale files with {len(expected_ids)} string IDs each.")
