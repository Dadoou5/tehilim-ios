# Architecture Technique

## Stack

- Langage : Swift 5.9+
- UI : SwiftUI (iOS 17+)
- Pattern : MVVM léger + dependency injection manuel
- Navigation : `NavigationStack` + `TabView`
- Persistance : `UserDefaults` (préférences) + JSON disque (corpus, contenu) + SwiftData (favoris/historique, V1.1 sinon `Codable` + JSON dans `Documents`)
- Tests : XCTest (unit) + XCUITest (UI)

## Principes

1. **Offline-first** : tout le corpus est embarqué dans `Resources/Content/`.
2. **Modularité par feature** : chaque feature autonome.
3. **Une seule source de vérité par domaine** : `PsalmRepository` est unique.
4. **Pas d'over-engineering** : pas de Redux, pas de Combine massif. `@Observable` (Observation framework iOS 17) pour ViewModels.
5. **Pas de réseau** en V1.

## Diagramme

```
┌──────────────────── App ─────────────────────────┐
│                                                   │
│  ┌─ App ─────────────────┐                        │
│  │ TehilimApp.swift      │                        │
│  │ AppContainer (DI)     │                        │
│  │ RootTabView           │                        │
│  └───────────────────────┘                        │
│                                                   │
│  ┌─ Features ────────────┐                        │
│  │ Home / Psalms /       │  ← Views + VM         │
│  │ Search / Daily /      │                       │
│  │ LifeCases / Psalm119  │                       │
│  │ Settings / Favorites  │                       │
│  └─────────┬─────────────┘                       │
│            │                                      │
│  ┌─ Core ──▼─────────────┐                       │
│  │ Models (Psalm, …)     │                       │
│  │ Services              │                       │
│  │  - ContentLoader      │  ── lit JSON local   │
│  │  - DailyEngine        │                       │
│  │  - SearchInterpreter  │                       │
│  │  - HebrewNumerals     │                       │
│  │ Persistence           │                       │
│  │  - Preferences        │  ── UserDefaults     │
│  │  - FavoritesStore     │                       │
│  │  - LastReadStore      │                       │
│  └───────────────────────┘                       │
│                                                   │
│  ┌─ Shared ──────────────┐                       │
│  │ Components            │                       │
│  │ Theme (DS tokens)     │                       │
│  │ Localization          │                       │
│  └───────────────────────┘                       │
│                                                   │
│  ┌─ Resources ───────────┐                       │
│  │ Content/psalms.json   │                       │
│  │ Content/life_cases…   │                       │
│  │ Content/psalm_119_…   │                       │
│  │ Content/daily_rules…  │                       │
│  │ Localizable.strings   │                       │
│  │ Assets.xcassets       │                       │
│  └───────────────────────┘                       │
└───────────────────────────────────────────────────┘
```

## Modèles principaux

```swift
struct Psalm: Codable, Identifiable {
    var id: Int                 // 1...150
    var book: Int               // 1...5
    var hebrewNumber: String    // "כג"
    var hebrewTitle: String?    // "מזמור לדוד"
    var verses: [Verse]
    var tags: [String]
}

struct Verse: Codable, Identifiable {
    var id: String              // e.g. "23:1"
    var number: Int
    var hebrewNumber: String
    var hebrew: String
    var translationFR: String?
}

struct LifeCase: Codable, Identifiable {
    var id: String              // slug
    var title: String
    var note: String
    var psalms: [Int]
    var symbol: String          // SF Symbol name
}

struct Psalm119Section: Codable, Identifiable {
    var id: String              // "alef"
    var letter: String          // "א"
    var index: Int              // 1...22
    var versesRange: ClosedRange<Int>  // 1...8
}

enum DailyMode: String, Codable, CaseIterable { case monthly, weekly, custom }

struct DailyRules: Codable {
    var monthly: [Int: [Int]]   // day(1...30) → [psalmIds]
    var weekly: [Weekday: [Int]]
}
```

## Services

### ContentLoader

```swift
protocol ContentLoading {
    func loadPsalms() throws -> [Psalm]
    func loadLifeCases() throws -> [LifeCase]
    func loadPsalm119Sections() throws -> [Psalm119Section]
    func loadDailyRules() throws -> DailyRules
}
```

Implémentation : `JSONDecoder` sur fichiers de bundle. Cache en mémoire après première lecture.

### DailyEngine

```swift
struct DailyEngine {
    let rules: DailyRules
    func psalmsForToday(mode: DailyMode, on date: Date = .now, calendar: Calendar = .hebrew) -> [Int]
}
```

Dépend du calendrier hébraïque (`Calendar(identifier: .hebrew)`).

### SearchInterpreter

```swift
struct SearchQueryResult {
    let psalmId: Int?
    let suggestions: [Int]
    let interpretation: String?  // ex. "Tehilim 23"
}

struct SearchInterpreter {
    func interpret(_ query: String) -> SearchQueryResult
}
```

Logique :
1. Trim, lower, supprimer "tehilim", "psaume", "תהילים".
2. Si chaîne purement numérique → parse Int.
3. Sinon, si chaîne purement hébraïque → `HebrewNumerals.toInt(...)`.
4. Sinon, mixte → tenter d'extraire un nombre.
5. Valider plage 1..150.
6. Construire suggestions (Levenshtein simple sur titres + numéros voisins).

### HebrewNumerals

```swift
enum HebrewNumerals {
    static func toInt(_ s: String) -> Int?
    static func toHebrew(_ n: Int) -> String   // 1..400
    // Respecte 15=טו, 16=טז
}
```

## Persistance

### Preferences (UserDefaults via `@AppStorage`)

| Clé | Type | Défaut |
|-----|------|--------|
| `pref.translation.fr` | Bool | false |
| `pref.theme` | String | "system" |
| `pref.textSize` | String | "m" |
| `pref.verseNumberStyle` | String | "hebrew" |
| `pref.dailyMode` | String | "monthly" |
| `pref.lastReadPsalmId` | Int | 0 |
| `pref.lastReadVerseId` | String | "" |

### FavoritesStore

JSON dans `Application Support/favorites.json`. Liste de `Int` (psalm IDs).

### LastReadStore

Combiné aux préférences ci-dessus.

## Modules / cibles

V1 : un seul target app + un target tests. Pas de Swift Package interne.
V2 envisageable : extraire `TehilimCore` en SwiftPM si la base se stabilise.

## Dépendances externes

Aucune en V1. Tout est natif Apple.

## Observabilité

Aucun analytics V1. Logs locaux via `os.Logger`.

## Sécurité

- Aucune donnée sensible.
- Pas de stockage chiffré requis.
- Pas d'authent.
- Pas d'accès photo/contacts/etc.
