# Notes de version Tehilim

## V1.8.1 — 14 mai 2026 (build 10) — Cas de la vie repensés pour iPad

### Refonte « Cas de la vie »
- **Grille adaptive de cartes** à la place d'une simple liste. Chaque catégorie affiche
  son icône colorée, son titre, un extrait du conseil et le nombre de Tehilim à lire.
- **3 colonnes sur iPad**, 2 colonnes sur iPhone — exploite enfin la largeur du grand
  écran sans étirer la lecture.
- **Sections visuellement distinctes** (Cycle de vie, Santé et épreuves, Spiritualité,
  Communauté et calendrier, Autres) avec leur propre header au-dessus de chaque grille.

### Détail d'une catégorie
- Header carte avec icône colorée + extrait du conseil + compteur de Tehilim
- Boutons « Prière avant » / « Prière après » repensés en cartes claires
- **Grille de Tehilim** : 3 colonnes sur iPad, liste verticale sur iPhone
- Reading width cap appliqué pour la lisibilité

### Raccourcis clavier (Magic Keyboard iPad)
- **⌘F** — ouvrir la recherche depuis l'accueil
- **⌘[** — Tehilim précédent (depuis la lecture)
- **⌘]** — Tehilim suivant (depuis la lecture)

### Deep-linking étendu
- `tehilim://home` → onglet Accueil
- `tehilim://psalms` → onglet Tehilim
- `tehilim://daily` → onglet Aujourd'hui (déjà présent)
- `tehilim://lifecases` → onglet Cas de la vie
- `tehilim://settings` → onglet Réglages

Utile pour les futures intégrations avec Raccourcis Apple, widgets et automations.

---

## V1.8.0 — 14 mai 2026 (build 9) — Tehilim sur iPad

### Universal binary
- **Support iPad complet** : iPhone + iPad sur la même app, même corpus, même expérience.
- **Toutes les orientations** sur iPad : portrait, paysage gauche, paysage droite, portrait inversé.
- **Multitâche iPad** activé (`UIRequiresFullScreen` = NO) — l'app fonctionne en Split View et Slide Over.

### Adaptation visuelle
- **Grille « Explorer »** : passe de 2 colonnes (iPhone) à **3 colonnes** sur iPad — les 6 cartes se rangent en 2 lignes équilibrées.
- **Grille des 22 lettres (119 - AlphaBeta)** : passe de 4 colonnes (iPhone) à **8 colonnes** sur iPad — les 22 lettres tiennent en 3 lignes.
- **Longueur de ligne lisible** : sur les écrans de lecture (Tehilim, Tehilim 119 section, Prières), la colonne de texte est **plafonnée à 700 pt** et centrée. La lecture reste confortable même sur un iPad Pro 13".
- **Padding latéral** plus généreux sur iPad (24 pt vs 16 pt sur iPhone).

### Architecture
- Nouveau helper **`AdaptiveLayout`** centralisant les règles de layout responsive (colonnes, padding, max width).
- Modifier `.readingWidth()` réutilisable pour toutes les vues de lecture.

---

## V1.7.5 — 13 mai 2026 (build 8)

### Lecture
- **Texte hébreu justifié à droite** : numéro de verset sur le bord droit puis texte s'écoulant RTL, comme dans un Tehilim imprimé.
- **Toggle de traduction sur la section AlphaBeta** : il est maintenant possible d'afficher/masquer la traduction directement depuis chaque section du Tehilim 119.

### Design
- **Cohérence visuelle 119 – AlphaBeta** : le fond de section utilise désormais `bgPrimary` (même bleu d'eau que les autres écrans de lecture).
- **Pavés des 22 lettres** : remplacement du matériau translucide par la couleur de surface du système de design + bordure subtile pour un rendu identique aux cartes d'accueil.

---

## V1.7.4 — Ezra SIL SR + téamim
- Police hébraïque **Ezra SIL SR** (SIL OFL) supportant les téamim.
- Re-fetch de l'intégralité du corpus avec téamim préservés (2 527 versets).
- Widget carré : max 6 Tehilim affichés (au lieu de 8).
- Renommage **« Tehilim 119 » → « 119 - AlphaBeta »**.

## V1.7.3 — Frank Ruhl Libre + splash dédicace
- Police hébraïque Frank Ruhl Libre.
- Splash animé avec dédicace en hébreu et français.
- Comptages : nombre de Tehilim par livre, nombre de versets par Tehilim.
- Renommage « Taille français » → « Taille de la traduction ».
- Numérotation : « Arabe » → « Numérique ».
- Banner Ilouy nichmat : « ג׳והאן » (à la place de « יוחנן »).

## V1.7.2 — Localisation unifiée
- Sélecteur de langue unique (Système / Français / English) contrôlant à la fois l'interface et la traduction.

## V1.7 — Multilingue anglais
- Traduction anglaise (JPS 1917 via Sefaria).
- UI bilingue fr/en.

## V1.6 — App Group
- Préférences partagées app ↔ widget via `group.com.david.tehilim`.

## V1.5 — Widget Tehilim du jour
- WidgetKit (small/medium/large) avec deep-link `tehilim://`.

## V1.0 → V1.4 — MVP
- 150 Tehilim avec hébreu + français (Beth Loubavitch).
- Cas de la vie (18 catégories).
- Tehilim du jour (cycle mensuel, jour de la semaine).
- Prières avant / après lecture.
- Notifications quotidiennes.
- Onboarding, favoris, recherche.
