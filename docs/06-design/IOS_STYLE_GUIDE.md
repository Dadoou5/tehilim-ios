# iOS Style Guide

## Conventions Apple respectées

- HIG : navigation, retour, swipe back natif partout.
- TabBar standard 5 onglets max.
- `NavigationStack` pour pile de navigation.
- `NavigationSplitView` non utilisé (iPhone V1).
- `searchable` pour la recherche intégrée.
- `Form` pour les Réglages.
- `sheet` et `confirmationDialog` natifs uniquement.
- Pas de bouton "Retour" custom.

## Toolbar

- `ToolbarItem(placement: .topBarTrailing)` pour les actions principales.
- `ToolbarItem(placement: .principal)` pour le titre custom (numéro de psaume + lettre).
- Icônes SF Symbols.

## Listes

- `.listStyle(.insetGrouped)` pour les Réglages et catégories.
- `.listStyle(.plain)` pour les listes de psaumes.

## Modales

- `sheet` avec `presentationDetents([.medium, .large])` pour la sélection de mode.
- `fullScreenCover` réservé à la recherche si nécessaire (mais préférer `sheet .large`).

## Accessibilité

- Tous les boutons ont `accessibilityLabel`.
- Tous les textes utilisent `Font.system(.body)` ou `.title` pour bénéficier de Dynamic Type.
- Test à AX5.
- VoiceOver : ordre de lecture vérifié.
- Contraste ≥ 4.5:1.

## Modes

- Light / Dark : auto via `@Environment(\.colorScheme)`.
- Override utilisateur via `Settings` → `preferredColorScheme(...)`.

## Localisation

- En V1 : FR + HE.
- Strings dans `Localizable.strings`.
- Support `String.LocalizationValue`.

## Gestes

- Swipe gauche/droite optionnel pour psaume précédent/suivant (V1.1).
- Long press sur verset → menu contextuel (Copier).
- Pas de pinch-to-zoom (utiliser slider taille texte).

## Performance

- `LazyVStack` pour psaumes longs (Tehilim 119 surtout).
- Préchargement zéro hors corpus.

## Confidentialité

- `Privacy - Tracking Usage Description` : non requis (pas de tracking).
- Pas de `Privacy - Location` etc.
- App Privacy Report dans App Store Connect : 0 collecte.

## App icon

- Lettre ת stylisée OU livre ouvert minimaliste.
- Validée en mode sombre/clair iOS.
- Aucune transparence.
