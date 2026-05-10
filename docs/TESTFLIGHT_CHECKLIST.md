# Checklist TestFlight — Tehilim

> Ce document liste tout ce qu'il faut faire pour publier sur TestFlight, étape par étape.

---

## A. Apple Developer Portal (à faire en premier)

URL : https://developer.apple.com/account/resources

### A.1 — Créer les App IDs

1. **Identifiers → App IDs → +** (App Description)
2. Type : **App**
3. **Bundle ID** : Explicit → `com.david.tehilim`
4. Description : `Tehilim`
5. Capabilities — coche :
   - **App Groups**
   - **Push Notifications** (pas obligatoire mais utile pour V2)
6. **Continue → Register**

Recommence pour le widget :
1. **+ → App IDs → App**
2. Bundle ID : `com.david.tehilim.widget`
3. Description : `Tehilim Widget`
4. Capabilities — coche :
   - **App Groups**
5. **Continue → Register**

### A.2 — Créer l'App Group

1. **Identifiers → App Groups → +**
2. Description : `Tehilim Shared`
3. Identifier : `group.com.david.tehilim`
4. **Continue → Register**

### A.3 — Lier les App IDs à l'App Group

1. **Identifiers → App IDs → `com.david.tehilim`** → Edit
2. **App Groups → Configure** → coche `group.com.david.tehilim` → Save
3. Continue à modifier l'App ID et **Save** en haut.
4. Recommence pour `com.david.tehilim.widget`.

> ⚠ Sans cette étape, Xcode refusera de signer l'archive — message « Provisioning profile doesn't include the application-groups entitlement ».

---

## B. Xcode — préparer l'archive

### B.1 — Vérifier la signature

1. Ouvrir `Tehilim.xcodeproj`.
2. Sélectionner le projet **Tehilim** dans le navigator.
3. Pour CHACUN des targets `Tehilim` et `TehilimWidget` :
   - Onglet **Signing & Capabilities**
   - **Automatically manage signing** : ✅
   - **Team** : sélectionne ton équipe (avec la mention "Apple Developer Program")
   - Vérifier que **App Groups** est listé avec `group.com.david.tehilim` coché

Si tu vois encore ton "Personal Team" → c'est que tu n'as pas encore activé ton compte payant ou que Xcode ne s'est pas synchronisé. Va dans **Xcode → Settings → Accounts → ton Apple ID → Download Manual Profiles** pour rafraîchir.

### B.2 — Régler la destination d'archive

1. En haut à côté de Tehilim, ouvre le menu **destination** → choisis **Any iOS Device (arm64)**.
   (Si tu as une destination Simulator, l'archive sera invalide.)

### B.3 — Archiver

1. Menu **Product → Archive** (peut prendre 1-3 min).
2. Si succès, la fenêtre **Organizer** s'ouvre avec ton archive.
3. Vérifie qu'il n'y a **aucune erreur de signature**. Si oui, corrige (cf. A.3).

---

## C. App Store Connect — créer l'app

URL : https://appstoreconnect.apple.com

### C.1 — Créer l'app

1. **Mes apps → +** → **Nouvelle app**
2. Plateforme : **iOS**
3. Nom : `Tehilim` (peut différer du bundle ID)
4. Langue principale : **Français (France)**
5. Bundle ID : choisir `com.david.tehilim — Tehilim` dans la liste
6. SKU : `tehilim-ios-001` (identifiant interne, libre)
7. Accès utilisateur : **Accès complet**
8. **Créer**

### C.2 — Renseigner les métadonnées (page Informations sur l'app)

#### Sous-titre (≤ 30 caractères)
> `Lecture des Tehilim en français`

#### Catégorie principale
**Référence** (sous-catégorie : Études)

#### Catégorie secondaire (optionnel)
**Style de vie**

#### Mots-clés (≤ 100 caractères, séparés par virgules)
```
tehilim,psaumes,torah,hebreu,priere,judaisme,kabbalah,david,loubavitch,segond,juif,quotidien
```

#### Description (suggestion)
```
Tehilim — l'application iPhone native pour la lecture quotidienne des Psaumes.

✦ Les 150 Tehilim en hébreu vocalisé (sans téamim, lecture confortable)
✦ Traduction française par le Beth Loubavitch, autorisée
✦ Mode phonétique sépharade pour ceux qui ne lisent pas l'hébreu
✦ Les 5 livres classiques + Tehilim 119 par lettres
✦ 17 cas de la vie : guérison, mariage, parnassa, brith mila, paix, État d'Israël, etc.
✦ Tikkoun HaKlali
✦ Tehilim du jour selon le cycle mensuel ou hebdomadaire
✦ Prière avant et après la lecture
✦ Recherche par numéro arabe, hébraïque, ou tolérante (ex. « tehilim 23 »)
✦ Favoris et reprise de lecture
✦ Rappel quotidien paramétrable
✦ Widget « Tehilim du jour » avec date hébraïque
✦ Partage de verset en image

Sobre, élégant, hors ligne. Aucune donnée personnelle collectée.

Sources :
• Texte hébreu : Sefaria — Miqra according to the Masorah
• Traduction française et autorisation : Beth Loubavitch (le-tehilim.online)
```

#### Description (sous-titre App Store)
> `150 Tehilim · Hébreu · Français · Phonétique · Sans tracker`

#### URL d'assistance
Une URL où l'utilisateur peut te contacter. Options :
- Une page GitHub Pages liée à `Dadoou5/tehilim-ios`
- Une adresse email de support — peut être `david.bouganim@gmail.com`
- Un compte Bluesky/Twitter

Apple exige **une URL accessible**, pas un mailto. La plus simple : créer une page Markdown dans le repo `tehilim-ios/SUPPORT.md` puis activer GitHub Pages, et l'URL sera `https://dadoou5.github.io/tehilim-ios/SUPPORT.html`.

#### URL de politique de confidentialité (obligatoire)
Idem — il faut une URL HTML accessible. Tu peux créer `tehilim-ios/PRIVACY.md` puis Pages.

Voir section **F** plus bas — je peux te générer ces deux pages.

---

### C.3 — Confidentialité de l'app (page Privacy)

1. **Mes apps → Tehilim → Confidentialité de l'app**.
2. Question : « Collectez-vous des données auprès de cette app ? » → **Non, je ne collecte aucune donnée**.
3. Sauvegarde.

> Notre `PrivacyInfo.xcprivacy` correspond à ce choix. Pas d'incohérence.

---

### C.4 — Conformité au chiffrement

Au moment de l'envoi du build :
1. Question : « Votre app utilise-t-elle des algorithmes de chiffrement non exemptés ? » → **Non**.
2. Confirmation.

> On a ajouté `ITSAppUsesNonExemptEncryption=false` dans Info.plist → cette question peut être pré-remplie.

---

## D. Téléverser le build sur TestFlight

### D.1 — Depuis Xcode

1. Dans **Organizer** (ouvert après `Product → Archive`), sélectionne ton archive.
2. **Distribute App**.
3. **App Store Connect** → **Next**.
4. **Upload** → **Next**.
5. Coche les options :
   - ✅ Strip Swift symbols
   - ✅ Upload your app's symbols
   - ✅ Manage Version and Build Number — laisse Xcode gérer si tu veux, ou décoche pour figer.
6. **Distribute** → patiente pendant l'upload (~3-5 min).
7. À la fin, **Done**.

### D.2 — Côté App Store Connect

1. **Mes apps → Tehilim → TestFlight**.
2. Le build apparaît au bout de quelques minutes (status « En cours de traitement »).
3. Une fois traité, il passe à « Test prêt » ou « Détails manquants ».

### D.3 — Compléter les "Détails manquants"

Si l'onglet TestFlight te dit "Détails manquants" :
1. Clique sur le build.
2. Réponds : **Conformité au chiffrement** → Non (on a déjà mis le flag).
3. Sauvegarde.

---

## E. Inviter des testeurs

### E.1 — Test interne (instantané, sans review Apple)
1. **TestFlight → Test interne** (App Store Connect Users).
2. **+** → ajoute des emails (membres du compte développeur).
3. **Sauvegarder**.
4. Les invités reçoivent un email TestFlight, installent l'app via TestFlight.

### E.2 — Test externe (jusqu'à 10 000 testeurs, review Apple ~24 h)
1. **TestFlight → Test externe → +**.
2. Crée un groupe (ex. "Bêta publique").
3. Ajoute le build au groupe.
4. Apple fait une review courte (~1 jour ouvré).
5. Tu peux ensuite ajouter des emails ou un **lien public** pour distribution.

---

## F. Pages de support et de confidentialité

Crée deux fichiers Markdown dans le repo, **dans une nouvelle branche `gh-pages`** ou via GitHub Pages activé sur `main`.

### `SUPPORT.md`
```markdown
# Support — Tehilim

Pour toute question ou suggestion :
- Email : david.bouganim@gmail.com
- GitHub Issues : https://github.com/Dadoou5/tehilim-ios/issues

Réponse sous 5 jours ouvrés.
```

### `PRIVACY.md`
```markdown
# Politique de confidentialité — Tehilim

L'application Tehilim ne collecte aucune donnée personnelle.

## Stockage local
- Préférences (taille de texte, mode quotidien, thème, traduction) : UserDefaults iOS, sur l'appareil uniquement.
- Favoris : fichier JSON dans Application Support, sur l'appareil uniquement.
- Dernière position de lecture : UserDefaults iOS.

## Connexions réseau
Aucune. L'application fonctionne hors ligne.

## Notifications
Les rappels quotidiens sont **locaux**, programmés par iOS sans serveur distant.

## Tiers
Aucun SDK tiers. Aucun tracker. Aucun analytics.

## Sources de contenu
- Texte hébreu : Sefaria (Miqra according to the Masorah, domaine public).
- Traduction française : Beth Loubavitch (le-tehilim.online), avec autorisation.

## Contact
david.bouganim@gmail.com

Dernière mise à jour : 2026-05-10.
```

### Activer GitHub Pages
1. https://github.com/Dadoou5/tehilim-ios/settings/pages
2. **Source** : Deploy from a branch
3. **Branch** : `main` / Folder : `/ (root)` ou `/docs` selon où sont les fichiers
4. **Save**
5. URL générée : `https://dadoou5.github.io/tehilim-ios/`

Tu pointes ensuite App Store Connect vers :
- Support : `https://dadoou5.github.io/tehilim-ios/SUPPORT.html`
- Privacy : `https://dadoou5.github.io/tehilim-ios/PRIVACY.html`

---

## G. Captures d'écran

Apple exige **au minimum** :
- **iPhone 6.7"** (iPhone 15 Pro Max / iPhone 17 Pro Max) : 3 à 10 captures.
- Recommandé : **iPhone 6.5"** + **iPhone 5.5"** également.

Pour générer :
1. Dans Xcode, choisis le simulateur **iPhone 17 Pro Max**.
2. Lance l'app (⌘R).
3. **⌘S** dans le simulateur pour capturer.
4. Les images sortent en `~/Desktop/Simulator Screenshot - iPhone 17 Pro Max - YYYY-MM-DD at HH.MM.SS.png`.
5. Captures suggérées :
   - Accueil (avec date hébraïque visible)
   - Détail d'un Tehilim (mode hébreu + traduction)
   - Mode phonétique
   - Cas de la vie (sectionnés)
   - Tehilim 119 (grille des lettres)
   - Tehilim du jour (Aujourd'hui)
   - Réglages

Upload dans App Store Connect → Tehilim → 1.0 Prepare for Submission → Captures d'écran iPhone.

---

## H. Soumettre le build à TestFlight

Une fois tout en place :
1. Dans App Store Connect → TestFlight → ton build.
2. **Soumettre pour examen** (pour test externe).
3. Apple répond généralement sous 24-48 h pour TestFlight (vs 1-2 semaines pour App Store).

---

## Récapitulatif rapide — étapes minimales

- [ ] A.1 Créer App IDs (`com.david.tehilim` + `.widget`)
- [ ] A.2 Créer App Group `group.com.david.tehilim`
- [ ] A.3 Lier les deux App IDs à l'App Group
- [ ] B.1 Vérifier signature dans Xcode (Team = Apple Developer Program)
- [ ] B.2 Sélectionner Any iOS Device (arm64)
- [ ] B.3 Product → Archive
- [ ] C.1 Créer l'app dans App Store Connect
- [ ] F.x Créer SUPPORT et PRIVACY pages (GitHub Pages)
- [ ] C.2 Métadonnées + URLs support/privacy
- [ ] C.3 Privacy = aucune donnée collectée
- [ ] G Captures d'écran iPhone 6.7"
- [ ] D.1 Distribute App → Upload
- [ ] D.3 Réponses au questionnaire de chiffrement
- [ ] E.1 Inviter testeurs internes (= toi-même + amis)
- [ ] H Soumettre build pour examen TestFlight (test externe)

---

## En cas de pépin

**Erreur : "Missing Push Notification Entitlement"**
→ On déclare qu'on utilise les notifications. Va dans Capabilities et vérifie que **Push Notifications** est activé même si on ne fait pas de remote push (UNUserNotificationCenter local s'en passe normalement).

**Erreur : "Invalid bundle identifier"**
→ Le bundle ID dans Xcode ne correspond pas à celui sur Apple Developer Portal. Vérifie qu'ils sont identiques.

**Erreur : "Provisioning profile doesn't include the application-groups entitlement"**
→ Tu n'as pas lié l'App Group aux App IDs (étape A.3).

**Build mis au statut "Invalid Binary"**
→ Lis le mail d'Apple, généralement c'est un PrivacyInfo manquant ou une icône manquante. Notre PrivacyInfo couvre les API standard. Si ça arrive, dis-le moi, je creuse.

---

## Quand tu auras fini

Dis-moi simplement « TestFlight OK, l'app est uploadée », et on continuera avec la V1.7 ou la préparation de la soumission App Store complète.
