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

# Runtime strings are ordinary, named Android resources. There is deliberately no
# source-phrase lookup table, translation cache, or text-matching bridge. Android
# resolves each R.string ID from the active locale configuration.

for language in ("fa", "en", "ar"):
    folder = FOLDERS[language]
    target = res / folder / "strings.xml"
    assert target.is_file(), f"Missing central locale file: {target}"
    for other in (res / folder).glob("*.xml"):
        if other == target:
            continue
        other_root = ET.parse(other).getroot()
        assert not any(item.tag in {"string", "string-array"} for item in other_root), (
            f"Localized text must be kept only in {target}, not {other}"
        )
    content = ET.parse(target).getroot()
    actual_ids = {
        item.attrib["name"]
        for item in content
        if item.tag in {"string", "string-array"}
    }
    assert actual_ids == expected_ids, f"String IDs are incomplete in {target}"

# Layout/configuration XML may contain non-linguistic placeholders and symbols,
# but every user-facing word must point to a named string resource.
android_namespace = "{http://schemas.android.com/apk/res/android}"
text_attributes = {"text", "hint", "contentDescription", "title", "summary", "label"}
for target in res.rglob("*.xml"):
    if target.parent.name.startswith("values"):
        continue
    content = ET.parse(target).getroot()
    for element in content.iter():
        for attribute in text_attributes:
            value = element.attrib.get(android_namespace + attribute)
            if not value or value.startswith(("@", "?")):
                continue
            assert not any(character.isalpha() for character in value), (
                f"User-facing text must use @string in {target}: {value!r}"
            )

print(
    f"Verified 3 single-file locale catalogs with {len(expected_ids)} resource IDs each."
)
