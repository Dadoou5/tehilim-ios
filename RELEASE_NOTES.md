# Notes de version Tehilim

## V1.9.4 — 14 mai 2026 (build 15) — Fix navigation iPad 5 livres → Tehilim

### Le bug
Depuis V1.9.0 (NavigationSplitView iPad), tap d'un Tehilim depuis « 5 livres
→ Livre N » dans la sidebar **ne s'ouvrait pas** dans la detail column.
Cause : `Button` + `onTapGesture` à l'intérieur d'une `List` ne sont pas
captés de manière fiable par SwiftUI dans certaines configurations.

### Le fix
Migration vers le **pattern Apple natif** `List(items, selection:)` :
- Sur iPad SplitView, la List utilise désormais sa propriété `selection`
  intrinsèque (au lieu d'un Button avec onTapGesture).
- SwiftUI gère lui-même le tap d'une ligne → met à jour le binding →
  re-render automatique de la detail column.
- Highlight visuel système au passage (sélection bleue native).
- Sur iPhone : NavigationLink push standard, inchangé.

Appliqué à `PsalmListView` et `FavoritesListView`.

---

## V1.9.3 — 14 mai 2026 (build 14) — Hotfixes iPad

### Fix 1 : lecture parallèle dans 119 - AlphaBeta
- Le mode hébreu ‖ traduction côte-à-côte (introduit en V1.9.0 pour
  `PsalmDetailView`) ne s'activait pas dans **`Psalm119SectionView`** : la
  traduction restait empilée sous l'hébreu.
- Maintenant : même comportement auto-détecté (iPad paysage + traduction
  activée + mode hébreu) que pour les Tehilim 1–150.

### Fix 2 : accès aux Tehilim depuis « 5 livres » sur iPad
- Sur iPad NavigationSplitView, le `Button` dans `List` avait une zone de tap
  parfois trop étroite et **aucun indicateur visuel** (pas de chevron) → l'utilisateur
  ne voyait pas comment ouvrir un Tehilim après avoir choisi un livre.
- Refonte :
  - **`onTapGesture` + `contentShape(Rectangle())`** au lieu de Button →
    la zone de tap couvre toute la ligne, plus de tap raté.
  - **Chevron `›`** ajouté à droite quand on est en mode sélection iPad.
  - Accessibility traits enrichis (`isButton`, label + hint).
- Même fix appliqué à `FavoritesListView`.

---

## V1.9.2 — 14 mai 2026 (build 13) — AlphaBeta repensé pour iPad

### Refonte de la grille des 22 lettres
- **Pavés enrichis sur iPad** : la lettre (64pt au lieu de 44), son nom phonétique
  (אלף, בית, גימל...), le numéro de section et le range de versets (v. 1–8, v. 9–16, etc.)
- **6 colonnes au lieu de 8** sur iPad → meilleur ratio des pavés, plus d'air entre eux
- **Pleine largeur** : la grille utilise désormais toute la largeur de l'écran iPad
  (suppression du cap 700pt) pour exploiter pleinement la place disponible
- **Header de contexte** sur iPad : carte d'introduction expliquant le principe
  des 22 sections alphabétiques
- iPhone (compact) : aucun changement, pavés compacts comme avant

### Architecture
- `HebrewLetterTile` adaptatif via `@Environment(\.horizontalSizeClass)`
- `AdaptiveLayout.psalm119ColumnCount(...)` : 8 → 6 sur regular

---

## V1.9.1 — 14 mai 2026 (build 12) — Hotfix : toggle traduction iPad

### Fix
- Sur iPad (NavigationSplitView), le bouton « Afficher la traduction » de la
  toolbar n'était pas suffisamment visible (icône bulle seule, parfois tronquée
  par SwiftUI dans la detail column).
- Maintenant :
  - **Bouton toolbar enrichi** : `Label` avec titre + icône (au lieu d'icône seule)
    → SwiftUI peut afficher le texte sur iPad regular.
  - **Barre d'action inline** ajoutée en haut de chaque écran de lecture
    (PsalmDetailView, Psalm119SectionView) **uniquement sur iPad**, garantissant
    que le toggle est toujours visible.
- Toolbar tooltip `.help(...)` ajouté pour Magic Keyboard.

---

## V1.9.0 — 14 mai 2026 (build 11) — Lecture parallèle + Sidebar iPad

### NavigationSplitView pour l'onglet Tehilim (iPad)
- Sur iPad (regular size class), l'onglet **Tehilim** bascule en **2 colonnes** :
  sidebar à gauche (picker + liste), Tehilim lu à droite. Plus besoin de revenir
  en arrière pour changer de psaume.
- Welcome view élégante quand aucun Tehilim n'est sélectionné.
- Highlight de la ligne sélectionnée (accent color + checkmark côté favoris).
- Sur iPhone et iPad portrait, navigation classique préservée (NavigationStack).

### Mode lecture parallèle (hébreu ‖ traduction)
- Sur **iPad paysage** + traduction activée + mode hébreu, le texte hébreu et sa
  traduction s'affichent désormais **côte-à-côte** au lieu d'empilés.
- Auto-détecté à partir d'une largeur de container ≥ 900pt — pas de réglage à
  toucher, ça s'active naturellement quand l'écran est assez large.
- Cap de largeur étendu à 1200pt en mode parallèle (au lieu de 700pt par défaut).

### Fix régression V1.7
- **Carte de partage de verset** : la langue de la traduction et l'attribution
  de source suivent désormais la préférence active (FR/EN) au lieu d'afficher
  toujours « Traduction : Beth Loubavitch » en français.

### Architecture
- `AdaptiveLayout` enrichi : `shouldUseSideBySide(containerWidth:sizeClass:)`,
  `sideBySideMinWidth`, `sideBySideMaxWidth`.
- `ReadingWidthModifier` paramétrable (max width custom).
- Listes (PsalmList, BookList, FavoritesList) acceptent un `selection: Binding<Int?>?`
  optionnel — nil = comportement push classique, non-nil = pilote la detail column.

---

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
