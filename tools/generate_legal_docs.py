#!/usr/bin/env python3
"""Keep the public legal pages identical to the in-app string resources."""
from pathlib import Path
import argparse
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def strings(folder):
    root = ET.parse(ROOT / 'app/src/main/res' / folder / 'strings.xml').getroot()
    return {item.attrib['name']: ''.join(item.itertext()).replace('\\n', '\n')
            .replace("\\'", "'").replace('\\"', '"')
            for item in root.findall('string')}


def documents():
    base = strings('values')
    locales = [('values-en', 'english', 'English'), ('values-fa', 'persian', 'فارسی'),
               ('values-ar', 'arabic', 'العربية')]
    links = '''- [OpenStreetMap privacy](https://osmfoundation.org/wiki/Privacy_Policy)
- [OpenStreetMap attribution / ODbL](https://www.openstreetmap.org/copyright)
- [TimeAPI privacy](https://www.timeapi.io/Home/Privacy)
- [Open-Meteo terms and privacy](https://open-meteo.com/en/terms)
'''
    for filename, label, prefix, sections in [
        ('PRIVACY_POLICY.md', 'Privacy policy', 'about_privacy_',
         ['intro', 'local', 'location', 'providers', 'media', 'security', 'delete', 'contact']),
        ('TERMS_OF_USE.md', 'Terms of use', 'about_terms_', ['use', 'reliability', 'external', 'changes'])
    ]:
        result = f"# {base['app_name']} — {label}\n\n"
        result += '[English](#english) · [فارسی](#persian) · [العربية](#arabic)\n\n'
        result += f"Developer / سازنده / المطوّر: **{base['about_developer_name']}**\n\n"
        result += f"Last updated: **{base['about_policy_date']}**\n\n"
        for folder, anchor, language in locales:
            values = strings(folder)
            result += f'<a id="{anchor}"></a>\n\n## {language}\n\n'
            for section in sections:
                result += f"### {values[prefix + section + '_title']}\n\n{values[prefix + section + '_body']}\n\n"
            if filename == 'PRIVACY_POLICY.md':
                result += links + '\n'
        yield ROOT / 'docs' / filename, result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='Fail if published copy differs from app text')
    args = parser.parse_args()
    outdated = []
    for path, content in documents():
        if args.check:
            if not path.exists() or path.read_text(encoding='utf-8') != content:
                outdated.append(str(path.relative_to(ROOT)))
        else:
            path.write_text(content, encoding='utf-8')
    if outdated:
        print('Legal pages out of date: ' + ', '.join(outdated), file=sys.stderr)
        print('Run: python tools/generate_legal_docs.py', file=sys.stderr)
        return 1
    print('Public and in-app legal text are synchronized.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
