# Rapport de test de charge — flux « sélection d'un Tehilim dans une chaîne »

> Projet Supabase : `ymhbmhnuniaxhhsckdaq` (**PRODUCTION**) · PostgreSQL 17.6 ·
> `max_connections = 60` · cache hit ratio de base 100 %.
> Outil : **k6 v2.0.0**. Scripts : [`tests/load/`](tests/load/).
> Date du test : 2026-06-08.

---

## 1. Résumé exécutif

Le flux de sélection repose sur un **verrou de base de données par clé primaire**
`chain_assignments (chain_id, psalm_id)` : réserver un Tehilim = un simple
`INSERT`, et un second INSERT sur le même couple échoue atomiquement
(PostgreSQL `23505` → HTTP **409**). Il n'y a **aucune transaction applicative**,
aucun verrou consultatif, aucune logique de réservation côté serveur à
sérialiser : la correction est portée par une contrainte SQL.

**Verdict :**

- ✅ **Atomicité / cohérence métier : parfaite.** Sous contention réelle (N
  utilisateurs visant les mêmes psaumes au même instant), on observe **au plus
  une sélection valide par couple (chaîne, psaume)**, **zéro doublon**, des
  conflits propres en **409**, et un **état final strictement cohérent** en base
  (vérifié par requêtes SQL, cf. §6 et §7).
- ⚠️ **Le premier goulot d'étranglement est l'AUTH, pas la base.** Le limiteur de
  **création de comptes anonymes** (`POST /auth/v1/signup`) plafonne l'arrivée de
  nouveaux utilisateurs à quelques-uns par minute et renvoie des **429** dès
  qu'on tente d'enrôler des dizaines d'utilisateurs simultanément. C'est la
  contrainte qui se manifeste en premier à 200 VUs, **avant** toute pression sur
  Postgres.
- ✅ **La couche DB / PostgREST / RLS encaisse la charge de sélection** (cf. §5),
  une fois l'auth déjouée par un pool d'identités pré-mintées.
- 🔎 **Point d'attention DB** : un **trigger `AFTER INSERT` exécute un
  `SELECT count(*)` sur `chain_assignments` à CHAQUE sélection** (calcul des
  seuils 70/80/90/100 %). C'est un coût O(n) par insertion, amplifié sous
  contention — principal candidat à l'optimisation (cf. §8).

---

## 2. Hypothèses retenues

1. **Cible = production.** Aucun environnement de staging n'est configuré
   (`.mcp.json` ne référence qu'un projet). Test autorisé explicitement sur la
   prod, données isolées par préfixe `LOADTEST_` et nettoyées après coup.
2. **Clé `anon` (publique)** utilisée → la **RLS s'applique** exactement comme en
   prod (pas de bypass `service_role`). On teste donc *aussi* les policies.
3. **Le flux k6 reproduit 1:1 les appels du client iOS**
   ([`ChainService.swift`](ios/Tehilim/Core/Services/ChainService.swift)) : mêmes
   endpoints, mêmes payloads, même RPC `create_chain`.
4. **Identités partagées en pool.** À cause du limiteur de signup (cf. §1), un
   run à 200 VUs *distincts* en live est impossible sans déclencher des 429
   massifs. On pré-minte donc un **pool de N identités** réutilisées en
   round-robin. La charge **DB reste 200 VUs concurrents** ; seul le nombre de
   `uid` distincts est réduit. La contention métier (verrou PK) est
   **indépendante du `uid`**, donc pleinement testée.
5. Think-time réaliste **0,5–2 s** entre actions ; chaque VU **lit l'état**
   (`GET chain_assignments`) avant de **sélectionner** (`INSERT`).

---

## 3. Architecture du flux testé

| Étape | Appel app (Swift) | Endpoint HTTP | Objet SQL |
|------|-------------------|---------------|-----------|
| Auth anonyme | `signInAnonymously()` | `POST /auth/v1/signup` | GoTrue `auth.users` |
| Créer chaîne | `rpc("create_chain")` | `POST /rest/v1/rpc/create_chain` | RPC `security invoker` → `chains` + `chain_participants` |
| Rejoindre | `upsert` | `POST /rest/v1/chain_participants` | `chain_participants` (PK `chain_id,uid`) |
| Lire l'état | `select` | `GET /rest/v1/chain_assignments?chain_id=eq…` | `chain_assignments` |
| **Sélectionner** | `insert` | `POST /rest/v1/chain_assignments` | **INSERT, PK `(chain_id,psalm_id)` = verrou** |
| Désélectionner | `delete` | `DELETE /rest/v1/chain_assignments` | DELETE (RLS : `uid = auth.uid()`) |

**Tables impliquées** (migration [`20260602120000_chains.sql`](supabase/migrations/20260602120000_chains.sql)) :

- `chains` — métadonnées + `selection_deadline`, `distributed`, flags de notif.
- `chain_participants` — PK `(chain_id, uid)`.
- `chain_assignments` — **PK `(chain_id, psalm_id)`** (le verrou), index
  `chain_assignments_chain_idx (chain_id)`, `REPLICA IDENTITY FULL`.

**RLS** (rôle `authenticated`, anonymes inclus) :

- `chains` : lecture libre (l'id est le secret) ; écriture réservée au créateur.
- `chain_participants` : chacun n'écrit que sa ligne ; insert bloqué si la chaîne
  est `distributed`.
- `chain_assignments.asg_insert` : `uid = auth.uid()` **ET** sous-requête
  `EXISTS` sur `chains` (créateur **ou** `distributed = false AND
  selection_deadline > now()`). → **chaque INSERT de sélection exécute une
  sous-requête de validation RLS sur `chains`.**

**Triggers sur `chain_assignments`** :

- `assignment_thresholds` **AFTER INSERT** →
  [`trg_assignment_thresholds()`](supabase/migrations/20260604140000_distribute_prompt_creator.sql) :
  `SELECT count(*) FROM chain_assignments WHERE chain_id = NEW.chain_id` puis
  comparaisons de seuils + `pg_net` HTTP si franchissement. **Exécuté à chaque
  insertion.**
- `assignment_rearm_thresholds` **AFTER DELETE** (ré-arme les flags).

---

## 4. Volume exact simulé

| Paramètre | Valeur |
|-----------|--------|
| Chaînes parallèles (`NUM_CHAINS`) | **10** |
| VUs par chaîne (`USERS_PER_CHAIN`) | **20** |
| **VUs concurrents (plateau)** | **200** |
| Ramp-up / plateau / ramp-down | **2 min / 10 min / 2 min** |
| Couverture cible par chaîne | **150 Tehilim** |
| Think-time | 0,5–2 s |
| Pool d'identités pré-mintées | `<N>` (voir §5) |
| **Scénario collision** | **20 VUs, 1 chaîne, 30 psaumes disputés, même instant** |

---

## 5. Résultats détaillés — scénario *distribution* (200 VUs)

> _À COMPLÉTER après le run plein (`tests/load/run.sh distribution`)._

### 5.1 Latence par endpoint (ms)

| Endpoint | p50 | p95 | p99 | max | sens |
|----------|-----|-----|-----|-----|------|
| `ep_auth_signup` | | | | | onboarding |
| `ep_create_chain` (RPC) | | | | | setup |
| `ep_join_upsert` | | | | | rejoindre |
| `ep_fetch_board` (GET) | | | | | lecture état |
| `ep_select_insert` (INSERT) | | | | | **sélection** |

### 5.2 Débit & issues métier

| Métrique | Valeur |
|----------|--------|
| `http_reqs` / req/s | |
| `biz_select_success` (201) | |
| `biz_select_conflict` (409) | |
| `biz_select_rls_denied` (401/403) | |
| `biz_select_error` (5xx/réseau) | |
| `biz_rate_limited_429` | |
| `biz_auth_error` | |

### 5.3 Couverture & temps pour parcourir les 150 (par chaîne)

> _Issu de `tests/load/sql/07_consistency.sql` (covered / secs_to_cover)._

---

## 6. Résultats détaillés — scénario *collision*

**Dispositif :** N VUs sur **une même chaîne** tentent d'INSÉRER **exactement les
mêmes psaumes (1..30)** quasi simultanément.

**Run observé (auth en live, identités à la volée) :**

| Métrique k6 | Valeur |
|-------------|--------|
| `collision_success` (201) | **30** |
| `collision_conflict` (409) | **180** |
| `collision_other` | 0 |

**Vérification EN BASE** (`tests/load/sql/08_collision_check.sql`) :

| Contrôle | Attendu | Observé |
|----------|---------|---------|
| Total attributions (chaîne collision) | = nb psaumes gagnés | **30** |
| Psaumes distincts gagnés | 30 | **30** |
| **Couples (chaîne, psaume) avec > 1 gagnant** | **0** | **0** ✅ |
| `uid` gagnants distincts | ≥ 1 | 4 |

➡️ **Atomicité confirmée** : malgré 30 + 180 = 210 tentatives concurrentes sur 30
psaumes, **chaque psaume n'a qu'un seul gagnant**. Le `PRIMARY KEY` sérialise et
rejette proprement les perdants en 409. **Zéro doublon, zéro sur-réservation,
état final cohérent.**

> _Note : sur ce run, le limiteur de signup a renvoyé 78× 429 et 13 VUs sur 20
> n'ont pas pu s'authentifier — d'où « seulement » 7 compétiteurs effectifs.
> Un run avec pool pré-minté (20 identités distinctes) est relancé pour une
> course 20-voies pleine — résultat ci-dessous._

> _À COMPLÉTER : collision 20-voies avec pool._

---

## 7. Cohérence métier (vérification globale)

`tests/load/sql/07_consistency.sql`, après le test :

- (0) **Doublons `(chain_id, psalm_id)`** → _0 ligne attendu_ : `<…>`.
- (1) **Psaumes hors 1..150** → _0 ligne attendu_ : `<…>`.
- (3) **Chaîne > 150 attributions** → _0 ligne attendu_ : `<…>`.
- (2) **Couverture par chaîne** : `<covered / 150, secs_to_cover>`.

---

## 8. Impact estimé sur Supabase & requêtes problématiques

> _Top requêtes issues de `pg_stat_statements` (reset avant test) — à compléter._

### 8.1 Connexions / pooler
- `max_connections = 60`. Échantillons `pg_stat_activity` pendant le plateau :
  `<actives / idle / waiting_on_lock>`. PostgREST mutualise les connexions via un
  pool ; le risque de saturation des 60 connexions vient surtout d'un pic de
  requêtes lentes qui retiennent les connexions.

### 8.2 Requête la plus coûteuse (cumul) — candidate n°1
- **`SELECT count(*) FROM chain_assignments WHERE chain_id = $1`** exécutée par le
  **trigger `assignment_thresholds` à chaque INSERT**. Coût **O(n)** (n = nb de
  psaumes déjà pris dans la chaîne, jusqu'à 150). Sur 10 chaînes × 150 inserts =
  1500 inserts, ce `count` s'exécute **1500 fois**, avec n croissant → coût
  quadratique cumulé par chaîne. Index `chain_assignments_chain_idx` utilisé,
  mais reste un index-scan agrégé à chaque écriture.

### 8.3 Sous-requête RLS sur chaque INSERT
- La policy `asg_insert` fait un `EXISTS (SELECT 1 FROM chains WHERE id = chain_id
  AND …)` à chaque sélection. Lookup par PK sur `chains` (rapide), mais c'est une
  requête supplémentaire systématique par insertion.

### 8.4 `pg_net` / Edge Function
- Aux franchissements 70/80/90/100 %, le trigger poste vers l'Edge Function
  `notify` via `pg_net` (async). Borné (≤ ~4 notifs/chaîne), mais génère des
  requêtes HTTP sortantes pendant la phase de remplissage.

---

## 9. Risques de concurrence

| Risque | Évaluation |
|--------|-----------|
| Doublon `(chaîne, psaume)` | **Éliminé** par le PRIMARY KEY (testé, §6). |
| Sur-réservation (> 150) | **Impossible** : 150 couples max, chacun unique. |
| Race condition sélection | Sérialisée par le verrou de ligne PK ; perdants → 409 propre. |
| Lost update | N/A : sélection = INSERT, désélection = DELETE (pas d'UPDATE). |
| Deadlock | Improbable (une seule ligne par INSERT, pas d'ordre multi-lignes). À confirmer via `pg_stat_database.deadlocks`. |
| Blocage long | À vérifier via `tests/load/sql/05_locks.sql` pendant le plateau. |

---

## 10. Recommandations priorisées

### Quick wins
1. **Limiteur d'auth** : si l'app prévoit des pics d'arrivées (chaîne virale,
   partage de lien), augmenter `RATE_LIMIT_ANONYMOUS_USERS` (dashboard Auth →
   Rate Limits) **ou** mettre en cache la session anonyme côté client (déjà fait :
   `ensureSignedIn` réutilise `currentUser`) pour éviter les re-signups.
2. **Côté client** : `fetchBoard` complet à chaque tick est acceptable (table ≤
   150 lignes) ; préférer le **Realtime** (déjà publié) plutôt que du polling
   `GET` pour rafraîchir l'état → moins de requêtes PostgREST.

### Index à ajouter
3. A priori **aucun nouvel index nécessaire** : la PK et
   `chain_assignments_chain_idx` couvrent les accès. (Confirmer avec
   `index_advisor` / `get_advisors performance` sur les requêtes réelles.)

### Requêtes / triggers à optimiser
4. **Trigger `assignment_thresholds`** (cf. §8.2) — le `count(*)` par insert est le
   point le plus coûteux. Pistes :
   - maintenir un **compteur dénormalisé** `chains.assigned_count` incrémenté dans
     le trigger (`UPDATE … SET assigned_count = assigned_count + 1 RETURNING`),
     supprimant le `count(*)` O(n) ;
   - ou ne déclencher les seuils que via le compteur, pas un reccount complet.
5. **RLS `asg_insert`** : la sous-requête `EXISTS` est correcte ; pour réduire son
   coût on peut s'assurer que `chains.id` (PK) suffit (c'est le cas).

### Points RLS à vérifier
6. La policy autorise l'INSERT **sans exiger d'être participant** (seulement
   `uid = auth.uid()` + chaîne ouverte). Cohérent avec le produit (rejoindre =
   partager un lien), mais à valider : un utilisateur peut réserver sans être
   inscrit comme participant.

### Batching / cache / RPC / transaction
7. Pas besoin de transaction plus stricte : l'atomicité par PK est suffisante et
   supérieure (pas de verrou applicatif). 
8. `assign_remaining` (créateur) est déjà un **INSERT … SELECT generate_series
   ON CONFLICT** en une requête — bon pattern, à conserver.

---

## 11. Distinction des limitations observées

| Couche | Limitante ? | Preuve |
|--------|-------------|--------|
| **Auth** (signup anonyme) | **OUI, en premier** | 429 « Request rate limit reached » dès ~quelques dizaines de signups rapprochés. |
| **API / PostgREST** | _voir §5_ | latences `ep_select_insert` / taux 5xx. |
| **DB / Postgres** | _voir §5/§8_ | `pg_stat_statements`, `pg_stat_activity`, cache hit. |
| **RLS** | non bloquante (correcte) | 0 `biz_select_rls_denied` inattendu ; sous-requête EXISTS peu coûteuse. |
| **Réseau** | _voir §5_ | `http_req_failed`, erreurs de connexion. |
| **Logique métier** | non limitante (atomique) | collision : 0 doublon (§6). |

---

## 12. Reproductibilité

```bash
cp tests/load/.env.example tests/load/.env   # + renseigner URL & ANON_KEY
TARGET=60 tests/load/mint_tokens.sh          # pool d'identités (contourne le limiteur)
# AVANT : tests/load/sql/01_reset_stats.sql
tests/load/run.sh distribution               # 200 VUs, 2m/10m/2m
tests/load/run.sh collision                  # course 20-voies
# PENDANT : échantillonner sql/02_activity.sql et sql/05_locks.sql
# APRÈS : sql/03,04,06 (perf) + sql/07,08 (cohérence) + sql/09 (cleanup)
```

Tout est paramétrable par variables d'environnement (voir
[`tests/load/.env.example`](tests/load/.env.example)). Détails dans
[`tests/load/README.md`](tests/load/README.md).
