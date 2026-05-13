#!/usr/bin/env python3
"""
Re-fetch le texte hébreu depuis Sefaria EN CONSERVANT les téamim (cantillation),
sans toucher aux traductions FR (Beth Loubavitch) et EN (JPS 1917) déjà présentes
dans data/psalms.json.

Usage :
    python3 scripts/refetch_hebrew_with_teamim.py
"""

from __future__ import annotations
import html as html_lib
import json
import re
import sys
import time
from pathlib import Path

try:
    import requests
except ImportError:
    sys.stderr.write("pip3 install --user requests\n")
    sys.exit(1)


SEFARIA_BASE = "https://www.sefaria.org/api/v3/texts"
HEBREW_VERSION = "Miqra according to the Masorah"
ROOT = Path(__file__).resolve().parent.parent
PSALMS = ROOT / "data" / "psalms.json"


def strip_html(t: str) -> str:
    t = re.sub(r"<[^>]+>", "", t)
    t = html_lib.unescape(t)
    return re.sub(r"\s+", " ", t.replace("\xa0", " ")).strip()


def fetch_hebrew(num: int) -> list[str]:
    url = f"{SEFARIA_BASE}/Psalms.{num}"
    params = [
        ("version", f"hebrew|{HEBREW_VERSION}"),
        ("return_format", "text_only"),
    ]
    r = requests.get(url, params=params, timeout=30)
    r.raise_for_status()
    data = r.json()
    for v in data.get("versions", []):
        if v.get("language") == "he":
            text = v.get("text") or []
            if text and isinstance(text[0], list):
                text = [item for sub in text for item in sub]
            # On NE retire PAS les téamim cette fois.
            return [strip_html(x) for x in text]
    return []


def main():
    data = json.loads(PSALMS.read_text(encoding="utf-8"))
    psalms = {p["id"]: p for p in data["psalms"]}
    updated = 0
    failures = []

    for num in range(1, 151):
        try:
            verses = fetch_hebrew(num)
        except Exception as e:
            failures.append((num, str(e)))
            continue

        psalm = psalms.get(num)
        if not psalm:
            continue

        for i, v in enumerate(psalm["verses"]):
            if i < len(verses):
                v["hebrew"] = verses[i]
                updated += 1

        print(f"  ✓ Tehilim {num:3d} → {len(verses):3d} versets HE avec téamim")
        time.sleep(0.05)

    data["version"] = "2.2.0"
    data["source"]["hebrew"] = "Sefaria — Miqra according to the Masorah (avec nikud ET téamim)"

    PSALMS.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\n→ {PSALMS}")
    print(f"  Versets HE mis à jour : {updated}")
    if failures:
        print(f"  Échecs : {failures}")


if __name__ == "__main__":
    main()
