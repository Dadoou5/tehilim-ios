# Configuration Firebase — feature « Chaîne de Tehilim » (ARCHIVÉ)

> ⚠️ **Obsolète sur `main`.** La feature a été **migrée vers Supabase** — voir
> **[`supabase-setup.md`](./supabase-setup.md)**. Ce document ne concerne plus
> que la **branche git `firebase`** (implémentation Firebase conservée pour
> référence / repli). Sur `main`, Firebase n'est plus utilisé.

Cette feature collaborative temps réel nécessite **Firebase Firestore** + **Auth anonyme**.
Le code applicatif est prêt et **reste vert sans config** (le plugin Android google-services
n'est appliqué que si `google-services.json` est présent ; iOS skip l'init si le plist manque).
Pour **activer** la feature, suis ces étapes une seule fois.

---

## 1. Créer le projet Firebase

1. https://console.firebase.google.com → **Ajouter un projet** (ex. « Tehilim »).
2. **Build → Firestore Database → Créer une base** → **mode production** → région `eur3` (Europe).
3. **Build → Authentication → Commencer → Anonyme → Activer**.

## 2. App iOS

1. Console → ⚙️ → **Vos applications → iOS+**.
2. **ID du bundle** : `com.david.tehilim`.
3. Télécharger **`GoogleService-Info.plist`** → le placer dans **`ios/Tehilim/Resources/GoogleService-Info.plist`**.
   (Le fichier est déjà attendu par `project.yml` ; il est git-ignoré — voir §6.)

## 3. App Android

1. Console → ⚙️ → **Vos applications → Android**.
2. **Nom du package** : `app.tehilim`.
3. Ajouter les **empreintes SHA-256** (Authentication / App Links) — les deux déjà connues :
   - `AF:E5:C9:76:1E:29:05:F1:51:3F:B4:0A:2E:9A:BA:94:D4:78:B8:8E:C1:AA:00:13:AF:3D:BC:08:92:B9:49:0B`
   - `D8:E9:2B:BA:44:09:B6:9F:E1:F7:F8:52:6F:08:52:1A:31:4C:9A:06:DB:A3:CA:C5:7D:CF:46:DB:6B:9C:DF:54`
4. **Ajouter une 2ᵉ app Android** pour le variant debug : package **`app.tehilim.debug`** (sinon les builds debug ne se connectent pas).
5. Télécharger **`google-services.json`** (il contient les deux packages) → le placer dans **`android/app/google-services.json`**.

## 4. Règles de sécurité Firestore

Console → **Firestore → Règles** → coller ceci → **Publier** :

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function signedIn() { return request.auth != null; }

    match /chains/{chainId} {
      function chain() {
        return get(/databases/$(database)/documents/chains/$(chainId)).data;
      }

      // Le secret est l'id de chaîne (non devinable) → lecture pour tout signed-in.
      allow read: if signedIn();
      allow create: if signedIn()
        && request.resource.data.creatorUid == request.auth.uid;
      // Seul le créateur modifie la chaîne (deadlines, distributed) ou la supprime.
      allow update, delete: if signedIn()
        && resource.data.creatorUid == request.auth.uid;

      match /participants/{uid} {
        allow read: if signedIn();
        allow create, update, delete: if signedIn() && uid == request.auth.uid;
      }

      // Optimisation quota : l'état des attributions tient dans UN doc
      // `state/board` (map des 150 cases) au lieu de 150 docs.
      match /state/{doc} {
        allow read: if signedIn();
        // Écriture autorisée au créateur, ou à tout participant pendant la
        // fenêtre de sélection (chaîne non distribuée). La logique « ne prendre
        // que des cases libres / ne libérer que les siennes » est appliquée
        // côté client par transaction (Firestore ne sait pas valider par clé de
        // map). Compromis assumé : app non-sensible, partage entre proches.
        allow create, update: if signedIn() && (
          chain().creatorUid == request.auth.uid
          || (!chain().distributed && chain().selectionDeadline > request.time)
        );
      }
    }
  }
}
```

## 5. Politique TTL (suppression auto après lecture)

Console → **Firestore → Indexes (ou Time-to-live)** → **Créer une politique TTL** :
- Collection group : `chains`
- Champ d'horodatage : **`expiresAt`**

→ Firebase supprime chaque chaîne ~24 h après `expiresAt` (que l'app fixe à *fin de lecture + 7 jours*).
Le **créateur** en conserve une **archive locale** (copie du compte rendu sur son appareil), donc il ne perd rien.

## 6. .gitignore (ne pas committer les secrets)

Ajouter (déjà prévu) :
```
ios/Tehilim/Resources/GoogleService-Info.plist
android/app/google-services.json
```

## 7. Repo Pages (`dadoou5.github.io`, séparé) — lien cliquable

1. Créer **`/c/index.html`** (page de redirection, copier sur le modèle de `/p/index.html`) qui lit `?id=…` et rouvre `tehilim://chain?id=…`, avec repli vers les stores.
2. Dans **`/.well-known/apple-app-site-association`**, ajouter le chemin `/c/*` à la liste `paths`/`components` (à côté de `/p/*`).
   `assetlinks.json` est par domaine → déjà couvert pour Android.

---

Une fois §1–§5 faits et les deux fichiers de config en place, préviens-moi : je finalise le wiring,
le `ChainService` et tous les écrans, puis on teste à deux simulateurs.
