# Backend Tehilim (VPS) — push + scheduler self-hostés

Backend Node.js/TypeScript qui **réduit la dépendance Supabase** en déportant sur
le VPS (1) l'**envoi des notifications push** (APNs/FCM) — auparavant l'Edge
Function `notify` — et (2) le **scheduling** des jobs chaîne — auparavant
`pg_cron`. Postgres, Auth et Realtime restent sur Supabase.

Plan d'ensemble : [`docs/04-tech/VPS_SELFHOST_MIGRATION.md`](../docs/04-tech/VPS_SELFHOST_MIGRATION.md).

## Architecture

Deux process PM2 indépendants :

| Process | Rôle | Écoute / déclenche |
|---|---|---|
| **`tehilim-api`** | HTTP : `/healthz`, `POST /internal/notify` (push) | `127.0.0.1:3000` (proxy Nginx `/api/`) |
| **`tehilim-scheduler`** | Cron : appelle les fonctions SQL chaîne | `*/5` reminders+lifecycle, `17 3` cleanup (UTC) |

`/internal/notify` reçoit le **même payload** que l'Edge Function d'origine
(envoyé par les triggers Postgres via `pg_net`), protégé par `x-notify-secret`.

```
src/
├── api.ts                 # entrée process api (Express)
├── scheduler.ts           # entrée process scheduler (node-cron)
├── config/env.ts          # lecture .env
├── log.ts                 # logs JSON
├── db/pg.ts               # pool Postgres (scheduler + phase 3)
└── services/
    ├── supabase.ts        # purge des tokens morts (REST service-role)
    └── push/{messages,jwt,apns,fcm,index}.ts   # envoi APNs/FCM
sql/
├── 01_repoint_notify_url.sql          # cutover phase 1 (+ rollback)
└── 02_phase2_cutover_disable_pgcron.sql  # cutover phase 2 (+ rollback)
```

## Variables d'environnement

Voir [`.env.example`](.env.example). `.env` vit dans `/var/www/tehilim/.env`
(perms `600`), chargé par Node via `--env-file`. Le worker push est **inerte**
(no-op) tant que les secrets push sont vides → démarrage sans risque.

## Build & déploiement

Depuis la machine locale (le code vit dans `server/`) :

```bash
# 1. Envoyer le code
rsync -az --delete --exclude=node_modules --exclude=dist --exclude=.env \
  -e ssh server/ tehilim:/var/www/tehilim/

# 2. Builder + (re)démarrer sur le serveur
ssh tehilim 'cd /var/www/tehilim && npm install --omit=dev=false && npm run build && pm2 reload ecosystem.config.cjs --only tehilim-api && pm2 save'
```

> `tehilim-scheduler` ne se démarre qu'à la **phase 2** (voir cutover).

## Exploitation (commandes)

```bash
ssh tehilim                       # se connecter
pm2 ls                            # état des process
pm2 logs tehilim-api              # logs push en direct
pm2 logs tehilim-scheduler        # logs cron
pm2 reload tehilim-api            # redémarrage sans coupure
pm2 restart tehilim-api           # redémarrage
pm2 stop tehilim-scheduler        # arrêt scheduler
curl -s https://tehilimapp.com/api/healthz   # sonde
```

## Cutover Phase 1 — basculer le push sur le VPS

1. Renseigner les secrets dans `/var/www/tehilim/.env` :
   `NOTIFY_SHARED_SECRET` (= `private.config.notify_secret`),
   `SUPABASE_SERVICE_ROLE_KEY`, `APNS_KEY_P8/ID/TEAM/BUNDLE`, `FCM_SERVICE_ACCOUNT`,
   `FCM_PROJECT_ID`. Puis `pm2 reload tehilim-api`.
2. **Test end-to-end** avant bascule (avec un token de test) :
   ```bash
   curl -s -X POST https://tehilimapp.com/api/internal/notify \
     -H "x-notify-secret: <NOTIFY_SHARED_SECRET>" -H "Content-Type: application/json" \
     -d '{"event":"threshold","value":70,"chainName":"Test","chainId":null,
          "tokens":[{"token":"<DEVICE_TOKEN>","platform":"ios","locale":"fr"}]}'
   # attendu : {"sent":1} + notification reçue
   ```
3. Appliquer [`sql/01_repoint_notify_url.sql`](sql/01_repoint_notify_url.sql)
   (repointe `notify_url` vers le VPS). **Rollback** : 1 `UPDATE` (dans le fichier).

## Cutover Phase 2 — basculer le scheduling sur le VPS

1. Renseigner `DATABASE_URL` (connexion directe Supabase, session 5432) dans `.env`.
2. Démarrer le scheduler **en shadow** et vérifier la parité dans les logs :
   ```bash
   ssh tehilim 'cd /var/www/tehilim && pm2 start ecosystem.config.cjs --only tehilim-scheduler && pm2 save'
   pm2 logs tehilim-scheduler        # doit montrer scheduler.job_ok sans erreur
   ```
   > ⚠️ Risque de **double envoi** tant que pg_cron tourne aussi. Garder la fenêtre
   > shadow courte (ou tester hors heures de pointe), puis enchaîner l'étape 3.
3. Appliquer [`sql/02_phase2_cutover_disable_pgcron.sql`](sql/02_phase2_cutover_disable_pgcron.sql)
   (désactive les 3 jobs `pg_cron`). **Rollback** : réactiver (dans le fichier) +
   `pm2 stop tehilim-scheduler`.

## Sécurité

- Secrets uniquement dans `.env` (perms 600), jamais dans le repo (`.gitignore`).
- `/internal/notify` protégé par `x-notify-secret` ; l'API n'écoute que sur
  `127.0.0.1` (exposée seulement via Nginx 443, UFW limite les ports).
- Logs sans secrets ni tokens complets.
