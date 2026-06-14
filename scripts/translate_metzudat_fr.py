#!/usr/bin/env python3
# Traduit en français (via Claude) le texte HÉBREU de Metzudat David sur les
# Tehilim et ajoute un champ "fr" à chaque commentaire metzudat. Idempotent.
import json, os, time, urllib.request

PATH = "/Users/dadoou/TEHILIM/data/commentaries.json"
MODEL = "claude-haiku-4-5-20251001"
BATCH = 20
SYS = ("Tu traduis le commentaire « Metsoudat David » sur les Tehilim, de l'hébreu "
       "vers le français. Metsoudat David explique le sens simple (pchat) du verset. "
       "Traduis fidèlement, registre clair et traditionnel. Rends les Noms divins "
       "(ה׳, אלהים…) par « l'Éternel » ou « Dieu » selon le sens. Garde les "
       "translittérations usuelles. Ne commente pas, n'ajoute rien, ne numérote pas. "
       "Entrée : un tableau JSON de chaînes hébraïques. "
       "Sortie : UNIQUEMENT un tableau JSON de chaînes françaises, même longueur, même ordre.")

def call(key, texts, single=False):
    content = texts if not single else texts[0]
    if not single:
        content = json.dumps(texts, ensure_ascii=False)
    sys_prompt = SYS if not single else SYS.replace(
        "Entrée : un tableau JSON de chaînes hébraïques. "
        "Sortie : UNIQUEMENT un tableau JSON de chaînes françaises, même longueur, même ordre.",
        "Réponds UNIQUEMENT par la traduction française.")
    body = {"model": MODEL, "max_tokens": 8000, "temperature": 0.2,
            "system": sys_prompt, "messages": [{"role": "user", "content": content}]}
    req = urllib.request.Request("https://api.anthropic.com/v1/messages",
        data=json.dumps(body).encode(),
        headers={"x-api-key": key, "anthropic-version": "2023-06-01", "content-type": "application/json"})
    with urllib.request.urlopen(req, timeout=120) as r:
        out = r and json.load(r)["content"][0]["text"].strip()
    if single:
        return out
    if not out.startswith("["):
        out = out[out.find("["):out.rfind("]")+1]
    return json.loads(out)

def main():
    key = os.environ["ANTHROPIC_API_KEY"]
    doc = json.load(open(PATH, encoding="utf-8"))
    todo = [c for p in doc["byPsalm"].values() for v in p.values()
            for c in v.get("metzudat", []) if c.get("he") and not c.get("fr")]
    print(f"À traduire (Metzudat) : {len(todo)}")
    done = 0
    for start in range(0, len(todo), BATCH):
        chunk = todo[start:start+BATCH]
        texts = [c["he"] for c in chunk]
        try:
            fr = call(key, texts)
            if len(fr) != len(texts):
                raise ValueError("len mismatch")
            for c, f in zip(chunk, fr):
                c["fr"] = f.strip()
        except Exception:
            # repli un par un (robuste)
            for c in chunk:
                for a in range(3):
                    try:
                        c["fr"] = call(key, [c["he"]], single=True); break
                    except Exception:
                        time.sleep(3)
        done += len(chunk)
        if (start // BATCH) % 5 == 0:
            json.dump(doc, open(PATH, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
            print(f"… {done}/{len(todo)} (sauvegardé)")
    json.dump(doc, open(PATH, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    tot = fr = 0
    for p in doc["byPsalm"].values():
        for v in p.values():
            for c in v.get("metzudat", []):
                tot += 1; fr += 1 if c.get("fr") else 0
    print(f"OK — Metzudat FR : {fr}/{tot}")

if __name__ == "__main__":
    main()
