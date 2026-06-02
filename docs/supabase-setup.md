# Configuration Supabase — feature « Chaîne de Tehilim »

Cette feature collaborative temps réel utilise **Supabase** (Postgres + Realtime
+ Auth anonyme) — elle **remplace Firebase** (l'ancienne implémentation est
conservée sur la branche git **`firebase`**).

Le code applicatif **reste vert sans config** : iOS skip l'init si
`Supabase-Info.plist` est absent ; Android laisse `BuildConfig.SUPABASE_URL/KEY`
vides si `supabase.properties` est absent → l'app demeure **100 % locale**.
Pour **activer** la feature, suivre ces étapes une seule fois.

Projet Supabase : **`ymhbmhnuniaxhhsckdaq`** → `https://ymhbmhnuniaxhhsckdaq.supabase.co`

---

## 1. Activer l'authentification anonyme

Console Supabase → **Authentication → Sign In / Providers** (ou **Settings**) →
activer **« Allow anonymous sign-ins »**.

> Sans ça, `signInAnonymously()` échoue et la feature ne démarre pas. (Équivalent
> de « Authentication → Anonyme → Activer » sous Firebase.)

## 2. Appliquer le schéma SQL (tables + RLS + RPC + Realtime + cleanup)

Le schéma est versionné dans **`supabase/migrations/20260602120000_chains.sql`**.
Trois façons de l'appliquer (au choix) :

- **MCP** (recommandé, intégré) : le serveur MCP Supabase est déjà déclaré dans
  `.mcp.json`. Dans un terminal : `claude /mcp` → sélectionner `supabase` →
  **Authenticate**. Ensuite, demande-moi d'appliquer la migration : j'utiliserai
  l'outil `apply_migration` du MCP.
- **SQL Editor** : Console → **SQL Editor** → coller le contenu du fichier `.sql`
  → **Run**. (Le script est idempotent : ré-exécutable sans erreur.)
- **Supabase CLI** : `supabase link --project-ref ymhbmhnuniaxhhsckdaq` puis
  `supabase db push`.

Ce que crée la migration :
- tables `chains`, `chain_participants`, `chain_assignments` ;
- **verrou** « 1 lecteur / Tehilim » via la **clé primaire `(chain_id, psalm_id)`** ;
- **RLS** : lecture pour tout utilisateur authentifié (le secret est l'id de la
  chaîne) ; écriture limitée au propriétaire (`uid = auth.uid()`) / au créateur,
  pendant la fenêtre de sélection ;
- **RPC** `create_chain` (création atomique chaîne + créateur) et
  `assign_remaining` (créateur attribue les restants) ;
- **RPC** `cleanup_expired_chains` (purge des chaînes expirées — remplace le TTL
  Firestore ; appelée par le cron, cf. §6) ;
- **Realtime** : les 3 tables ajoutées à la publication `supabase_realtime`.

## 3. Récupérer l'URL + la clé anon

Console → **Settings → API** :
- **Project URL** : `https://ymhbmhnuniaxhhsckdaq.supabase.co`
- **Project API keys → `anon` `public`** : la clé à coller ci-dessous.

> La clé `anon` est **publique par conception** (elle est embarquée dans les
> apps clientes ; c'est la **RLS** qui protège les données). On la garde toutefois
> hors du dépôt par convention (cf. `.gitignore`), comme l'était
> `GoogleService-Info.plist`. **Ne jamais committer la clé `service_role`.**

## 4. Config iOS

```bash
cp ios/Tehilim/Resources/Supabase-Info.example.plist \
   ios/Tehilim/Resources/Supabase-Info.plist
```
Puis éditer `Supabase-Info.plist` : coller la clé anon dans `SUPABASE_ANON_KEY`.
Le fichier est gitignoré et embarqué automatiquement par XcodeGen (placé dans
`Resources/`). Régénérer le projet si besoin : `xcodegen generate`.

## 5. Config Android

```bash
cp android/app/supabase.properties.example android/app/supabase.properties
```
Puis éditer `supabase.properties` : coller la clé anon dans `SUPABASE_ANON_KEY`.
Le fichier est gitignoré ; ses valeurs sont injectées dans `BuildConfig` au build.

## 6. Cron GitHub (keep-alive + purge) — contourne la pause 7 jours

Le free tier Supabase **met le projet en pause après ~7 jours d'inactivité**. Le
workflow **`.github/workflows/supabase-keepalive.yml`** s'exécute **chaque jour**
et appelle l'RPC `cleanup_expired_chains`, ce qui :
1. **garde le projet actif** (toute requête réarme le compteur de 7 jours) ;
2. **supprime les chaînes expirées** (remplace le TTL Firestore).

À configurer une fois — **Settings → Secrets and variables → Actions → New
repository secret** :
- `SUPABASE_URL` = `https://ymhbmhnuniaxhhsckdaq.supabase.co`
- `SUPABASE_ANON_KEY` = la clé anon publique

Vérifs : onglet **Actions → Supabase keep-alive + cleanup → Run workflow** pour
un test manuel immédiat.

> ⚠️ Limites GitHub Actions : les `schedule` ne tournent que sur la **branche par
> défaut** (`main`) ; un schedule est **désactivé après 60 jours sans activité**
> sur le dépôt (un commit le réarme).

## 7. (Inchangé) Repo Pages + liens cliquables

Le lien de partage `/c/?id=…` (page de redirection GitHub Pages + Universal/App
Links) est **indépendant du backend** : rien à changer côté Pages lors de la
migration Firebase → Supabase.

---

## Modèle de données (rappel)

```
chains(id, name, intention_type, intention_detail, creator_uid, creator_name,
       created_at, selection_deadline, reading_deadline, distributed, expires_at)
chain_participants(chain_id, uid, name, is_creator, joined_at)   PK (chain_id, uid)
chain_assignments(chain_id, psalm_id, uid, name, by_creator, assigned_at)
                                                                 PK (chain_id, psalm_id)
```

- **Phase** dérivée (non stockée) : `selecting` si `now < selection_deadline &&
  !distributed`, sinon `locked`.
- **Verrou** : l'unicité `(chain_id, psalm_id)` empêche tout double-booking,
  atomiquement, sans transaction applicative.
- **Cycle de vie** : suppression cloud automatique après `expires_at` (= fin de
  lecture + 7 j) via le cron ; le **créateur** garde une **archive locale** du
  compte rendu sur son appareil.

Une fois §1–§5 faits + les secrets du §6 définis, préviens-moi : je relance le
test des règles à distance (création, verrou anti-double, propriété) pour
confirmer le bon fonctionnement de bout en bout.
