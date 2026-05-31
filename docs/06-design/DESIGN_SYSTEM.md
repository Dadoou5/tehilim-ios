# Design System

> Sobre, lisible, premium. Le texte est l'objet principal — l'UI s'efface.

## Palette

### Mode clair

| Token | Hex | Usage |
|-------|-----|-------|
| `bg.primary` | `#F1F7FB` | Fond principal (bleu d'eau très pâle) |
| `bg.surface` | `#FFFFFF` | Cartes, listes |
| `bg.elevated` | `#E3F2FD` | Sections hero, surfaces de second plan |
| `text.primary` | `#1C1C1E` | Texte principal |
| `text.secondary` | `#4A4A50` | Sous-titres, traduction (AAA) |
| `text.tertiary` | `#5E5E63` | Métadonnées, chips |
| `accent.primary` | `#1E40AF` | Accent (bleu royal profond) |
| `accent.muted` | `#BFDBFE` | Bordures actives, surlignage |
| `divider` | `#CDDFEC` | Séparateurs |
| `error` | `#A33A2C` | États erreur |

### Mode sombre

| Token | Hex |
|-------|-----|
| `bg.primary` | `#0C111B` |
| `bg.surface` | `#1A2235` |
| `bg.elevated` | `#252D40` |
| `text.primary` | `#F2EFE8` |
| `text.secondary` | `#C9C3B7` |
| `text.tertiary` | `#989283` |
| `accent.primary` | `#93C5FD` |
| `accent.muted` | `#1E40AF` |
| `divider` | `#2D3548` |
| `error` | `#E07A6E` |

Le mode sombre privilégie un bleu nuit très sombre (jamais noir pur) pour limiter la fatigue oculaire.

### Audit contrastes WCAG (palette éclaircie)

| Token | Mode clair | Mode sombre |
|-------|-----------|-------------|
| textPrimary | 15.75:1 AAA | 16.45:1 AAA |
| textSecondary | **8.14:1 AAA** ⤴ | **10.77:1 AAA** ⤴ |
| textTertiary | 5.97:1 AA | 6.09:1 AA |
| accentMain | 8.07:1 AAA | 10.48:1 AAA |
| errorToken | 6.08:1 AA | 6.46:1 AA |

### Profondeur des cartes

Les cartes (`bgSurface`) reposent sur un fond `bgPrimary` plus contrasté qu'avant. Combiné à une ombre douce :

```swift
.shadow(color: Color.black.opacity(0.04), radius: 8, x: 0, y: 2)
```

Cela donne un effet de léger relief sans skeumorphisme. Centralisé via le modificateur `.appCard()` (cf. `Shared/Theme/ViewModifiers.swift`).

## Typographie

| Rôle | Police | Taille | Style |
|------|--------|--------|-------|
| Hébreu courant | `SBLHebrew` ou `David CLM` ou fallback `.system` | 18 pt | Regular |
| Hébreu titre | idem | 28 pt | Regular |
| FR courant | `New York` (`Font.system(.body, design: .serif)`) | 17 pt | Regular |
| FR titre | New York | 28 pt | Bold |
| Numéros versets | `SF Mono` | 13 pt | Regular |
| Métadonnées | `SF Pro Text` | 13 pt | Regular |
| Action / boutons | `SF Pro Text` | 17 pt | Semibold |

**Échelles Dynamic Type** : tout texte est exprimé via `Font.dynamicSize(...)` pour scaler.

Polices hébraïques : nécessitent vérification de licence pour redistribution. Fallback systématique sur la police hébraïque iOS (Mishafi / system).

## Espacement

Échelle 4 pt. Tokens :
- `space.xs` 4
- `space.s` 8
- `space.m` 12
- `space.l` 16
- `space.xl` 24
- `space.xxl` 32

## Radius

- `radius.s` 8 (chips)
- `radius.m` 12 (cartes)
- `radius.l` 20 (modals)

## Élévation

- `elevation.0` aucune ombre (lecture)
- `elevation.1` ombre douce y=2 r=8 0.05 (cartes)
- `elevation.2` y=8 r=20 0.08 (sheets)

## Iconographie

- 100 % SF Symbols.
- Pas d'illustration custom en V1 sauf logo et icône.
- Cas de la vie : SF Symbols sémantiques (`heart`, `airplane`, `briefcase`, `figure.walk`, etc.).

## Composants — états

| Composant | Default | Pressed | Disabled |
|-----------|---------|---------|----------|
| Card | bg.surface + elevation.1 | scale 0.98 | opacity 0.5 |
| Button primary | accent.primary fill, white text | accent.primary 80% | opacity 0.4 |
| Button secondary | divider stroke, text.primary | bg.elevated | opacity 0.4 |
| Toggle | tint accent.primary | – | – |

## Texte hébreu : règles

- `lineSpacing` : 1.5x base
- `lineLimit(nil)`
- `multilineTextAlignment(.trailing)` (RTL)
- `environment(\.layoutDirection, .rightToLeft)` autour
- Numéro de verset : aligné `.leading` quand RTL, donc visuellement à droite

## Texte FR : règles

- Italique discret (`.italic()`) en mode "secondaire" sous chaque verset.
- Couleur `text.secondary`.
- Pas de soulignement.

## Tokens code (extrait)

```swift
extension Color {
    static let bgPrimary    = Color("bgPrimary")
    static let bgSurface    = Color("bgSurface")
    static let textPrimary  = Color("textPrimary")
    static let accentMain   = Color("accentMain")
    // …
}

extension Font {
    static func hebrewBody(_ size: TextSize) -> Font { … }
    static func frBody(_ size: TextSize) -> Font { … }
}
```
