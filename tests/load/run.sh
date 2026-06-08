#!/usr/bin/env bash
# ============================================================================
# Orchestrateur du test de charge Tehilim.
#   ./run.sh distribution   → test principal 200 VUs (10 chaînes × 20)
#   ./run.sh collision      → test de collision (20 users, même chaîne)
#   ./run.sh both           → les deux dans un seul run k6
# Variables : voir tests/load/.env.example (chargées depuis tests/load/.env).
# ============================================================================
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCENARIO="${1:-distribution}"
RESULTS="$HERE/results"
mkdir -p "$RESULTS"

# Charge tests/load/.env si présent (sinon on suppose les vars déjà exportées).
if [[ -f "$HERE/.env" ]]; then
  set -a; # shellcheck disable=SC1090
  source "$HERE/.env"; set +a
fi

: "${SUPABASE_URL:?SUPABASE_URL requis (voir .env.example)}"
: "${SUPABASE_ANON_KEY:?SUPABASE_ANON_KEY requis (voir .env.example)}"

# Pool de tokens pré-mintés (recommandé derrière le rate-limiter d'auth).
TOKENS_FILE="${TOKENS_FILE:-$HERE/tokens.json}"
if [[ -f "$TOKENS_FILE" ]]; then
  echo "ℹ Pool de tokens : $TOKENS_FILE ($(python3 -c "import json;print(len(json.load(open('$TOKENS_FILE'))))" 2>/dev/null || echo '?') identités réutilisées)"
  TOKENS_ARG=(-e TOKENS_FILE="$TOKENS_FILE")
else
  echo "⚠ Pas de tokens.json → signup anonyme par VU (plafonné par le rate-limiter)."
  echo "  Conseillé : TARGET=60 ./mint_tokens.sh  avant un run 200 VUs."
  TOKENS_ARG=()
fi

STAMP="$(date +%Y%m%d_%H%M%S)"
echo "▶ Scénario=$SCENARIO  cible=$SUPABASE_URL  $STAMP"
echo "  Pense à reset les stats AVANT (tests/load/sql/01_reset_stats.sql) et à"
echo "  échantillonner 02_activity.sql / 05_locks.sql PENDANT le plateau."

k6 run \
  -e SUPABASE_URL="$SUPABASE_URL" \
  -e SUPABASE_ANON_KEY="$SUPABASE_ANON_KEY" \
  -e NUM_CHAINS="${NUM_CHAINS:-10}" \
  -e USERS_PER_CHAIN="${USERS_PER_CHAIN:-20}" \
  -e RAMP_UP="${RAMP_UP:-2m}" \
  -e PLATEAU="${PLATEAU:-10m}" \
  -e RAMP_DOWN="${RAMP_DOWN:-2m}" \
  -e THINK_MIN_MS="${THINK_MIN_MS:-500}" \
  -e THINK_MAX_MS="${THINK_MAX_MS:-2000}" \
  -e SELECTION_WINDOW_S="${SELECTION_WINDOW_S:-3600}" \
  -e COLLISION_VUS="${COLLISION_VUS:-20}" \
  -e COLLISION_PSALMS="${COLLISION_PSALMS:-30}" \
  -e TEST_TAG="${TEST_TAG:-LOADTEST}" \
  -e SCENARIO="$SCENARIO" \
  "${TOKENS_ARG[@]}" \
  --summary-export "$RESULTS/summary_${SCENARIO}_${STAMP}.json" \
  "$HERE/k6/main.js" | tee "$RESULTS/console_${SCENARIO}_${STAMP}.log"

echo "✓ Terminé. Résultats : $RESULTS/"
echo "  → Vérifier la cohérence : tests/load/sql/07_consistency.sql (+ 08 pour collision)"
echo "  → Nettoyer les données  : tests/load/sql/09_cleanup.sql"
