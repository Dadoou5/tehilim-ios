# Contrat realtime mobile — serveur WS self-hosté (phase 3)

Comment l'app iOS/Android consomme le temps réel des chaînes **via le serveur
WebSocket du VPS** (remplaçant Supabase Realtime). À implémenter en natif :
`URLSessionWebSocketTask` (Swift) / `OkHttp WebSocket` (Kotlin).

> Déployé derrière un **feature flag distant** : tant qu'il est OFF, le client
> garde Supabase Realtime. Voir « Dual-run » plus bas.

## Endpoint

```
wss://tehilimapp.com/realtime
```

## Authentification

Le client envoie, **juste après l'ouverture**, le **même JWT Supabase** que celui
utilisé pour l'API REST (session anonyme). Délai d'auth : **8 s**, sinon fermeture.

```jsonc
// client → serveur (1er message)
{ "type": "auth", "token": "<supabase access_token>" }
// serveur → client
{ "type": "auth_ok" }                  // OK
{ "type": "error", "error": "auth failed" }   // puis fermeture
```

Le serveur vérifie la signature HS256 + l'expiration + le rôle `authenticated`.
→ **Rafraîchir le token** (refresh Supabase) avant expiration et se ré-authentifier
si la session WS dure longtemps.

## Abonnement par chaîne

Après `auth_ok`, s'abonner aux chaînes affichées (≤ 20 par connexion) :

```jsonc
// client → serveur
{ "type": "subscribe",   "chainId": "<uuid>" }
{ "type": "unsubscribe", "chainId": "<uuid>" }
// serveur → client
{ "type": "subscribed",   "chainId": "<uuid>" }
{ "type": "unsubscribed", "chainId": "<uuid>" }
```

> **Autorisation** : tout JWT valide peut s'abonner à n'importe quelle chaîne par
> son id — c'est volontaire et identique à la RLS actuelle (`select using(true)` ;
> l'id de chaîne, non devinable, EST le secret d'accès partagé par lien/QR).

## Deltas reçus

Un message par changement de ligne, routé vers les abonnés de la chaîne :

```jsonc
{ "type": "delta", "chainId": "<uuid>", "table": "...", "op": "INSERT|UPDATE|DELETE",
  "row": { ... } | null, "old": { ... } | null }
```

Charge utile minimale par table :

| table | `row` (INSERT/UPDATE) | `old` (UPDATE/DELETE) |
|---|---|---|
| `chain_assignments` | `{psalm_id, uid, name, by_creator}` | `{psalm_id, uid, name}` |
| `chain_participants` | `{uid, name, is_creator}` | `{uid, name}` |
| `chains` | `{id, name, distributed, selection_deadline, reading_deadline}` | `{id}` |

### Application des deltas (équivalences métier)

- **`chain_assignments` / INSERT** → un Tehilim `psalm_id` vient d'être **pris** par
  `name` (verrouillé). Marquer la case occupée.
- **`chain_assignments` / DELETE** → Tehilim `old.psalm_id` **libéré** (désélection /
  redistribution). Remettre la case disponible.
- **`chain_participants` / INSERT** → nouveau participant `name`. UPDATE → renommage.
  DELETE → départ.
- **`chains` / UPDATE** → surveiller `distributed` (false→true = chaîne distribuée,
  fin de sélection) et les échéances. DELETE → chaîne supprimée (sortir de l'écran).

## Reconnexion (important)

Le WS ne rejoue **pas** l'historique. Stratégie cliente :

1. Backoff exponentiel à la reconnexion (1s → 2s → … plafonné ~30s).
2. À chaque (re)connexion : `auth` → `subscribe` des chaînes actives → **un
   refetch REST complet** de l'état de chaque chaîne (combler les deltas manqués
   pendant la coupure), puis appliquer les deltas live.
3. Heartbeat : le serveur envoie des `ping` WS (toutes les 30 s) ; répondre par
   `pong` (géré automatiquement par les libs natives). Une connexion sans pong est
   fermée côté serveur.

## Correspondance avec Supabase Realtime (guide de portage client)

| Aujourd'hui (Supabase Realtime) | Demain (WS VPS) |
|---|---|
| `channel("chain:<id>")` + `postgres_changes` | `subscribe { chainId }` |
| payload `{eventType, new, old, table}` | `delta {op, row, old, table, chainId}` |
| filtre `chain_id=eq.<id>` | routage serveur par `chainId` |
| auth via SDK (JWT) | message `auth` (même JWT) |

La sémantique INSERT/UPDATE/DELETE est conservée → la couche d'application des
changements côté app peut être largement réutilisée.

## Dual-run & feature flag

- Le serveur WS tourne **en parallèle** de Supabase Realtime (les deux lisent la
  même base). On NE retire PAS les tables de la publication `supabase_realtime`
  tant que d'anciennes versions de l'app sont en service.
- Un **flag distant** (ex. ligne dans `private.config` ou table dédiée, lue au
  lancement) choisit la source : `supabase` (défaut) ou `vps`. Permet une bascule
  progressive par cohorte et un rollback sans nouvelle release.

## Activation côté serveur (à faire au lancement de la phase 3)

1. `.env` VPS : `DATABASE_URL` (session 5432) + `SUPABASE_JWT_SECRET`
   (Supabase → Project Settings → API → JWT Secret).
2. Appliquer `sql/03_realtime_delta_triggers.sql` (additif, non destructif).
3. Nginx — ajouter dans le `server { listen 443 }` :
   ```nginx
   location /realtime {
       proxy_pass http://127.0.0.1:3001;
       proxy_http_version 1.1;
       proxy_set_header Upgrade $http_upgrade;
       proxy_set_header Connection $connection_upgrade;
       proxy_set_header Host $host;
       proxy_set_header X-Real-IP $remote_addr;
       proxy_read_timeout 3600s;   # connexions WS longues
       proxy_send_timeout 3600s;
   }
   ```
   puis `nginx -t && systemctl reload nginx`.
4. Démarrer le process : `pm2 start ecosystem.config.cjs --only tehilim-realtime && pm2 save`.
5. Tester : `wss://tehilimapp.com/realtime` → `auth` → `subscribe` → provoquer une
   sélection sur une chaîne de test → vérifier la réception du `delta`.

## Sécurité

- JWT vérifié (signature + exp + rôle) à chaque connexion.
- WS lié à `127.0.0.1:3001`, exposé seulement via Nginx 443 (UFW ferme 3001).
- Pas de donnée sensible diffusée hors du modèle RLS existant (lecture publique
  par id de chaîne, déjà le cas aujourd'hui).
- Limite d'abonnements par connexion (anti-abus).
