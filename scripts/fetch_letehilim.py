#!/usr/bin/env python3
"""
Récupère la traduction française et les commentaires depuis le-tehilim.online.

Source : Beth Loubavitch (8 rue Lamartine, 75009 Paris — chabad@loubavitch.fr).
Autorisation : accordée explicitement au porteur du projet.
Mention obligatoire dans l'app : "Traduction et commentaires : Beth Loubavitch / le-tehilim.online".

Usage :
    pip3 install --user requests
    python3 scripts/fetch_letehilim.py
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
    sys.stderr.write("Run: pip3 install --user requests\n")
    sys.exit(1)


BASE_URL = "https://le-tehilim.online/tehilim.php"

BOOK_RANGES = [
    ("Premier livre",   1,   41),
    ("Deuxième livre",  42,  72),
    ("Troisième livre", 73,  89),
    ("Quatrième livre", 90,  106),
    ("Cinquième livre", 107, 150),
]

OUTPUT = Path(__file__).resolve().parent.parent / "data" / "letehilim.json"

# -- Helpers ---------------------------------------------------------------------------

def book_for_psalm(n: int) -> str:
    for name, lo, hi in BOOK_RANGES:
        if lo <= n <= hi:
            return name
    raise ValueError(f"Psaume hors plage : {n}")

def fetch_psalm_html(num: int) -> str:
    params = {
        "sefer": book_for_psalm(num),
        "perek": f"Psaume {num}",
        "fr": "true", "he": "true", "com": "true",
        "jour": "0",
    }
    r = requests.get(BASE_URL, params=params, timeout=30)
    r.raise_for_status()
    return r.text

# Capture chaque <div class="X" ...>...</div> non imbriqué (les blocs du site sont plats)
DIV_RE = re.compile(
    r'<div[^>]*class="(francais|hebreu|commentaire)"[^>]*>(.*?)</div>',
    re.DOTALL | re.IGNORECASE,
)
VERSE_NUM_RE = re.compile(r'^\(\s*(\d+)\s*\)\s*')

def clean_text(raw: str) -> str:
    txt = re.sub(r'<[^>]+>', '', raw)
    txt = html_lib.unescape(txt)
    txt = txt.replace('\xa0', ' ').replace('‏', '').replace('‎', '')
    return re.sub(r'\s+', ' ', txt).strip()

# -- Parsing --------------------------------------------------------------------------

def parse_psalm(html: str):
    """Renvoie (intro, [{number, fr, commentary}])."""
    intro_text = None
    versets_fr: dict[int, str] = {}
    versets_com: dict[int, str] = {}

    for cls, content in DIV_RE.findall(html):
        text = clean_text(content)
        if not text:
            continue

        m = VERSE_NUM_RE.match(text)
        if m:
            n = int(m.group(1))
            body = text[m.end():].strip()
            if cls == "francais":
                versets_fr[n] = body
            elif cls == "commentaire":
                versets_com[n] = body
            # On ne récupère pas l'hébreu (déjà dans psalms.json depuis Sefaria)
        else:
            # Bloc sans numéro de verset = intro (premier rencontré seulement)
            if cls == "francais" and intro_text is None:
                intro_text = text

    all_numbers = sorted(set(versets_fr.keys()) | set(versets_com.keys()))
    verses = [
        {
            "number": n,
            "fr": versets_fr.get(n),
            "commentary": versets_com.get(n) or None,
        }
        for n in all_numbers
    ]
    return intro_text, verses

# -- Main -----------------------------------------------------------------------------

def main():
    psalms_out = []
    fr_total = 0
    com_total = 0
    intros = 0
    failures = []

    for num in range(1, 151):
        try:
            html = fetch_psalm_html(num)
        except Exception as e:
            print(f"  x Psalm {num} HTTP error: {e}")
            failures.append(num)
            continue

        intro, verses = parse_psalm(html)
        psalms_out.append({
            "id": num,
            "intro": intro,
            "verses": verses,
        })

        v_fr = sum(1 for v in verses if v["fr"])
        v_com = sum(1 for v in verses if v["commentary"])
        fr_total += v_fr
        com_total += v_com
        if intro:
            intros += 1

        flag = "✓" if v_fr == len(verses) else "Δ"
        print(f"  {flag} Tehilim {num:3d}: {len(verses):3d} versets · FR {v_fr} · COM {v_com} · intro {'Y' if intro else 'N'}")
        time.sleep(0.1)  # politesse réseau

    output = {
        "version": "1.0.0",
        "source": {
            "name": "Beth Loubavitch",
            "url": "https://le-tehilim.online",
            "address": "8 rue Lamartine, 75009 Paris",
            "contact": "chabad@loubavitch.fr",
            "license": "Autorisation accordée par le Beth Loubavitch pour intégration dans l'app Tehilim. Mention obligatoire dans l'app : 'Traduction et commentaires : Beth Loubavitch — le-tehilim.online'.",
            "fetchedAt": time.strftime("%Y-%m-%d"),
        },
        "psalms": psalms_out,
    }

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(output, ensure_ascii=False, indent=2), encoding="utf-8")

    total = sum(len(p["verses"]) for p in psalms_out)
    print(f"\n→ {OUTPUT}")
    print(f"  Psaumes        : {len(psalms_out)}/150")
    print(f"  Versets total  : {total}")
    print(f"  Versets FR     : {fr_total}")
    print(f"  Commentaires   : {com_total}")
    print(f"  Intros         : {intros}")
    if failures:
        print(f"  Échecs HTTP    : {failures}")

if __name__ == "__main__":
    main()
