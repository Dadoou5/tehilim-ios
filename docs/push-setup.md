# Notifications push « Chaîne de Tehilim » — activation

Les **participants** d'une chaîne reçoivent une notification quand :
- la complétude atteint **70 %, 80 %, 90 %** ;
- la chaîne est **distribuée** ;
- la chaîne est **supprimée** par le créateur (hors expiration auto).

Architecture : triggers Postgres → Edge Function `notify` (déjà déployée) → APNs
(iOS) / FCM (Android). Le code app + la base sont **déjà en place**. Il reste à
fournir les **credentials** ci-dessous (le système est inerte sans eux).

> ℹ️ Le push iOS ne fonctionne **que sur un appareil réel** (jamais le simulateur).

---

## 1. Secret partagé (protège l'appel trigger → Edge Function)

Récupère la valeur générée (ne la commite pas) :
```sql
select value from private.config where key = 'notify_secret';
```
→ Supabase → **Edge Functions → notify → Secrets** (ou Project Settings → Edge
Functions secrets), ajoute :
- `NOTIFY_SHARED_SECRET` = cette valeur.

## 2. iOS — APNs

1. [Apple Developer](https://developer.apple.com/account) → **Certificates, Identifiers & Profiles → Keys** → **+** → coche **Apple Push Notifications service (APNs)** → crée la clé → **télécharge le `.p8`** (une seule fois). Note le **Key ID**.
2. **Identifiers → `com.david.tehilim`** → active la capability **Push Notifications**.
3. Edge Function secrets :
   - `APNS_KEY_P8` = contenu intégral du fichier `.p8` (avec les lignes `-----BEGIN PRIVATE KEY-----`…)
   - `APNS_KEY_ID` = l'ID de la clé
   - `APNS_TEAM_ID` = `NFQ2Q87CV9`
   - `APNS_BUNDLE_ID` = `com.david.tehilim`
   - `APNS_HOST` = **optionnel**. La fonction tente production puis bascule
     automatiquement sur sandbox (et inversement) selon le token → dev (Xcode) et
     prod (App Store) marchent sans rien changer. Tu peux le laisser non défini,
     ou forcer `api.sandbox.push.apple.com` / `api.push.apple.com`.
4. L'entitlement `aps-environment` est `development` (déjà posé). Pour l'App Store,
   Xcode bascule en `production` via la capability Push.

## 3. Android — FCM (messagerie uniquement ; la base reste Supabase)

1. [Firebase console](https://console.firebase.google.com) → projet (existant ou nouveau) → **Ajouter une app Android** : package **`app.tehilim`** (puis aussi **`app.tehilim.debug`** pour le variant debug).
2. **Télécharge `google-services.json`** → place-le dans **`android/app/google-services.json`** (gitignoré).
3. **Project settings → Cloud Messaging** : vérifie que **Firebase Cloud Messaging API (V1)** est activé.
4. **Project settings → Service accounts → Generate new private key** → télécharge le JSON. Edge Function secrets :
   - `FCM_SERVICE_ACCOUNT` = le JSON complet (sur une seule ligne, ou tel quel)
   - `FCM_PROJECT_ID` = l'ID du projet Firebase (champ `project_id` du JSON)

## 4. Poser les secrets côté Supabase

Dashboard → **Edge Functions → Secrets** (project-wide) → ajoute les variables des
§1–§3. La fonction `notify` les lit à chaud (pas besoin de redéployer).

## 5. Tester

1. Lance l'app sur **2 appareils réels** (iOS et/ou Android), ouvre/rejoins une chaîne
   (cela demande la permission notifications + enregistre le token).
2. Vérifie les tokens : `select platform, count(*) from public.device_tokens group by 1;`
3. Fais grimper une chaîne à ≥ 70 % (ou distribue / supprime) → les participants
   reçoivent la notification.
4. Logs en cas de souci : Supabase → **Edge Functions → notify → Logs**, et
   `select * from net._http_response order by created desc limit 5;` (réponses APNs/FCM).

---

### Rappels de fonctionnement
- **Une seule** notification par seuil (drapeaux `notified_70/80/90` sur `chains`).
- Suppression : notifiée seulement si `expires_at > now()` (les purges du cron ne
  notifient pas).
- Sans credentials, l'Edge Function répond `401` (secret absent) ou ignore la
  plateforme non configurée — aucun crash, l'app reste fonctionnelle.
