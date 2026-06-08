# Test de charge — flux « sélection d'un Tehilim dans une chaîne »

Test de charge **k6** + monitoring SQL pour la feature *Chaîne de Tehilim*
(Supabase Postgres + PostgREST + Auth anonyme). Reproduit fidèlement les appels
du client mobile [`ChainService.swift`](../../ios/Tehilim/Core/Services/ChainService.swift).

## Flux testé (1:1 avec l'app)

| Étape | Appel app (Swift) | Endpoint HTTP |
|------|-------------------|---------------|
| Auth anonyme | `signInAnonymously()` | `POST /auth/v1/signup` |
| Créer chaîne | `rpc("create_chain")` | `POST /rest/v1/rpc/create_chain` |
| Rejoindre | `from("chain_participants").upsert` | `POST /rest/v1/chain_participants` |
| Lire l'état | `from("chain_assignments").select` | `GET /rest/v1/chain_assignments` |
| **Sélectionner** | `from("chain_assignments").insert` | `POST /rest/v1/chain_assignments` → **201** / **409** |

Le verrou métier = **`PRIMARY KEY (chain_id, psalm_id)`** : un 2ᵉ INSERT sur le
même couple échoue (Postgres 23505 → HTTP **409**). C'est *l'*atomicité testée.

## Prérequis

- [`k6`](https://k6.io) (`brew install k6`)
- Un projet Supabase + sa **clé anon** (publique, protégée par la RLS).

## Configuration

```bash
cp tests/load/.env.example tests/load/.env
# éditer .env : SUPABASE_URL + SUPABASE_ANON_KEY
```

## Exécution (commande unique)

```bash
# Test principal : 10 chaînes × 20 VUs = 200 VUs (ramp 2m / plateau 10m / ramp 2m)
tests/load/run.sh distribution

# Test de collision : 20 users sur LA MÊME chaîne, mêmes psaumes, même instant
tests/load/run.sh collision

# Les deux dans un seul run
tests/load/run.sh both
```

Tout est paramétrable par variable d'env (voir `.env.example`) : `NUM_CHAINS`,
`USERS_PER_CHAIN`, `RAMP_UP`, `PLATEAU`, `RAMP_DOWN`, `THINK_MIN_MS`,
`THINK_MAX_MS`, `COLLISION_VUS`, `COLLISION_PSALMS`…

### Variante « répétition rapide » (smoke, ~1 min)

```bash
RAMP_UP=20s PLATEAU=30s RAMP_DOWN=10s NUM_CHAINS=3 USERS_PER_CHAIN=5 \
  tests/load/run.sh distribution
```

## Monitoring base de données (avant / pendant / après)

Exécuter ces requêtes via le SQL Editor Supabase, `psql`, ou le MCP Supabase.

| Quand | Fichier | But |
|-------|---------|-----|
| **Avant** | `sql/01_reset_stats.sql` | reset `pg_stat_statements` (attribution propre) |
| **Pendant** (échantillonner) | `sql/02_activity.sql` | connexions actives / idle / saturation des 60 conns |
| **Pendant** (échantillonner) | `sql/05_locks.sql` | verrous bloquants / requêtes en attente |
| **Après** | `sql/03_top_queries.sql` | top requêtes par temps cumulé |
| **Après** | `sql/04_top_by_calls_and_max.sql` | top par nb d'appels & par temps max |
| **Après** | `sql/06_db_health.sql` | cache hit ratio, rollbacks (=409), tailles |
| **Après** | `sql/07_consistency.sql` | **cohérence métier** (0 doublon, couverture 150) |
| **Après (collision)** | `sql/08_collision_check.sql` | **≤ 1 gagnant par (chaîne, psaume)** |
| **Cleanup** | `sql/09_cleanup.sql` | supprime toutes les données `LOADTEST_%` |

## Métriques k6 (sortie console + `results/summary_*.json`)

- Latence par endpoint (p50/p90/p95/p99/max) : `ep_auth_signup`, `ep_create_chain`,
  `ep_join_upsert`, `ep_fetch_board`, `ep_select_insert`.
- Métier : `biz_select_success` (201), `biz_select_conflict` (409),
  `biz_select_rls_denied` (401/403), `biz_select_error` (5xx/réseau),
  `biz_rate_limited_429`, `biz_auth_error`.
- Collision : `collision_success`, `collision_conflict`, `collision_other`.
- Standard k6 : `http_reqs` (débit), `http_req_duration`, `http_req_failed`.

## Sécurité / propreté

- **Toutes** les chaînes de test sont nommées `LOADTEST_*` → cleanup ciblé et sûr.
- `expires_at` est posé dans le futur proche : le cron de nettoyage les supprime
  aussi automatiquement si on oublie `09_cleanup.sql`.
- La clé utilisée est la **anon** : la RLS s'applique exactement comme en prod
  (aucun bypass service_role) — on teste donc *aussi* les policies RLS.

⚠️ **Le scénario par défaut (200 VUs/14 min) est conçu pour saturer.** Le lancer
contre un projet de **production** peut dégrader l'app pour les vrais
utilisateurs et consommer du quota. Privilégier une **branche Supabase** isolée,
ou réduire la charge (`NUM_CHAINS`, `USERS_PER_CHAIN`, `PLATEAU`).
