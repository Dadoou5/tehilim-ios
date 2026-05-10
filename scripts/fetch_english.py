#!/usr/bin/env python3
"""
Récupère la traduction anglaise des Tehilim depuis Sefaria et l'ajoute à psalms.json.

Source : Sefaria — JPS 1917 (The Holy Scriptures: A New Translation), domaine public.
Numérotation : Sefaria utilise la numérotation hébraïque/juive ; alignement direct
avec le texte hébreu déjà présent dans psalms.json (pas de décalage à gérer).

Usage :
    pip3 install --user requests
    python3 scripts/fetch_english.py
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


SEFARIA_BASE = "https://www.sefaria.org/api/v3/texts"
ENGLISH_CANDIDATES = [
    "The Holy Scriptures: A New Translation (JPS 1917)",
    "JPS 1917",
    "Tanakh: The Holy Scriptures, published by JPS",
]

ROOT = Path(__file__).resolve().parent.parent
PSALMS_PATH = ROOT / "data" / "psalms.json"


def strip_html(s: str) -> str:
    s = re.sub(r"<[^>]+>", "", s)
    s = html_lib.unescape(s)
    s = s.replace("\xa0", " ")
    return re.sub(r"\s+", " ", s).strip()


def list_versions() -> list[dict]:
    try:
        r = requests.get("https://www.sefaria.org/api/texts/versions/Psalms", timeout=30)
        r.raise_for_status()
        return r.json()
    except Exception as e:
        print(f"  ! versions endpoint failed: {e}")
        return []


def pick_english_version(versions: list[dict]) -> str | None:
    en_versions = [v for v in versions if v.get("language") == "en"]
    titles = [v.get("versionTitle", "") for v in en_versions]
    for cand in ENGLISH_CANDIDATES:
        for t in titles:
            if cand.lower() in t.lower():
                return t
    return titles[0] if titles else None


def fetch_psalm_english(num: int, version: str | None) -> list[str]:
    url = f"{SEFARIA_BASE}/Psalms.{num}"
    params = []
    if version:
        params.append(("version", f"english|{version}"))
    else:
        params.append(("version", "english"))
    params.append(("return_format", "text_only"))
    r = requests.get(url, params=params, timeout=30)
    r.raise_for_status()
    data = r.json()
    for v in data.get("versions", []):
        lang = v.get("language") or v.get("actualLanguage") or ""
        if lang == "en":
            text = v.get("text") or []
            if text and isinstance(text[0], list):
                text = [item for sub in text for item in sub]
            return [strip_html(t) for t in text]
    return []


def main():
    print("Listing Sefaria English versions for Psalms…")
    versions = list_versions()
    en_v = pick_english_version(versions) if versions else None
    print(f"  English version: {en_v or 'default'}")

    data = json.loads(PSALMS_PATH.read_text(encoding="utf-8"))
    psalms_by_id = {p["id"]: p for p in data["psalms"]}

    en_total = 0
    he_total = 0
    failures = []

    for num in range(1, 151):
        try:
            verses_en = fetch_psalm_english(num, en_v)
        except Exception as e:
            print(f"  x Psalm {num} HTTP error: {e}")
            failures.append(num)
            continue

        psalm = psalms_by_id.get(num)
        if not psalm:
            continue

        n_he = len(psalm["verses"])
        n_en = len(verses_en)
        he_total += n_he
        diff = n_he - n_en

        for i, v in enumerate(psalm["verses"]):
            j = i  # même numérotation côté Sefaria
            if j < n_en:
                en_text = verses_en[j]
                v["translationEN"] = en_text if en_text else None
                if en_text:
                    en_total += 1
            else:
                v["translationEN"] = None

        flag = "✓" if (n_en == n_he) else "Δ"
        filled = sum(1 for v in psalm["verses"] if v.get("translationEN"))
        print(f"  {flag} Tehilim {num:3d}: HE {n_he:3d} · EN {n_en:3d} (filled {filled})")
        time.sleep(0.05)

    # Mise à jour des métadonnées
    data["version"] = "2.1.0"
    data["source"]["english"] = f"Sefaria — {en_v or 'default English'} (domaine public)"
    notes = data["source"].get("notes", "")
    if "anglaise" not in notes:
        data["source"]["notes"] = (notes + " Traduction anglaise ajoutée en V2.1 (JPS 1917).").strip()

    PSALMS_PATH.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"\n→ {PSALMS_PATH}")
    print(f"  Versets hébreu : {he_total}")
    print(f"  Versets EN     : {en_total} ({en_total*100//max(he_total,1)}%)")
    if failures:
        print(f"  Échecs HTTP    : {failures}")


if __name__ == "__main__":
    main()
