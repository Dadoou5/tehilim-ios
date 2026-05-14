# Tehilim Android

Port Android du projet iOS Tehilim — même corpus, même design, **fonctionnalité iso** avec V1.10.5 iOS.

## 🏗️ Stack technique

- **Kotlin 2.0** + **Jetpack Compose** (Material 3)
- **min SDK 26** (Android 8.0) · **target SDK 35** (Android 15)
- **DataStore Preferences** (UserDefaults équivalent)
- **kotlinx.serialization** pour les JSON (Codable équivalent)
- **Navigation Compose** (5 tabs)
- **kotlinx.datetime** pour le calendrier
- **Glance** pour le widget (V2)

## 📁 Architecture

```
android/
├── app/src/main/java/com/david/tehilim/
│   ├── core/
│   │   ├── model/           # Psalm, LifeCase, Psalm119Section, Prayer, PersonalizedReading...
│   │   ├── service/         # ContentLoader, repositories, DailyEngine, generators
│   │   └── persistence/     # Preferences (DataStore), FavoritesStore, SavedPrayerStore
│   ├── ui/
│   │   ├── theme/           # Material 3 + tokens matching iOS (bleu eau)
│   │   └── components/      # VerseRow, IluyNishmatBanner, AppCard, EmptyState
│   ├── features/
│   │   ├── home/            # HomeScreen
│   │   ├── psalms/          # PsalmsScreen (segmented) + PsalmList + PsalmDetail
│   │   ├── daily/           # DailyScreen (Tehilim du jour)
│   │   ├── lifecases/       # LifeCasesScreen + LifeCaseDetailScreen
│   │   ├── psalm119/        # Grille 22 lettres + Section
│   │   ├── personalized/    # Lelouy Nichmat (form + list + saved)
│   │   └── settings/        # SettingsScreen
│   ├── navigation/          # AppNavigation + Destinations
│   ├── TehilimApplication.kt
│   ├── AppContainer.kt      # DI manuel (équivalent iOS AppContainer)
│   └── MainActivity.kt
├── app/src/main/assets/data/    # JSON (copié depuis ../data au build via Gradle)
├── app/src/main/res/font/       # Ezra SIL SR (copiée depuis iOS au build via Gradle)
└── app/src/test/                # JUnit tests (mapper + generator)
```

## 🔄 Source de vérité partagée avec iOS

Le dossier `/data` à la racine du repo est **commun aux deux plateformes** :
- `psalms.json` (150 Tehilim avec hébreu + FR + EN)
- `life_cases.json` (18 cas de la vie)
- `daily_reading_rules.json` (cycle mensuel / hebdomadaire)
- `letehilim.json` (corpus alternatif)

Une task Gradle (`copySharedAssets`) copie ces fichiers vers `app/src/main/assets/data/`
au build. **Modifie uniquement `/data` à la racine** — jamais le contenu copié.

La police hébraïque **Ezra SIL SR** est aussi partagée (copiée depuis `ios/Tehilim/Resources/Fonts/`).

## 🚀 Premier build

### Prérequis
- **Android Studio Ladybug** (2024.2.1) ou plus récent
- **JDK 17** (bundled avec Android Studio)
- Aucun SDK Android manuel requis — Studio installera tout

### Étapes
```bash
# 1. Ouvre le projet
open -a "Android Studio" /Users/dadoou/TEHILIM/android

# 2. Android Studio :
#    - "Trust Project" si demandé
#    - Sync Gradle (en bas, "Gradle Sync") — télécharge dépendances
#    - File → Settings → SDK Manager → installer Android SDK 35 si pas déjà
#    - Build → Make Project (Cmd+F9)

# 3. Lancer
#    - Sélectionner un AVD ou device USB
#    - Run ▶ (Ctrl+R)
```

### En ligne de commande (après que Studio a généré le wrapper)
```bash
cd android
./gradlew assembleDebug    # APK debug
./gradlew test             # JUnit tests
./gradlew installDebug     # Installer sur device USB connecté
```

## 🧪 Tests

Mirror exact des tests iOS :
- `HebrewLetterMapperTest` (8 tests — finals → base, validation, filter)
- `LetterSequenceGeneratorTest` (10 tests — règle métier nechama, sources, dédup)

Exécuter : `./gradlew test`

## 📱 Status fonctionnel V1.1 Android (parité iOS V1.10.5)

| Feature | Status |
|---|---|
| 150 Tehilim avec hébreu + FR + EN | ✅ |
| 5 onglets + recherche globale | ✅ |
| Lecture détaillée avec téamim (Ezra SIL SR) | ✅ |
| **Calendrier hébraïque complet** (ICU HebrewCalendar) | ✅ |
| **Translittération phonétique sépharade** (port HebrewTransliterator) | ✅ |
| **HebrewDateBanner** (jour + translittération + RTL hébreu) | ✅ |
| Picker Livres / Tous / Favoris | ✅ |
| Tehilim 119 — grille 22 lettres enrichies (lettre + nom + range) | ✅ |
| Cas de la vie + détail | ✅ |
| **Prière avant / après** — ModalBottomSheet | ✅ |
| **Recherche** par numéro / hébreu / mot-clé | ✅ |
| Lelouy Nichmat — formulaire + auto-save + liste | ✅ |
| Sequence context Tehilim 119 (prev/next dans Lelouy) | ✅ |
| Réglages (thème, langue, taille texte, mode) | ✅ |
| **Notifications quotidiennes** (WorkManager) | ✅ |
| **Widget « Tehilim du jour »** (Glance) | ✅ |
| **Partage stylisé** d'un verset en image 1080×1080 (long-press) | ✅ |
| **Onboarding** 3 pages | ✅ |
| **About pages** (Sources, Confidentialité) | ✅ |
| **AdaptiveLayout** tablette (>600dp) + reading width cap | ✅ |
| DataStore (Preferences, Favoris, Prières) | ✅ |
| Tests JUnit (mapper, generator, transliterator) | ✅ |
| Auto-Backup Android (sync Drive auto) | ✅ |
| NavigationSplitView 2-col tablette | 🟡 V1.2 |
| Mode lecture parallèle (paysage tablette) | 🟡 V1.2 |
| TimePicker dialog pour heure notif | 🟡 V1.2 |

## 🌐 Différences UX vs iOS

| Aspect | iOS | Android |
|---|---|---|
| Navigation tabs | `TabView` bottom | `NavigationBar` bottom (Material 3) |
| Toolbar Tehilim 119 | `.toolbar` SwiftUI | `TopAppBar` Material 3 |
| Pickers | `SegmentedPickerStyle` | `PrimaryTabRow` |
| Sync | iCloud Key-Value Store | Backup Android (Drive auto) |
| Police hébraïque | Ezra SIL SR | Ezra SIL SR (identique) |
| Couleurs accent | Bleu eau `#1E6091` | Identique |
| Mode sombre | `@Environment(\.colorScheme)` | `isSystemInDarkTheme()` |

## 📋 Roadmap

### V1.0 (livré)
- MVP de lecture des 150 Tehilim
- Lelouy Nichmat avec auto-save
- Toutes les fonctionnalités principales

### V1.1 (prochaine)
- Notifications quotidiennes (WorkManager)
- Widget Glance « Tehilim du jour »
- Port complet du HebrewDateFormatter
- Port du HebrewTransliterator (mode phonétique)
- Partage stylisé d'un verset en image (Canvas API)

### V1.2
- Adaptation tablette (NavigationRail + Detail)
- Réorganisation des Tehilim par drag-drop
- Recherche full-text

## 🤝 Contributing

Le code Android suit la même architecture que iOS pour faciliter les évolutions
parallèles. Toute nouvelle feature côté iOS doit être pensée pour être portable
côté Android (modèles simples, JSON share-ready, séparation UI/logique).

## 📜 Licence

Privé — David Bouganim. Code propriétaire.
Polices : Ezra SIL SR sous SIL OFL.
Contenu : Sefaria (CC-BY), Beth Loubavitch (avec permission).
