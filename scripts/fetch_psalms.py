#!/usr/bin/env python3
"""
Fetch the 150 Psalms.

- Hebrew  (Sefaria):  Miqra according to the Masorah, te'amim stripped, nikud kept.
- French  (bolls.life): Bible Louis Segond 1910 (FRLSG) — domaine public.

Note : la numérotation chrétienne (Segond) intègre la "superscription" dans
le titre du psaume, donc certains psaumes ont 1 ou 2 versets de moins en FR
qu'en hébreu. Le script aligne automatiquement par offset.

Sortie: data/psalms.json conforme au schéma de l'app.
"""

from __future__ import annotations
import json, re, sys, time
from pathlib import Path

try:
    import requests
except ImportError:
    sys.stderr.write("pip3 install --user -r scripts/requirements.txt\n")
    sys.exit(1)


SEFARIA_BASE = "https://www.sefaria.org/api/v3/texts"
# Note V1.7.4 : on conserve les téamim (cantillation) cette fois.
HEBREW_VERSION = "Miqra according to the Masorah"

BOLLS_BASE = "https://bolls.life/get-text/FRLSG"
BOLLS_PSALMS_BOOK = 19  # Protestant book number

OUTPUT = Path(__file__).resolve().parent.parent / "data" / "psalms.json"

# --- Hebrew utilities ---

TEAMIM_RANGE = range(0x0591, 0x05B0)

def strip_teamim(t: str) -> str:
    return "".join(c for c in t if ord(c) not in TEAMIM_RANGE)

def strip_html(t: str) -> str:
    t = re.sub(r"<[^>]+>", "", t)
    return t.replace("\xa0", " ").strip()

HEB_LETTERS = [(400,"ת"),(300,"ש"),(200,"ר"),(100,"ק"),
               (90,"צ"),(80,"פ"),(70,"ע"),(60,"ס"),
               (50,"נ"),(40,"מ"),(30,"ל"),(20,"כ"),(10,"י"),
               (9,"ט"),(8,"ח"),(7,"ז"),(6,"ו"),(5,"ה"),(4,"ד"),(3,"ג"),(2,"ב"),(1,"א")]

def to_hebrew_numeral(n: int) -> str:
    if n <= 0: return ""
    out, r = "", n
    for v, l in HEB_LETTERS:
        if v < 100: break
        while r >= v: out += l; r -= v
    if r == 15: return out + "טו"
    if r == 16: return out + "טז"
    for v, l in HEB_LETTERS:
        if v >= 100: continue
        while r >= v: out += l; r -= v
    return out

def book_for_psalm(n: int) -> int:
    return 1 if n<=41 else 2 if n<=72 else 3 if n<=89 else 4 if n<=106 else 5

# --- Sefaria ---

def fetch_hebrew(num: int) -> list[str]:
    url = f"{SEFARIA_BASE}/Psalms.{num}"
    params = [("version", f"hebrew|{HEBREW_VERSION}"),
              ("return_format", "text_only")]
    r = requests.get(url, params=params, timeout=30)
    r.raise_for_status()
    data = r.json()
    text = []
    for v in data.get("versions", []):
        if v.get("language") == "he":
            text = v.get("text") or []
            break
    if text and isinstance(text[0], list):
        text = [item for sub in text for item in sub]
    # V1.7.4 : on conserve les téamim (cantillation) — Ezra SIL SR les rend correctement.
    return [strip_html(t) for t in text]

# --- Bolls (LSG) ---

def fetch_french(num: int) -> list[str]:
    url = f"{BOLLS_BASE}/{BOLLS_PSALMS_BOOK}/{num}/"
    r = requests.get(url, timeout=30)
    r.raise_for_status()
    data = r.json()
    if isinstance(data, list):
        return [strip_html(v.get("text", "")) for v in data]
    return []

# --- Alignment ---

def align(he: list[str], fr: list[str]) -> list[str | None]:
    """Pour chaque verset hébreu, renvoie la traduction FR alignée (ou None).
    Gère les décalages de superscription (Segond combine le titre dans v.1)."""
    n_he = len(he)
    n_fr = len(fr)
    if n_he == n_fr:
        return [fr[i] or None for i in range(n_he)]
    diff = n_he - n_fr
    if 1 <= diff <= 2:
        # Versets hébreux 1..diff = pas de FR direct (titre absorbé dans le v.1 chrétien)
        result = []
        for i in range(n_he):
            j = i - diff
            if j < 0:
                result.append(None)
            elif j < n_fr:
                result.append(fr[j] or None)
            else:
                result.append(None)
        return result
    if diff < 0:
        # Cas rare : FR a plus de versets — on tronque
        return [fr[i] or None for i in range(min(n_he, n_fr))] + [None]*max(0, n_he-n_fr)
    # Décalage anormal — on laisse FR=None pour signaler
    return [None]*n_he

# --- Main ---

def main():
    psalms = []
    he_total = 0
    fr_total = 0
    misses = []

    for num in range(1, 151):
        try:
            he = fetch_hebrew(num)
        except Exception as e:
            print(f"  x Psalm {num} HE error: {e}")
            continue
        try:
            fr = fetch_french(num)
        except Exception as e:
            print(f"  ! Psalm {num} FR error: {e}")
            fr = []

        fr_aligned = align(he, fr) if fr else [None]*len(he)

        verses = []
        for i, (h, f) in enumerate(zip(he, fr_aligned), start=1):
            verses.append({
                "id": f"{num}:{i}",
                "number": i,
                "hebrewNumber": to_hebrew_numeral(i),
                "hebrew": h,
                "translationFR": f,
            })
            he_total += 1
            if f: fr_total += 1

        psalms.append({
            "id": num,
            "book": book_for_psalm(num),
            "hebrewNumber": to_hebrew_numeral(num),
            "hebrewTitle": None,
            "tags": [],
            "verses": verses,
        })

        fr_count = sum(1 for v in verses if v["translationFR"])
        flag = " " if fr_count == len(verses) else ("Δ" if fr_count else "?")
        print(f"  {flag} Psalm {num:3d}: HE {len(verses):3d}, FR {fr_count}/{len(verses)}")
        if fr_count == 0 and fr:
            misses.append(num)
        time.sleep(0.05)

    output = {
        "version": "1.0.0",
        "source": {
            "hebrew": f"Sefaria — {HEBREW_VERSION} (te'amim stripped, nikud kept)",
            "french": "bolls.life — Bible Louis Segond 1910 (FRLSG, domaine public)",
            "license": "Texte hébreu massorétique : domaine public. Louis Segond 1910 : domaine public.",
            "notes": "Numérotation hébraïque conservée. Pour certains psaumes, les versets hébreux 1 (ou 1–2) n'ont pas de traduction FR alignée car la superscription est intégrée au titre dans la tradition chrétienne.",
        },
        "language": {"primary": "he", "translations": ["fr"]},
        "psalms": psalms,
    }

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(output, ensure_ascii=False, indent=2), encoding="utf-8")

    print()
    print(f"Wrote {OUTPUT}")
    print(f"  Psaumes : {len(psalms)}/150")
    print(f"  Versets hébreu : {he_total}")
    print(f"  Versets FR     : {fr_total} ({fr_total*100//max(he_total,1)}%)")
    if misses:
        print(f"  Psaumes sans aucune traduction FR : {misses}")

if __name__ == "__main__":
    main()
