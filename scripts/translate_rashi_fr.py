#!/usr/bin/env python3
# Traduit en français (via Claude) la traduction anglaise de Rashi déjà présente
# dans data/commentaries.json, et ajoute un champ "fr" à chaque commentaire Rashi.
# Idempotent : ne retraduit pas ce qui a déjà un "fr". Sauvegarde régulièrement.
import json, os, time, urllib.request, urllib.error

PATH = "/Users/dadoou/TEHILIM/data/commentaries.json"
MODEL = "claude-haiku-4-5-20251001"
BATCH = 25
SYS = ("Tu es un traducteur de commentaires bibliques juifs (Rachi sur les Tehilim). "
       "Traduis fidèlement de l'anglais vers le français, dans un registre soigné et "
       "traditionnel. Rends « the Lord », « God », « the Holy One » par « l'Éternel » "
       "(ou « le Saint, béni soit-Il » selon le sens). Garde les translittérations "
       "hébraïques usuelles. Ne commente pas, n'ajoute rien, ne numérote pas. "
       "Entrée : un tableau JSON de chaînes anglaises. "
       "Sortie : UNIQUEMENT un tableau JSON de chaînes françaises, même longueur, même ordre.")

def call(key, texts):
    body = {
        "model": MODEL, "max_tokens": 8000, "temperature": 0.2,
        "system": SYS,
        "messages": [{"role": "user", "content": json.dumps(texts, ensure_ascii=False)}],
    }
    req = urllib.request.Request(
        "https://api.anthropic.com/v1/messages",
        data=json.dumps(body).encode(),
        headers={"x-api-key": key, "anthropic-version": "2023-06-01",
                 "content-type": "application/json"})
    with urllib.request.urlopen(req, timeout=120) as r:
        out = json.load(r)["content"][0]["text"].strip()
    # Robustesse : isole le tableau JSON.
    if not out.startswith("["):
        i, j = out.find("["), out.rfind("]")
        out = out[i:j+1]
    return json.loads(out)

def main():
    key = os.environ["ANTHROPIC_API_KEY"]
    doc = json.load(open(PATH, encoding="utf-8"))
    # Collecte des commentaires Rashi à traduire (en présent, fr absent).
    todo = []
    for p in doc["byPsalm"].values():
        for v in p.values():
            for c in v.get("rashi", []):
                if c.get("en") and not c.get("fr"):
                    todo.append(c)
    print(f"À traduire : {len(todo)} commentaires Rashi")
    done = 0
    for start in range(0, len(todo), BATCH):
        chunk = todo[start:start+BATCH]
        texts = [c["en"] for c in chunk]
        for attempt in range(3):
            try:
                fr = call(key, texts)
                if len(fr) != len(texts):
                    raise ValueError(f"longueur {len(fr)} != {len(texts)}")
                for c, f in zip(chunk, fr):
                    c["fr"] = f.strip()
                break
            except Exception as e:
                print(f"  retry {attempt+1} (batch @{start}): {e}")
                time.sleep(3 * (attempt + 1))
        else:
            print(f"  ! échec batch @{start}, laissé sans fr")
        done += len(chunk)
        if start // BATCH % 5 == 0:
            json.dump(doc, open(PATH, "w", encoding="utf-8"),
                      ensure_ascii=False, separators=(",", ":"))
            print(f"… {done}/{len(todo)} (sauvegardé)")
    json.dump(doc, open(PATH, "w", encoding="utf-8"),
              ensure_ascii=False, separators=(",", ":"))
    # Stat finale
    tot = fr = 0
    for p in doc["byPsalm"].values():
        for v in p.values():
            for c in v.get("rashi", []):
                if c.get("en"):
                    tot += 1; fr += 1 if c.get("fr") else 0
    print(f"OK — Rashi FR : {fr}/{tot}")

if __name__ == "__main__":
    main()
