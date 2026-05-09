# UI Spec

> Référence : SwiftUI sur iOS 17+. Design system : `DESIGN_SYSTEM.md`.

## Conventions générales

- Layout : `NavigationStack` par feature, `TabView` racine.
- Padding standard : 16 pt horizontal, 12 pt vertical.
- Largeur de colonne lecture : 100 % - 32 pt latéral.
- Fond : `Color.background` (token système, adapt. mode sombre).
- Hébreu : `.environment(\.layoutDirection, .rightToLeft)` localement aux blocs hébreu.
- Pas d'animation > 250 ms sur la lecture.

---

## 1. HomeView

| Élément | Type | Style |
|---------|------|-------|
| Titre "Tehilim" | `Text` | `Font.serif.title.bold` |
| Bouton recherche header | `Button` (icon `magnifyingglass`) | TopTrailing toolbar |
| Champ recherche (présent) | `TextField` | placeholder bilingue (FR + heb) |
| Section "Reprendre" | `Card` (DS) | masquée si `lastReadId == nil` |
| Section "Tehilim du jour" | `Card` | mode + numéros en chips |
| Grille "Explorer" | `LazyVGrid` 2 colonnes | 4 cartes : 5 livres, Cas de la vie, Tehilim 119, Tous |

États :
- Chargement initial : skeleton sur 3 cartes max 1 s.
- Aucune lecture précédente : section "Reprendre" cachée, pas d'état vide explicite.

## 2. PsalmListView (livre OU complète)

- `List` (style `.plain`).
- Cellule : `[numéro arabe] [numéro hébreu] [titre] [♡ si fav]`.
- Search inline : `.searchable(text: ..., placement: .navigationBarDrawer(displayMode: .always))`.
- Header de section optionnel (cas "Tous" → headers par livre).

## 3. PsalmDetailView

- `ScrollView` + `LazyVStack`.
- Toolbar :
  - leading : back natif.
  - trailing : ♡ favori, FR toggle, Aa (sheet taille texte), ⋯ menu.
- Chaque verset : composant `VerseRowView` (voir COMPONENT_LIBRARY).
- Footer : `HStack` avec deux boutons "← N-1" et "N+1 →" (désactivés aux bornes).
- `onAppear` : enregistre `lastReadId`.
- `onScroll` : enregistre la position toutes les 2s ou au changement de paragraphe.

Animation : transition entre psaumes = `.slide` discrète.

## 4. SearchView

- Présentée en `sheet` ou en plein écran selon point d'entrée.
- `TextField` autofocus.
- Liste de résultats : 1 résultat exact en gras, suggestions en gris.
- Section "Récents" (maxi 10).
- Section "Suggestions" (Tehilim populaires : 23, 27, 91, 121, 130, 150).
- État vide : pas d'erreur visible, juste les suggestions.
- État sans résultat : message + suggestions.

Logique d'interprétation : voir `Core/Search/SearchInterpreter.swift`.

## 5. DailyView

- Bandeau supérieur fixe : "Cycle mensuel — jour 7" + bouton mode.
- Liste de psaumes du jour.
- Bouton primaire bas : "Lire à la suite" → ouvre 1er psaume avec une chaîne.
- Sheet "Mode" : Picker `.segmented` ou liste.

## 6. LifeCasesListView

- `List` style `.insetGrouped`.
- Cellule : icône (SF Symbol éditorial) + titre + nb de psaumes en accessoire.

## 7. LifeCaseDetailView

- Header : titre catégorie + note éditoriale (`Text`, line spacing 1.2).
- Liste de psaumes (compact).
- Avertissement bas (≤ 1 ligne, gris) : « Tradition. Ne remplace pas un avis professionnel. »

## 8. Psalm119HomeView

- Grille 4 colonnes (compact) ou 5 (regular) de 22 cellules de lettre.
- Cellule : grand caractère hébreu + indice (#1, #2…).

## 9. Psalm119SectionView

- Réutilise `PsalmDetailView` paramétré sur sous-plage de versets.
- Footer : navigation lettre prev / next.

## 10. SettingsView

- `Form` style natif.
- Sections : Affichage, Lecture quotidienne, À propos.
- Toggles, Pickers, NavigationLinks vers détails.

---

## Comportement clavier (recherche)

- Clavier hébreu et latin tous deux acceptés.
- Bouton "search" du clavier valide.
- Sur iPad future : `KeyboardShortcut("⌘F")` pour ouvrir search.

## Haptics

- Tap favori : `UIImpactFeedbackGenerator(style: .soft)`.
- Toggle traduction : `selectionChanged`.
- Erreur recherche : aucun haptique (non agressif).

## Animations

- Apparition cartes accueil : fade simple.
- Scroll : pas de parallax.
- Détail psaume : pas d'effet, lecture pure.
