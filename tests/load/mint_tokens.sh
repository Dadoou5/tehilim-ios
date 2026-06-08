#!/usr/bin/env bash
# ============================================================================
# Pré-provisionne un POOL de JWT anonymes (utilisateurs Supabase) et les met en
# cache dans tokens.json. Indispensable derrière le rate-limiter de signup :
# on mint LENTEMENT une fois, puis k6 RÉUTILISE le pool (tokens valides ~1 h).
#
# Cela DÉCOUPLE la capacité d'auth (limitée, caractérisée à part) de la capacité
# DB/sélection (l'objet réel du test). Chaque VU tape quand même la base en
# concurrence réelle ; seules les identités sont partagées en round-robin.
#
# Usage :  TARGET=60 ./mint_tokens.sh   (défaut 60)
# ============================================================================
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
[[ -f "$HERE/.env" ]] && { set -a; source "$HERE/.env"; set +a; }

: "${SUPABASE_URL:?SUPABASE_URL requis}"
: "${SUPABASE_ANON_KEY:?SUPABASE_ANON_KEY requis}"
TARGET="${TARGET:-60}"
OUT="${TOKENS_FILE:-$HERE/tokens.json}"
URL="${SUPABASE_URL%/}/auth/v1/signup"

echo "▶ Mint de $TARGET tokens vers $OUT (rate-limit aware)…"
tmp="$(mktemp)"; echo "[" > "$tmp"; n=0; first=1; tries=0
while [[ $n -lt $TARGET ]]; do
  tries=$((tries+1))
  resp="$(curl -s -w '\n%{http_code}' -X POST "$URL" \
    -H "apikey: $SUPABASE_ANON_KEY" -H "Authorization: Bearer $SUPABASE_ANON_KEY" \
    -H "Content-Type: application/json" -d '{}')"
  code="$(printf '%s' "$resp" | tail -n1)"
  body="$(printf '%s' "$resp" | sed '$d')"
  if [[ "$code" == "200" ]]; then
    row="$(printf '%s' "$body" | python3 -c "import sys,json;d=json.load(sys.stdin);print(json.dumps({'jwt':d['access_token'],'uid':str(d['user']['id']).lower()}))")"
    [[ $first -eq 0 ]] && echo "," >> "$tmp"; first=0
    printf '%s' "$row" >> "$tmp"
    n=$((n+1)); printf "\r  %d/%d tokens (essais=%d)   " "$n" "$TARGET" "$tries"
  else
    # 429 / autre : on respecte le bucket → petite pause avant de réessayer.
    sleep 2
  fi
done
echo "]" >> "$tmp"; mv "$tmp" "$OUT"
echo ""; echo "✓ $n tokens écrits dans $OUT"
