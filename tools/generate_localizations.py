#!/usr/bin/env python3
import base64
import json
import pathlib
import zlib
from xml.sax.saxutils import escape

ROOT = pathlib.Path(__file__).resolve().parents[1]
PAYLOAD_DIR = ROOT / "tools" / "localization_payload"
DATA = "".join((PAYLOAD_DIR / f"part{i}.txt").read_text(encoding="utf-8").strip() for i in range(5))
payload = json.loads(zlib.decompress(base64.b64decode(DATA)).decode("utf-8"))
sources = payload["sources"]
translations = payload["translations"]
palettes = payload["palette"]

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

def android_escape(value):
    # Android's resource parser requires apostrophes and literal backslashes escaped.
    return escape(value.replace("\\", "\\\\").replace("'", "\\'"))

def items(values):
    return "\n".join(f"        <item>{android_escape(value)}</item>" for value in values)

def base_xml():
    return f'''<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <!-- Complete Persian source catalog. Array order must match runtime_translation. -->\n    <string-array name="runtime_source_fa" translatable="false">\n{items(sources)}\n    </string-array>\n    <string-array name="runtime_translation">\n{items(translations['fa'])}\n    </string-array>\n    <string-array name="palette_names">\n{items(palettes['fa'])}\n    </string-array>\n</resources>\n'''

def locale_xml(code):
    return f'''<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <string-array name="runtime_translation">\n{items(translations[code])}\n    </string-array>\n    <string-array name="palette_names">\n{items(palettes[code])}\n    </string-array>\n</resources>\n'''

res = ROOT / "app" / "src" / "main" / "res"
(res / "values" / "localization_full.xml").write_text(base_xml(), encoding="utf-8")
for code, folder in FOLDERS.items():
    target = res / folder / "localization_full.xml"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(locale_xml(code), encoding="utf-8")

expected = len(sources)
assert all(len(translations[code]) == expected for code in translations)
assert all(len(palettes[code]) == 7 for code in palettes)
print(f"Generated {len(FOLDERS) + 1} localization files with {expected} translated UI entries each.")
