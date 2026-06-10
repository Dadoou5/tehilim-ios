# Migration self-hosted (VPS) — réduire la dépendance Supabase

> **Statut : PLAN — aucun code applicatif écrit.** Document de décision à valider
> avant implémentation. Périmètre demandé : **push + scheduling + realtime**
> self-hostés sur le VPS IONOS (`tehilimapp.com`, 212.227.44.125).
> Rédigé le 2026-06-10.

---

## 0. Contrainte structurante

L'app est **native publiée** (iOS build 65 / Android vc 48). Deux familles de
changements, radicalement différentes :

- **Côté serveur uniquement** (push, scheduling) → invisible pour l'app,
  **réversible en 1 `UPDATE` SQL**, déployable immédiatement.
- **Touche le contrat client** (realtime) → impose une **nouvelle version
  iOS + Android** (review App Store / Play), donc une **période de coexistence**
  où d'anciennes versions tournent encore sur l'ancien système.

➡️ Conséquence : on migre dans cet ordre **push → scheduling → realtime**, et le
realtime se fait en **dual-run** (ancien + nouveau en parallèle) le temps que le
parc d'apps se mette à jour.

---

## 1. Ce qu'on garde sur Supabase (et pourquoi)

| Brique | On garde | Raison |
|---|---|---|
| **Postgres** (données + RLS) | ✅ | Source de vérité live (1338 attributions, 54 tokens). Migrer la base = chantier critique sans gain pour « realtime/push ». Le VPS s'y **connecte** en direct. |
| **Auth** (`auth.uid()`) | ✅ | Les JWT Supabase identifient l'appareil anonyme. On les **vérifie** côté VPS, on ne réémet rien. Migrer l'auth = release mobile + refonte RLS. |

Le VPS devient un **plan applicatif** au-dessus de la base Supabase, pas un
remplacement de la base.

---

## 2. Architecture cible

```
                          ┌──────────────────────────────────────────┐
   App native iOS/Android │                 VPS IONOS                 │
   (Supabase JWT anon)    │            (Node/TS + PM2 + Nginx)        │
        │                 │                                           │
        │  WSS /realtime   ┌──────────────┐   LISTEN     ┌──────────┐ │
        ├─────────────────▶│ realtime-ws  │◀─────────────│          │ │
        │  (JWT + chainId) │  (auth+fanout)│  pg_notify   │          │ │
        │                 │└──────────────┘              │ Supabase │ │
        │  HTTPS REST      ┌──────────────┐   SQL/RPC    │ Postgres │ │
        ├─────────────────▶│   api        │─────────────▶│ (source  │ │
        │  (register token)│  (REST métier)│              │ de       │ │
        │                 │└──────────────┘              │ vérité)  │ │
        │                  ┌──────────────┐   SQL/RPC    │          │ │
        │                  │  scheduler   │─────────────▶│          │ │
   APNs/FCM ◀──────────────│ (cron + push)│   SELECT     │          │ │
                          │└──────────────┘  device_tokens└──────────┘ │
                          └──────────────────────────────────────────┘
```

Trois process PM2 (ou un seul multi-rôle au début) :

- **`api`** — REST : `/healthz`, endpoints métier sensibles, enregistrement de
  token (aujourd'hui fait en direct Supabase REST → on peut le conserver tel quel
  au début).
- **`realtime-ws`** — serveur WebSocket : authentifie le JWT Supabase, autorise
  l'abonnement à une chaîne (vérif `chain_participants`), diffuse les deltas.
- **`scheduler`** — remplace `pg_cron`/`pg_net` : rappels, cleanup,
  auto-extension/distribution, **et l'envoi push** (APNs/FCM).

### Connexion à Postgres
Le VPS se connecte en **direct** (session mode, port 5432 — pas le pooler
transactionnel 6543, car `LISTEN/NOTIFY` exige une session persistante). Secret :
chaîne de connexion `postgres` Supabase, stockée en `.env` sur le VPS.

---

## 3. Brique PUSH (Phase 1 — risque 🟢, pas de release mobile)

**But :** sortir l'Edge Function `notify` du chemin critique ; le VPS envoie APNs/FCM.

- Port du code existant `supabase/functions/notify/index.ts` (Deno) → Node/TS :
  JWT APNs **ES256**, FCM **v1** (service account), messages bilingues, deep-link
  `chainId`, purge des tokens morts (`410/BadDeviceToken`, `UNREGISTERED`).
- Les triggers Postgres appellent déjà une URL via `pg_net` lue dans
  `private.config.notify_url`. **On repointe cette URL** vers
  `https://tehilimapp.com/api/internal/notify` (protégé par `x-notify-secret`,
  déjà en place). **Une seule ligne SQL change.**
- Secrets déplacés vers le `.env` du VPS : `APNS_KEY_P8, APNS_KEY_ID,
  APNS_TEAM_ID, APNS_BUNDLE_ID, APNS_HOST, FCM_SERVICE_ACCOUNT, FCM_PROJECT_ID,
  NOTIFY_SHARED_SECRET, SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY`.

**Rollback :** `update private.config set value=<url Edge Function> where
key='notify_url';` → on revient à l'Edge Function en quelques secondes.

---

## 4. Brique SCHEDULING (Phase 2 — risque 🟡, pas de release mobile)

`pg_cron` exécute aujourd'hui (vu dans les migrations) : rappels toutes les 5 min,
cleanup quotidien, auto-extension/auto-distribution.

- Un **cron Node** (`node-cron`) sur le VPS appelle les **mêmes fonctions SQL**
  déjà existantes (via RPC/SQL) — on **ne réécrit pas la logique métier**, on
  change seulement le déclencheur.
- Déploiement en **shadow** d'abord (le cron Node tourne en lecture/idempotent),
  puis on **désactive les jobs `pg_cron`** une fois la parité vérifiée.

**Rollback :** réactiver les jobs `pg_cron` (one-liner par job), couper le cron Node.

---

## 5. Brique REALTIME (Phase 3 — risque 🔴, **release mobile requise**)

### 5.1 Côté serveur (VPS)
- **Serveur WebSocket** (`ws`) exposé en **WSS** via Nginx (`/realtime`, upgrade
  déjà supporté dans la config Nginx).
- **Auth** : le client envoie son **JWT Supabase** ; le serveur le **vérifie**
  (secret JWT Supabase, HS256) → extrait `uid`.
- **Autorisation par chaîne** : avant d'abonner à `chain:<id>`, le serveur vérifie
  que `uid ∈ chain_participants(chain_id)` (réplique la règle RLS).
- **Source des deltas** : triggers **additifs** sur `chains`, `chain_participants`,
  `chain_assignments` qui font `pg_notify('chain_<id>', <delta jsonb>)`. Le serveur
  tient **une** connexion `LISTEN` et **fan-out** vers les abonnés du topic.
  - *Note vs contrainte initiale* : `LISTEN/NOTIFY` est ici **interne
    Postgres→serveur** (pas exposé au client, qui reçoit un WS propre). La
    contrainte « pas de LISTEN/NOTIFY comme diffusion **client** » est respectée.
  - Payload `NOTIFY` < 8 ko : les deltas (un psaume pris/relâché, un état de
    chaîne) tiennent largement.

### 5.2 Côté mobile (le vrai coût)
- **iOS (Swift)** : remplacer l'abonnement Realtime Supabase par un client
  `URLSessionWebSocketTask` (auth JWT, reconnexion + backoff, application des
  deltas). Aujourd'hui : « realtime seulement en phase de sélection » + UI
  optimiste → surface gérable mais à retester.
- **Android (Kotlin)** : idem avec `OkHttp WebSocket`.
- **Feature flag distant** (dans `private.config` ou une table) pour basculer le
  client VPS↔Supabase **sans nouvelle release**, et router par cohorte.

### 5.3 Dual-run (obligatoire)
Pendant que le parc se met à jour :
1. Le serveur WS du VPS tourne **en parallèle** de Supabase Realtime (les deux
   lisent la même base).
2. Les **nouvelles** versions de l'app utilisent le WS du VPS ; les **anciennes**
   restent sur Supabase Realtime (publication `supabase_realtime` **maintenue**).
3. Quand les anciennes versions sont **drainées** (semaines), on retire les tables
   de la publication `supabase_realtime`.

**Rollback :** feature flag → repasser les clients neufs sur Supabase Realtime.

---

## 6. Sécurité

- Tous les secrets en `.env` (jamais en dur, jamais dans le repo) — UFW limite
  déjà les ports ; le WS et l'API ne sont accessibles que via Nginx 443.
- Vérif stricte du **JWT Supabase** (exp, signature) côté WS et API.
- Autorisation **par chaîne** rejouée côté serveur (ne pas faire confiance au
  client sur `chain_id`).
- `x-notify-secret` conservé pour l'endpoint interne `notify`.
- Rate-limiting raisonnable (par `uid`/IP) sur l'API et les abonnements WS.
- Logs **sans secrets** ; rotation via PM2/logrotate.

---

## 7. Plan de migration ordonné

| Phase | Contenu | Release mobile | Risque | Réversibilité |
|---|---|---|---|---|
| **1** | Push worker sur VPS + repoint `notify_url` | ❌ | 🟢 | 1 `UPDATE` SQL |
| **2** | Scheduler Node (shadow → bascule), désactiver `pg_cron` | ❌ | 🟡 | réactiver `pg_cron` |
| **3a** | Serveur WS VPS en dual-run (serveur seul) | ❌ | 🟡 | couper le process |
| **3b** | Clients WS natifs iOS+Android derrière feature flag | ✅ | 🔴 | flag → Supabase RT |
| **3c** | Bascule progressive par cohorte, drainage | ✅ | 🔴 | flag |
| **3d** | Retrait des tables de `supabase_realtime` | ❌ | 🟡 | re-`add table` |

Chaque phase est **livrable et stable indépendamment** : on peut s'arrêter après
la 1 ou la 2 sans dette.

---

## 8. Fichiers à créer / modifier (à l'implémentation)

**Backend VPS** (`/var/www/tehilim`, restructuré) :
```
src/
├── server.ts                 # api: /healthz, /internal/notify, REST métier
├── realtime/
│   ├── wsServer.ts           # serveur WS (phase 3a)
│   ├── auth.ts               # vérif JWT Supabase
│   └── pgListener.ts         # LISTEN + fan-out
├── services/push/
│   ├── apns.ts               # portage notify (ES256)
│   └── fcm.ts                # portage notify (v1)
├── jobs/scheduler.ts         # cron (phase 2)
├── db/pg.ts                  # pool Postgres (session mode)
├── config/env.ts            # secrets .env
└── types.ts
.env.example
ecosystem.config.cjs          # 1→3 apps PM2
```

**SQL (migrations non destructives)** :
- `repoint_notify_url.sql` (phase 1, + rollback)
- `realtime_notify_triggers.sql` (phase 3a, additif : `pg_notify` deltas)
- `disable_pg_cron_jobs.sql` (phase 2, réversible)

**Nginx** : ajout `location /realtime { proxy + upgrade WSS }`.

**Docs livrables** : architecture finale, exploitation (run/restart/logs/deploy),
« contrat realtime mobile » (protocole WS, topics, deltas), « déclenchement push ».

---

## 9. Points à confirmer / pré-requis

- [ ] **Accès Postgres direct** : récupérer la connection string Supabase (session
      mode, 5432) → secret `.env`. *(Réglages Supabase → Database → Connection.)*
- [ ] **JWT secret Supabase** (pour vérifier les tokens côté VPS).
- [ ] **Secrets push** (APNs `.p8` + IDs, service account FCM) — récupérables
      depuis les secrets actuels de l'Edge Function.
- [ ] Capacité à **livrer une nouvelle version mobile** (compte développeur, cycle
      de review) pour la phase 3.
- [ ] Plan de **monitoring/uptime** du VPS (le WS devient un service critique que
      *tu* opères, là où Supabase était managé).

---

## 10. Recommandation de démarrage

Commencer par **Phase 1 (push)** : valeur immédiate, risque minimal, **aucune
release mobile**, **100 % réversible**. Elle valide la connexion VPS↔Postgres et la
chaîne APNs/FCM auto-hébergée — fondations réutilisées par les phases 2 et 3.
Les phases 3b+ (clients natifs) se planifient avec ton cycle de release mobile.
