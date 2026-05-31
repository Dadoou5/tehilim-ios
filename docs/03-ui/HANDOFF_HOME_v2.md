# Handoff Spec — HomeView v2

> Cible : iOS 17+, SwiftUI
> Auteur : design handoff agent
> Référence design system : [`docs/06-design/DESIGN_SYSTEM.md`](../06-design/DESIGN_SYSTEM.md)
> Statut : implémenté ([`HomeView.swift`](../../ios/Tehilim/Features/Home/HomeView.swift))

---

## 1. Overview

Mise à jour de l'écran d'accueil pour ajouter :

1. **Bandeau de date hébraïque** en tête d'écran (information ambiante, non interactive).
2. **Section « Mes favoris »** entre « Reprendre la lecture » et « Tehilim du jour », fournissant un raccourci d'un tap vers la liste complète des favoris (segment 2 de l'onglet `Tehilim`).

Contexte d'usage : l'utilisateur ouvre l'app au quotidien, voit immédiatement la date hébraïque (utile pour les Tehilim du jour calculés sur ce calendrier), et accède en un tap à ses Tehilim sauvegardés sans passer par l'onglet Tehilim → segment Favoris.

## 2. Layout

Empilement vertical (ScrollView + VStack) avec spacing inter-section `space.xl` (24 pt). Padding latéral `space.l` (16 pt). Pas de breakpoint : iPhone uniquement en V1, le layout s'adapte par Dynamic Type.

```
ScrollView
└─ VStack(spacing: 24)
   ├─ HebrewDateBanner          ← NEW
   ├─ [Reprendre la lecture]    (conditionnel, si dernier psaume connu)
   ├─ Mes favoris (header)      ← NEW
   ├─ FavoritesShortcutCard     ← NEW
   ├─ Tehilim du jour (header)
   ├─ DailySummaryCard
   ├─ Explorer (header)
   └─ LazyVGrid 2 colonnes
      ├─ 5 livres
      ├─ Cas de la vie
      ├─ Tehilim 119
      └─ Tous (1–150)
```

## 3. Design tokens utilisés

| Token | Valeur | Usage |
|-------|--------|-------|
| `Color.bgPrimary` | `#F8F9FB` (clair) / `#0C111B` (sombre) | Fond ScrollView |
| `Color.bgSurface` | `#FFFFFF` / `#141926` | Cartes (Resume, Favoris, Daily) |
| `Color.accentMain` | `#1E40AF` / `#93C5FD` | Icône cœur, icônes Explore |
| `Color.textPrimary` | `#1C1C1E` / `#F2EFE8` | Titres de carte |
| `Color.textSecondary` | implicite via `.secondary` | Labels secondaires, dates en alphabet latin |
| `Color.textTertiary` | `#5E5E63` / `#989283` | Date hébraïque, chevron |
| `space.l` 16 pt | padding interne carte, padding horizontal écran |
| `space.xl` 24 pt | spacing entre sections |
| `radius.m` 12 pt | `RoundedRectangle(cornerRadius: 12)` toutes cartes |
| `Font.subheadline.weight(.medium)` | Date latine — bandeau |
| `Font.subheadline` | Date hébraïque — bandeau |
| `Font.headline` | Section headers + texte primaire des cartes |
| `Font.caption` | Sous-titres de carte |
| `Font.title2` | Icône cœur dans `FavoritesShortcutCard` |

## 4. Components

### 4.1 `HebrewDateBanner` (privé)

| Prop | Type | Défaut | Notes |
|------|------|--------|-------|
| `date` | `Date` | `Date()` | Calculé à l'instanciation, pas réactif |

Sortie de `HebrewDateFormatter.formatted(_:)` :

```swift
struct DisplayDate {
    let dayOfWeek: String     // "Lundi"
    let transliterated: String // "7 Iyar 5786"
    let hebrew: String         // "ז׳ באייר ה׳תשפ״ו"
}
```

Layout : `VStack(alignment: .leading, spacing: 2)` avec deux `Text`. Première ligne en latin/français, deuxième en hébreu, `layoutDirection: .rightToLeft` localement.

### 4.2 `FavoritesShortcutCard` (privé)

| Prop | Type | Notes |
|------|------|-------|
| `count` | `Int` | Nombre de favoris (`favorites.ids.count`) |

Variantes selon `count` :

| count | Icône | Titre | Sous-titre |
|-------|-------|-------|-----------|
| 0 | `heart` (vide) | "Aucun favori" | "Tape ♡ sur un Tehilim pour l'ajouter ici" |
| 1 | `heart.fill` (plein) | "1 Tehilim sauvegardé" | "Voir la liste" |
| n>1 | `heart.fill` | "{n} Tehilim sauvegardés" | "Voir la liste" |

Layout : `HStack` avec icône (32 pt fixe), `VStack` titre+sous-titre (flex), `Spacer`, chevron à droite.

## 5. States and Interactions

| Élément | Action | Comportement |
|---------|--------|--------------|
| `HebrewDateBanner` | Aucune | Non interactif. Calculé une fois au render. |
| `FavoritesShortcutCard` | Tap | Bascule TabRouter vers `.psalms`, segment `2` ; le bandeau d'onglets reflète l'onglet `Tehilim` ; segment `Favoris` actif. |
| `FavoritesShortcutCard` | Long press | Aucun handler en V1 (réservé V2 pour quick actions). |
| `FavoritesShortcutCard` | Touch down | Pas d'effet visuel (`.buttonStyle(.plain)`) ; iOS gère un léger fade système. |

**Important** : la liste des favoris n'est pas affichée en preview sur la home. La carte est un raccourci uniquement. Si on ajoutait un preview, il faudrait paginer pour ne pas explorer tout le `FavoritesStore`.

## 6. Responsive behavior

Plateforme iPhone uniquement en V1. Pas de breakpoints largeur. Les contraintes adaptatives concernent :

| Condition | Comportement |
|-----------|--------------|
| Dynamic Type ≤ XL | Comme spécifié, layout horizontal sur la card Favoris |
| Dynamic Type AX1–AX5 | iOS bascule potentiellement le HStack en VStack pour le contenu de la carte (laissé au système) |
| iPhone SE (375 pt large) | Grille Explorer reste 2 colonnes mais cartes plus étroites |
| iPad (V2) | Non pris en charge V1 |
| Mode sombre | Tokens `bgSurface`, `bgPrimary`, `accentMain` (clair `#1E40AF` → sombre `#93C5FD`) basculent automatiquement |

## 7. Edge cases

| Cas | Comportement |
|-----|--------------|
| **Aucun favori** | Card affichée avec icône vide, message d'incitation. Pas de masquage. |
| **Beaucoup de favoris** (> 99) | Texte « 142 Tehilim sauvegardés ». Pas de troncature attendue jusqu'à 999. |
| **Date qui change pendant l'usage** (passage minuit) | Banner figé sur la valeur initiale ; rafraîchi à `onAppear` quand on revient sur la home. Décision V1, ré-évaluable V2 avec un `Timer` ou `Date` réactif. |
| **Calendrier hébraïque indisponible** | iOS retourne toujours via `Calendar(.hebrew)`. Aucun fallback nécessaire. |
| **Locale système non française** | `dayOfWeek` localisé en français (forcé via `Locale("fr_FR")`). Reste cohérent. |
| **Locale système hébraïque** | Banner reste cohérent : la chaîne `hebrew` utilise `he_IL`, le reste `fr_FR`. |
| **Suppression d'un favori depuis la liste** | `FavoritesStore` met à jour `ids`, `count` met à jour le titre de la carte au prochain affichage de la home. |

## 8. Animation / Motion

| Élément | Trigger | Animation | Durée | Easing | Notes |
|---------|---------|-----------|-------|--------|-------|
| `FavoritesShortcutCard` | Tap | Fade système (TabView switch) | ~200 ms | iOS default | Géré par `TabView` |
| Compteur favoris | Mise à jour `count` | Aucune | – | – | Texte change instantanément (acceptable) |
| `HebrewDateBanner` | Apparition | Aucune | – | – | – |

Toutes ces animations respectent l'environnement `\.accessibilityReduceMotion` (déjà honoré globalement dans `PsalmDetailView` et hérité par TabView pour les transitions).

## 9. Accessibility

### Banner Hebrew date

- `accessibilityElement(children: .combine)` → un seul élément lu par VoiceOver.
- Label combiné : « Lundi 7 Iyar 5786. Calendrier hébraïque : ז׳ באייר ה׳תשפ״ו. »
- VoiceOver lira l'hébreu phonétiquement selon la voix sélectionnée par l'utilisateur (Carmit / Yoav recommandées).

### `FavoritesShortcutCard`

- `accessibilityElement(children: .ignore)` + label/hint custom.
- **Label** : « Voir mes 3 favoris » ou « Aucun favori ».
- **Hint** : « Ouvre la liste des Tehilim sauvegardés ».
- **Trait** : `.isButton`.
- **Cible tactile** : full-width × 64 pt min → ≥ 44×44 ✓.

### Section header « Mes favoris »

- `Text` avec `.accessibilityAddTraits(.isHeader)` → permet le rotor VoiceOver « En-têtes ».

## 10. Implementation notes

- Fichier : [`ios/Tehilim/Features/Home/HomeView.swift`](../../ios/Tehilim/Features/Home/HomeView.swift)
- Utilitaire date : [`ios/Tehilim/Core/Services/HebrewDateFormatter.swift`](../../ios/Tehilim/Core/Services/HebrewDateFormatter.swift)
- Navigation onglet : `TabRouter.go(.psalms, psalmsSegment: 2)` (cf. [`TabRouter.swift`](../../ios/Tehilim/App/TabRouter.swift))
- Source des favoris : `@EnvironmentObject FavoritesStore` injecté par `RootTabView`.

## 11. À tester sur device

- [ ] Date hébraïque correcte aujourd'hui (vérifier la journée bascule au coucher du soleil → décision : pour V1, on suit le passage de minuit grégorien comme iOS, à documenter si validation rabbinique demande autre chose).
- [ ] Card Favoris navigue vers segment `Favoris` du Tehilim tab.
- [ ] Compteur correct après ajout/retrait depuis le détail d'un Tehilim.
- [ ] VoiceOver lit le bandeau date dans l'ordre attendu.
- [ ] Dynamic Type AX5 sans clipping.
- [ ] Mode sombre + nouvelle palette bleue rendu OK.
