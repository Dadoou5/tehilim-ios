# Migration du domaine de partage → `tehilimapp.com`

> Faire passer les liens de partage **chaîne** (`/c/`) et **prière / Lelouy
> Nichmat** (`/p/`) de `dadoou5.github.io` vers `tehilimapp.com`.
> **N'entre en vigueur qu'à la prochaine release iOS + Android.** En attendant,
> les pages `tehilimapp.com/c|p/` fonctionnent déjà via le repli schéma
> `tehilim://` (les anciens liens `dadoou5.github.io` continuent aussi de marcher).

## État déjà en place (serveur)

- Pages au design réel : `https://tehilimapp.com/c/` et `/p/` (mirror exact des
  pages GitHub Pages, images repointées sur le domaine).
- `https://tehilimapp.com/.well-known/apple-app-site-association` (AASA, iOS)
- `https://tehilimapp.com/.well-known/assetlinks.json` (Android) — **à compléter
  avec l'empreinte SHA-256** (voir plus bas).

## Stratégie : dual-domaine

On **ajoute** `tehilimapp.com` sans retirer `dadoou5.github.io` → les liens déjà
partagés restent valides. Le code de parsing accepte déjà **n'importe quel host
https** avec un path `/c` ou `/p` (`isChainLink` / `isPrayerLink` ne testent que
le path), donc les deux domaines ouvrent l'app sans changement de logique.

---

## Diffs à appliquer (release)

### 1. iOS — `ios/Tehilim/Core/Services/ChainShareLink.swift`
```diff
-    static let webBaseURL = "https://dadoou5.github.io/c/"
+    static let webBaseURL = "https://tehilimapp.com/c/"
```

### 2. iOS — `ios/Tehilim/Core/Services/PrayerShareLink.swift`
```diff
-    static let webBaseURL = "https://dadoou5.github.io/p/"
+    static let webBaseURL = "https://tehilimapp.com/p/"
```

### 3. Android — `android/.../core/service/ChainShareLink.kt`
```diff
-    const val WEB_BASE_URL = "https://dadoou5.github.io/c/"
+    const val WEB_BASE_URL = "https://tehilimapp.com/c/"
```

### 4. Android — `android/.../core/service/PrayerShareLink.kt`
```diff
-    const val WEB_BASE_URL = "https://dadoou5.github.io/p/"
+    const val WEB_BASE_URL = "https://tehilimapp.com/p/"
```

### 5. iOS — entitlements (`Tehilim.entitlements` ET `Tehilim.Release.entitlements`)
Ajouter le nouveau domaine (garder l'ancien pour les liens déjà partagés) :
```diff
     <key>com.apple.developer.associated-domains</key>
     <array>
         <string>applinks:dadoou5.github.io</string>
+        <string>applinks:tehilimapp.com</string>
     </array>
```

### 6. Android — `android/app/src/main/AndroidManifest.xml`
Dans l'`<intent-filter android:autoVerify="true">` (celui des liens https),
ajouter les `data` du nouveau host à côté des existants :
```diff
                 <data android:scheme="https"
                       android:host="dadoou5.github.io"
                       android:pathPrefix="/p/" />
                 <data android:scheme="https"
                       android:host="dadoou5.github.io"
                       android:pathPrefix="/c/" />
+                <data android:scheme="https"
+                      android:host="tehilimapp.com"
+                      android:pathPrefix="/p/" />
+                <data android:scheme="https"
+                      android:host="tehilimapp.com"
+                      android:pathPrefix="/c/" />
```

---

## Compléter `assetlinks.json` (empreinte Android)

Le fichier serveur contient un placeholder. Récupère le **SHA-256 du certificat
de signature** :
- **Play App Signing** : Play Console → ton app → *Test and release → App
  integrity → App signing* → copier le **SHA-256 certificate fingerprint** (clé
  *App signing*, pas la clé d'upload).
- ou en local : `keytool -list -v -keystore <ta-clé>.keystore -alias <alias>`.

Remplace `REMPLACER_PAR_LE_SHA256_…` (format `AA:BB:CC:…`) puis redéploie le site
(`rsync … Tehilimapp.com/ tehilim:/var/www/tehilimapp-site/`).

> Tip : ajouter aussi l'empreinte de la **clé d'upload / debug** dans le tableau
> `sha256_cert_fingerprints` permet de tester les App Links avec un build debug.

## Vérification (après release)

- iOS : `https://tehilimapp.com/.well-known/apple-app-site-association` →
  `200` + `Content-Type: application/json`. Tester un lien `https://tehilimapp.com/c/?id=…`
  sur un appareil avec la nouvelle build → ouverture directe (sans page).
- Android : `https://tehilimapp.com/.well-known/assetlinks.json` → `200`. Vérifier
  l'auto-verify : `adb shell pm get-app-links app.tehilim` (statut `verified`).

## Ordre de déploiement

1. Compléter `assetlinks.json` (SHA-256) + s'assurer que les `.well-known` sont
   servis (déjà fait côté serveur).
2. Appliquer les diffs 1→6, builder, soumettre iOS + Android.
3. Après publication : les **nouveaux** liens pointent sur `tehilimapp.com` et
   s'ouvrent en Universal/App Links. Les anciens liens `dadoou5.github.io`
   continuent de fonctionner (dual-domaine).
4. (Optionnel, plus tard) Quand le parc a basculé, on peut retirer
   `dadoou5.github.io` des entitlements/manifest.
