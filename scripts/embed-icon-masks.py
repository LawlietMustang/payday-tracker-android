#!/usr/bin/env python3
"""Embed supplied SVG artwork so masks work in the offline Android WebView.

Run after editing an icon. --check verifies the committed CSS without writing.
Original SVG files (including metadata) stay intact; only drawing data is embedded.
"""
import argparse
import re
from pathlib import Path
from urllib.parse import quote
import xml.etree.ElementTree as ET

ASSETS = Path(__file__).resolve().parents[1] / 'app/src/main/assets'
ICONS = {
    'workplaces': 'workplaces', 'planning': 'budgets-goals',
    'reminders': 'reminders-widget', 'profile': 'profile', 'lock': 'lock',
    'recovery': 'cloud-sync', 'cancel': 'cancel', 'alert': 'alert',
    'info': 'info', 'sun': 'sun', 'moon': 'moon',
}
ET.register_namespace('', 'http://www.w3.org/2000/svg')


def embedded_css(css):
    for name, filename in ICONS.items():
        svg = ET.parse(ASSETS / (filename + '.svg')).getroot()
        for child in list(svg):
            if child.tag.endswith('}metadata'):
                svg.remove(child)
        for element in svg.iter():
            element.tail = None
            if element.text and not element.text.strip():
                element.text = None
        uri = 'data:image/svg+xml,' + quote(ET.tostring(svg, encoding='unicode'), safe='')
        rule = '[data-icon="' + name + '"] { --route-icon: url("' + uri + '"); }'
        css, count = re.subn(r'\[data-icon="' + re.escape(name) + r'"\] \{ --route-icon:.*?\}', lambda _: rule, css)
        if count != 1:
            raise ValueError('Expected one icon rule for ' + name)
    return css


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    stylesheet = ASSETS / 'design.css'
    original = stylesheet.read_text()
    updated = embedded_css(original)
    if args.check:
        if original != updated:
            raise SystemExit('Icon CSS is stale. Run python3 scripts/embed-icon-masks.py')
        print('All 11 icon masks match the supplied SVG artwork and are embedded.')
    else:
        stylesheet.write_text(updated)
