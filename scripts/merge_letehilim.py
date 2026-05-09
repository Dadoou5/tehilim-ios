#!/usr/bin/env python3
"""
Fusionne data/letehilim.json (Beth Loubavitch) dans data/psalms.json :
- Remplace verses[].translationFR (Louis Segond) par la traduction Loubavitch
- Ajoute verses[].commentary (commentaire Loubavitch, optionnel)
- Ajoute psalms[].intro (introduction de psaume, optionnelle)
- Met à jour les métadonnées source.

Usage : python3 scripts/merge_letehilim.py
"""

from __future__ import annotations
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PSALMS = ROOT / "data" / "psalms.json"
LETEHILIM = ROOT / "data" / "letehilim.json"
BACKUP = ROOT / "data" / "psalms.before-letehilim.json"


def main():
    psalms_data = json.loads(PSALMS.read_text(encoding="utf-8"))
    let_data = json.loads(LETEHILIM.read_text(encoding="utf-8"))

    let_by_id = {p["id"]: p for p in let_data["psalms"]}

    # Sauvegarde
    if not BACKUP.exists():
        shutil.copy(PSALMS, BACKUP)
        print(f"Backup → {BACKUP.name}")

    # Stats
    fr_replaced = 0
    com_added = 0
    intros_added = 0
    fr_missing = []

    for psalm in psalms_data["psalms"]:
        pid = psalm["id"]
        let = let_by_id.get(pid)
        if not let:
            print(f"  ! Tehilim {pid} absent de letehilim.json")
            continue

        # Intro de psaume
        if let.get("intro"):
            psalm["intro"] = let["intro"]
            intros_added += 1
        else:
            psalm["intro"] = None

        # Versets — appariement par numéro
        let_verses_by_num = {v["number"]: v for v in let["verses"]}

        for v in psalm["verses"]:
            n = v["number"]
            lv = let_verses_by_num.get(n)
            if not lv:
                fr_missing.append(f"{pid}:{n}")
                continue

            if lv.get("fr"):
                v["translationFR"] = lv["fr"]
                fr_replaced += 1
            else:
                fr_missing.append(f"{pid}:{n}")

            # Commentaire (optionnel par verset)
            v["commentary"] = lv.get("commentary")
            if lv.get("commentary"):
                com_added += 1

    # Mise à jour des métadonnées source
    psalms_data["version"] = "2.0.0"
    psalms_data["source"] = {
        "hebrew": psalms_data["source"].get("hebrew", "Sefaria — Miqra according to the Masorah (te'amim stripped, nikud kept)"),
        "french": "Beth Loubavitch — le-tehilim.online",
        "commentary": "Beth Loubavitch — le-tehilim.online",
        "license": "Texte hébreu massorétique : domaine public. Traduction française et commentaires : Beth Loubavitch (autorisation accordée). Mention obligatoire : 'Traduction et commentaires : Beth Loubavitch — le-tehilim.online'.",
        "letehilimFetchedAt": let_data["source"]["fetchedAt"],
        "notes": "Numérotation hébraïque conservée. Les commentaires citent leurs sources internes (Séfer Ha Maamarim, Or Ha Torah, Likoutei Si'hot, etc.).",
    }

    PSALMS.write_text(json.dumps(psalms_data, ensure_ascii=False, indent=2), encoding="utf-8")

    total_verses = sum(len(p["verses"]) for p in psalms_data["psalms"])
    print(f"\n→ {PSALMS}")
    print(f"  Psaumes traités    : {len(psalms_data['psalms'])}")
    print(f"  Versets total      : {total_verses}")
    print(f"  FR remplacés       : {fr_replaced}")
    print(f"  Commentaires ajoutés : {com_added}")
    print(f"  Intros ajoutés     : {intros_added}")
    if fr_missing:
        print(f"  ⚠ Versets sans FR remplacé : {len(fr_missing)} (ex. {fr_missing[:5]})")

if __name__ == "__main__":
    main()
