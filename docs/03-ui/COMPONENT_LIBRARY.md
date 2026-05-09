# Component Library (SwiftUI)

> Tous les composants vivent dans `ios/Tehilim/Shared/Components/`.

## 1. PsalmCard

```swift
struct PsalmCard: View {
    let psalm: Psalm
    let isFavorite: Bool
    let onTap: () -> Void
}
```

Visuel : carte légère, ombre 1, padding 16, titre + numéro hébreu.

## 2. VerseRowView

```swift
struct VerseRowView: View {
    let verse: Verse
    let showTranslation: Bool
    let textSize: TextSize
}
```

Structure :
- HStack RTL : numéro de verset + texte hébreu.
- Si traduction visible : `Text(translation)` en dessous, gris secondaire, taille - 1 step.
- Long-press : menu copier / partager (V2).

## 3. SearchBarView

```swift
struct SearchBarView: View {
    @Binding var query: String
    var onSubmit: () -> Void
}
```

Bilingue (placeholder dynamique selon clavier).

## 4. ModeChip

```swift
struct ModeChip: View {
    let title: String
    let isActive: Bool
}
```

Pour afficher le mode "Tehilim du jour" en haut.

## 5. HebrewLetterTile

```swift
struct HebrewLetterTile: View {
    let letter: String       // "א"
    let index: Int           // 1..22
    let onTap: () -> Void
}
```

Tile carrée, lettre 48 pt, numéro 12 pt en bas.

## 6. SectionHeader

```swift
struct SectionHeader: View {
    let title: String
    var subtitle: String? = nil
}
```

## 7. EmptyStateView

```swift
struct EmptyStateView: View {
    let symbol: String       // SF Symbol
    let title: String
    let message: String?
    var action: (label: String, handler: () -> Void)? = nil
}
```

## 8. ErrorBanner

```swift
struct ErrorBanner: View {
    let message: String
    let retry: (() -> Void)?
}
```

## 9. TextSizeStepper

```swift
struct TextSizeStepper: View {
    @Binding var size: TextSize     // .xs .s .m .l .xl
}
```

Aperçu live d'un verset.

## 10. ToolbarButton (raccourci)

```swift
struct ToolbarButton: View {
    let symbol: String
    let label: String
    let action: () -> Void
}
```

Toujours `accessibilityLabel` rempli.

---

## Tokens partagés

```swift
enum TextSize: String, CaseIterable, Codable {
    case xs, s, m, l, xl
}

enum AppTheme: String, CaseIterable, Codable {
    case system, light, dark
}

enum VerseNumberStyle: String, Codable {
    case hebrew, arabic
}
```
