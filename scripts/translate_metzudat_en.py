#!/usr/bin/env python3
# Traduit en ANGLAIS (via Claude) le texte HÉBREU de Metzudat David sur les
# Tehilim et ajoute un champ "en" à chaque commentaire metzudat. Idempotent.
import json, os, time, urllib.request

PATH = "/Users/dadoou/TEHILIM/data/commentaries.json"
MODEL = "claude-haiku-4-5-20251001"
BATCH = 20
SYS_BATCH = ("You translate the 'Metzudat David' commentary on Psalms from Hebrew to English. "
             "Metzudat David explains the plain meaning (peshat) of the verse. Translate "
             "faithfully, in a clear and traditional register. Render the divine names "
             "(ה׳, אלהים…) as 'the Lord' or 'God' as appropriate. Keep common transliterations. "
             "Do not comment, do not add anything, do not number. "
             "Input: a JSON array of Hebrew strings. "
             "Output: ONLY a JSON array of English strings, same length, same order.")
SYS_SINGLE = ("You translate the 'Metzudat David' commentary on Psalms from Hebrew to English, "
              "explaining the plain meaning (peshat). Faithful, clear, traditional register. "
              "Render divine names as 'the Lord'/'God'. Reply ONLY with the English translation.")

def call(key, texts, single=False):
    content = texts[0] if single else json.dumps(texts, ensure_ascii=False)
    sys_prompt = SYS_SINGLE if single else SYS_BATCH
    body = {"model": MODEL, "max_tokens": 8000, "temperature": 0.2,
            "system": sys_prompt, "messages": [{"role": "user", "content": content}]}
    req = urllib.request.Request("https://api.anthropic.com/v1/messages",
        data=json.dumps(body).encode(),
        headers={"x-api-key": key, "anthropic-version": "2023-06-01", "content-type": "application/json"})
    with urllib.request.urlopen(req, timeout=120) as r:
        out = json.load(r)["content"][0]["text"].strip()
    if single:
        return out
    if not out.startswith("["):
        out = out[out.find("["):out.rfind("]")+1]
    return json.loads(out)

def main():
    key = os.environ["ANTHROPIC_API_KEY"]
    doc = json.load(open(PATH, encoding="utf-8"))
    todo = [c for p in doc["byPsalm"].values() for v in p.values()
            for c in v.get("metzudat", []) if c.get("he") and not c.get("en")]
    print(f"À traduire (Metzudat EN) : {len(todo)}")
    done = 0
    for start in range(0, len(todo), BATCH):
        chunk = todo[start:start+BATCH]
        texts = [c["he"] for c in chunk]
        try:
            en = call(key, texts)
            if len(en) != len(texts):
                raise ValueError("len mismatch")
            for c, e in zip(chunk, en):
                c["en"] = e.strip()
        except Exception:
            for c in chunk:
                for a in range(3):
                    try:
                        c["en"] = call(key, [c["he"]], single=True); break
                    except Exception:
                        time.sleep(3)
        done += len(chunk)
        if (start // BATCH) % 5 == 0:
            json.dump(doc, open(PATH, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
            print(f"… {done}/{len(todo)} (sauvegardé)")
    json.dump(doc, open(PATH, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    tot = en = 0
    for p in doc["byPsalm"].values():
        for v in p.values():
            for c in v.get("metzudat", []):
                tot += 1; en += 1 if c.get("en") else 0
    print(f"OK — Metzudat EN : {en}/{tot}")

if __name__ == "__main__":
    main()
