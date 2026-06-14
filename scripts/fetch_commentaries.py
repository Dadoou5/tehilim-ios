#!/usr/bin/env python3
# Récupère Rashi (he+en) et Metzudat David (he) sur les Tehilim depuis Sefaria
# (CC-BY / domaine public), et produit data/commentaries.json bundlé dans l'app.
import json, re, time, urllib.request, os

OUT = "/Users/dadoou/TEHILIM/data/commentaries.json"
SOURCES = [
    ("rashi", "Rashi_on_Psalms"),
    ("metzudat", "Metzudat_David_on_Psalms"),
]
TAG = re.compile(r"<[^>]+>")
WS = re.compile(r"[ \t]+")

def clean(s: str) -> str:
    s = s.replace("‎", "").replace("‏", "")
    s = TAG.sub("", s)
    s = s.replace("\n", " ").strip()
    return WS.sub(" ", s)

def split_lemma(raw: str):
    """Sépare le dibour hamatchil (premier <b>…</b>) du corps."""
    m = re.match(r"\s*<b>(.*?)</b>(.*)", raw, re.S)
    if m:
        return clean(m.group(1)).rstrip(".:"), clean(m.group(2))
    return "", clean(raw)

def fetch(ref: str):
    url = f"https://www.sefaria.org/api/texts/{ref}?context=0&pad=0&commentary=0"
    req = urllib.request.Request(url, headers={"User-Agent": "TehilimApp/1.0"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)

def comments_for_verse(he_list, en_list, vi):
    """Retourne la liste de commentaires {lemma, he, en} pour le verset index vi."""
    he_v = he_list[vi] if vi < len(he_list) else []
    en_v = en_list[vi] if (en_list and vi < len(en_list)) else []
    if isinstance(he_v, str): he_v = [he_v]
    if isinstance(en_v, str): en_v = [en_v]
    out = []
    for ci, raw in enumerate(he_v):
        if not raw or not clean(raw): continue
        lemma, body = split_lemma(raw)
        item = {"he": body}
        if lemma: item["lemma"] = lemma
        en_raw = en_v[ci] if ci < len(en_v) else ""
        if en_raw and clean(en_raw):
            _, en_body = split_lemma(en_raw)
            item["en"] = en_body
        out.append(item)
    return out

def main():
    by_psalm = {}
    for chapter in range(1, 151):
        verse_map = {}
        for key, ref in SOURCES:
            try:
                d = fetch(f"{ref}.{chapter}")
            except Exception as e:
                print(f"  ! {ref}.{chapter}: {e}")
                continue
            he = d.get("he") or []
            en = d.get("text") or []
            for vi in range(len(he)):
                cs = comments_for_verse(he, en, vi)
                if not cs: continue
                vnum = str(vi + 1)
                verse_map.setdefault(vnum, {})[key] = cs
            time.sleep(0.15)
        if verse_map:
            by_psalm[str(chapter)] = verse_map
        if chapter % 10 == 0:
            print(f"… {chapter}/150")
    doc = {
        "version": "1.0",
        "sources": {
            "rashi": {
                "he": "רש\"י על תהילים",
                "credit": "Rashi on Psalms — The Judaica Press complete Tanach with Rashi, "
                          "trans. A. J. Rosenberg, via Sefaria (CC-BY)",
            },
            "metzudat": {
                "he": "מצודת דוד על תהילים",
                "credit": "Metzudat David on Psalms — domaine public, via Sefaria",
            },
        },
        "byPsalm": by_psalm,
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(doc, f, ensure_ascii=False, separators=(",", ":"))
    sz = os.path.getsize(OUT)
    nverses = sum(len(v) for v in by_psalm.values())
    print(f"OK — {len(by_psalm)} psaumes, {nverses} versets commentés, {sz//1024} Ko")

if __name__ == "__main__":
    main()
