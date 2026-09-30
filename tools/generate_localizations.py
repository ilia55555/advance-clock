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

# The legacy runtime bridge has one stable resource ID for every Persian source
# phrase and one matching localized resource reference. Keep this relationship
# explicit so a locale remains fully editable from its single strings.xml file.
full_catalog = ET.parse(res / "values" / "localization_full.xml").getroot()
source_strings = {
    item.attrib["name"]: "".join(item.itertext())
    for item in full_catalog
    if item.tag == "string" and item.attrib["name"].startswith("runtime_source_")
}
source_array = full_catalog.find("string-array[@name='runtime_source_fa']")
translation_array = full_catalog.find("string-array[@name='runtime_translation']")
assert source_array is not None and translation_array is not None, (
    "Runtime localization arrays are missing"
)
source_refs = [item.text for item in source_array]
translation_refs = [item.text for item in translation_array]
assert len(source_refs) == len(translation_refs) == len(source_strings), (
    "Runtime source and translation catalogs must have the same size"
)
assert len(set(source_strings.values())) == len(source_strings), (
    "Every runtime source phrase must have one independent resource ID"
)
for source_ref, translation_ref in zip(source_refs, translation_refs):
    source_name = source_ref.removeprefix("@string/")
    translation_name = translation_ref.removeprefix("@string/")
    assert source_name in source_strings, f"Unknown runtime source: {source_ref}"
    assert translation_name in expected_ids, (
        f"Unknown runtime translation: {translation_ref}"
    )

for folder in FOLDERS.values():
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
    f"Verified {len(FOLDERS)} single-file locale catalogs with "
    f"{len(expected_ids)} string IDs and {len(source_strings)} runtime phrases each."
)
