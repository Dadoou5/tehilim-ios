# Tehilim — Application iPhone

Application iOS native (**SwiftUI · iOS 17+**) dédiée à la lecture quotidienne et contextuelle des Tehilim.

> Sobre, premium, hors-ligne, sans collecte de données. Le texte est l'objet principal.

---

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Sources de contenu](#sources-de-contenu)
- [Pour lancer l'app](#pour-lancer-lapp)
- [Architecture](#architecture)
- [Structure du dépôt](#structure-du-dépôt)
- [Roadmap](#roadmap)
- [Tests](#tests)
- [Confidentialité](#confidentialité)
- [Validation rabbinique](#validation-rabbinique)
- [Workflow Git](#workflow-git)

---

## Fonctionnalités

### Lecture des Tehilim
- 150 psaumes en **hébreu vocalisé** (nikud, sans téamim) + traduction française.
- Mode **phonétique sépharade** activable (transcription algorithmique avec règles Tetragrammaton, ḥiriq-yod, etc.).
- Tailles de texte hébreu et français **paramétrables séparément** (XS → XL, AAA contraste).
- Numérotation des versets en lettres hébraïques ou chiffres arabes.
- Mode clair / sombre / système.
- Navigation prev/next **contextuelle** : depuis Favoris ou Cas de la vie, on reste dans la liste correspondante.

### Découverte
- **5 livres** classiques (1–41 / 42–72 / 73–89 / 90–106 / 107–150).
- **17 cas de la vie** organisés en 4 sections (Cycle de vie, Santé et épreuves, Spiritualité, Communauté et calendrier) avec **validation rabbinique reçue**.
- **Tehilim 119** par 22 lettres de l'alphabet hébreu.
- **Tikkoun HaKlali** (Rabbi Nachman de Breslev — 10 psaumes).

### Aujourd'hui
- **Tehilim du jour** selon cycle mensuel (calendrier hébraïque) ou jour de la semaine.
- **Date hébraïque** affichée sur l'accueil (et dans le widget).
- **Prière avant** + **Prière après** (Tehilim 95:1-3 et 14:7+37:39-40).

### Recherche
- Saisie en chiffres arabes (`23`), lettres hébraïques (`כג`), ou mixte tolérante (`tehilim 23`, `psaume 23`, `תהילים כג`).
- Suggestions + récents.

### Personnel
- **Favoris** locaux.
- **Reprise de lecture**.
- **Iluy Nishmat** : dédicace en tête de chaque psaume.
- **Partage de verset** : long-press → menu Copier / Partager (image carrée 1080×1080 avec attribution).

### Notifications & Widget
- **Rappel quotidien** paramétrable (heure choisie, deep link vers Aujourd'hui).
- **Widget WidgetKit** « Tehilim du jour » 3 tailles (small/medium/large) avec date hébraïque, liste des psaumes du jour, mode actif.
- **App Group** pour synchroniser le mode utilisateur entre l'app et le widget.

### Onboarding
- 3 écrans courts au premier lancement (skip-able), ne réapparait plus ensuite.

### Accessibilité
- Conforme **WCAG 2.1 AA** (la plupart des paires AAA — voir `docs/06-design/DESIGN_SYSTEM.md`).
- VoiceOver optimisé : versets lus avec `accessibilityLabel` combiné, traits sémantiques (`.isHeader`, `.isButton`).
- **Dynamic Type** jusqu'à AX5.
- **Reduce Motion** respecté.
- Cibles tactiles ≥ 44 × 44 pt.

---

## Sources de contenu

| Élément | Source | Licence |
|---------|--------|---------|
| Texte hébreu | [Sefaria](https://www.sefaria.org) — *Miqra according to the Masorah* (te'amim retirés, nikud conservé) | Domaine public |
| Traduction française | [Beth Loubavitch — le-tehilim.online](https://le-tehilim.online) (8 rue Lamartine, 75009 Paris — `chabad@loubavitch.fr`) | **Autorisation expresse reçue.** Mention obligatoire affichée dans Réglages → Sources de l'app. |
| Catégorisation cas de la vie | Compilations classiques (*Tehillot Hashem*) | Validé rabbiniquement (cf. `docs/05-content/content_validation_notes.md`) |

Toutes les sources sont citées dans l'app : **Réglages → Sources du contenu**.

---

## Pour lancer l'app

### Pré-requis
- macOS avec **Xcode 15+** (iOS 17 SDK).
- **XcodeGen** (binaire fourni dans `.tools/` ou via `brew install xcodegen`).
- Optionnel : Python 3 pour régénérer les données (`pip3 install -r scripts/requirements.txt`).

### En une commande
```bash
git clone https://github.com/Dadoou5/tehilim-ios.git
cd tehilim-ios
make open
```

`make open` :
1. Génère `Tehilim.xcodeproj` à partir de `project.yml` (XcodeGen).
2. Ouvre le projet dans Xcode.

Puis dans Xcode : **⌘R** sur un simulateur iPhone 17 (iOS 17+).

### Commandes utiles
```bash
make project    # (re)génère le .xcodeproj
make open       # génère + ouvre Xcode
make test       # lance les tests unitaires (32 tests)
make fetch      # re-télécharge le corpus Tehilim depuis Sefaria + Beth Loubavitch
make clean      # supprime le .xcodeproj généré
```

### Régénération du corpus
```bash
DEVELOPER_DIR=/Library/Developer/CommandLineTools /usr/bin/python3 scripts/fetch_psalms.py
DEVELOPER_DIR=/Library/Developer/CommandLineTools /usr/bin/python3 scripts/fetch_letehilim.py
DEVELOPER_DIR=/Library/Developer/CommandLineTools /usr/bin/python3 scripts/merge_letehilim.py
```

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│  TehilimApp (target principal)                  │
│  ┌─ App ─────────────────┐                      │
│  │ TehilimApp            │                      │
│  │ AppContainer (DI)     │                      │
│  │ RootTabView           │                      │
│  │ TabRouter             │                      │
│  └───────────────────────┘                      │
│                                                 │
│  ┌─ Features ────────────┐                      │
│  │ Home                  │                      │
│  │ Psalms                │                      │
│  │ Search / Daily        │                      │
│  │ LifeCases / Psalm119  │                      │
│  │ Settings / Favorites  │                      │
│  │ Onboarding / Prayers  │                      │
│  │ Sharing               │                      │
│  └───────────────────────┘                      │
│                                                 │
│  ┌─ Core ────────────────┐                      │
│  │ Models                │                      │
│  │ Services              │                      │
│  │   ContentLoader       │                      │
│  │   PsalmRepository     │                      │
│  │   LifeCaseRepository  │                      │
│  │   DailyEngine         │                      │
│  │   HebrewNumerals      │                      │
│  │   HebrewDateFormatter │                      │
│  │   HebrewTransliterator│                      │
│  │   NotificationManager │                      │
│  │   AppGroup            │                      │
│  │ Persistence           │                      │
│  │   Preferences         │                      │
│  │   FavoritesStore      │                      │
│  │ Search                │                      │
│  │   SearchInterpreter   │                      │
│  └───────────────────────┘                      │
└─────────────────────────────────────────────────┘
              │ App Group
              │ group.com.david.tehilim
              ▼
┌─────────────────────────────────────────────────┐
│  TehilimWidget (extension WidgetKit)            │
│  ┌─ DailyVerseWidget ───────────────┐           │
│  │ TimelineProvider                 │           │
│  │ View (3 tailles : S / M / L)     │           │
│  │ Sources partagées avec l'app :   │           │
│  │   Psalm, DailyRules, ...         │           │
│  │   AppGroup, HebrewDateFormatter  │           │
│  └──────────────────────────────────┘           │
└─────────────────────────────────────────────────┘
```

**Pattern** : MVVM léger + DI manuel (`AppContainer`). Pas de framework tiers. Apple Frameworks uniquement.

**Storage** :
- `UserDefaults.standard` → préférences locales de l'app.
- `UserDefaults(suiteName: "group.com.david.tehilim")` → préférences partagées avec le widget (mode quotidien).
- `Application Support/favorites.json` → favoris.
- Bundle (read-only) → corpus JSON.

---

## Structure du dépôt

```
TEHILIM/
├── README.md                         ← ce fichier
├── Makefile                          ← raccourcis : project, open, test, fetch
├── project.yml                       ← config XcodeGen (2 targets)
├── docs/                             ← spécifications complètes
│   ├── 01-product/                   ← PRD, ROADMAP, BACKLOG, ACCEPTANCE
│   ├── 02-ux/                        ← FLOWS, IA, WIREFRAMES
│   ├── 03-ui/                        ← UI_SPEC, COMPONENT_LIBRARY, HANDOFF_HOME
│   ├── 04-tech/                      ← ARCHITECTURE, IMPLEMENTATION_PLAN
│   ├── 05-content/                   ← TEHILIM_CONTENT_SPEC, validation_notes
│   ├── 06-design/                    ← DESIGN_SYSTEM, IOS_STYLE_GUIDE
│   ├── 07-qa/                        ← TEST_PLAN, ACCESSIBILITY, NAVIGATION_TEST
│   ├── CROSS_REVIEW.md
│   ├── V1.1_PHONETIC.md              ← phonétique sépharade
│   ├── V1.2_NOTIFICATIONS.md         ← rappel quotidien
│   ├── V1.4_QUICK_WINS.md            ← Tikkoun, partage, onboarding
│   ├── V1.5_WIDGET.md                ← widget WidgetKit
│   └── V1.6_APP_GROUP.md             ← partage app↔widget
├── data/                             ← contenu JSON embarqué dans l'app
│   ├── psalms.json                   ← 150 psaumes HE+FR (Sefaria + Beth Loubavitch)
│   ├── life_cases.json               ← 17 catégories validées
│   ├── psalm_119_sections.json       ← 22 sections du Tehilim 119
│   ├── daily_reading_rules.json      ← cycles mensuel + hebdo
│   └── translations_fr.schema.json   ← schéma traductions
├── scripts/                          ← scripts Python pour régénérer les JSONs
│   ├── fetch_psalms.py               ← Sefaria
│   ├── fetch_letehilim.py            ← Beth Loubavitch
│   ├── merge_letehilim.py            ← merge des deux sources
│   └── requirements.txt
└── ios/
    ├── Tehilim/                      ← target principal
    │   ├── App/                      ← TehilimApp, AppContainer, RootTabView, TabRouter
    │   ├── Core/
    │   │   ├── Models/
    │   │   ├── Services/
    │   │   │   ├── ContentLoader, PsalmRepository, LifeCaseRepository
    │   │   │   ├── DailyEngine, HebrewNumerals, HebrewDateFormatter
    │   │   │   ├── HebrewTransliterator, NotificationManager
    │   │   │   └── AppGroup                ← App Group identifier + UserDefaults
    │   │   ├── Persistence/          ← Preferences, FavoritesStore
    │   │   └── Search/               ← SearchInterpreter
    │   ├── Features/
    │   │   ├── Home/                 ← Accueil (date hébraïque, cartes, prières)
    │   │   ├── Psalms/               ← BookListView, PsalmListView, PsalmDetailView
    │   │   ├── Search/               ← SearchView avec interprétation tolérante
    │   │   ├── Daily/                ← Tehilim du jour + sheet mode
    │   │   ├── LifeCases/            ← liste sectionnée + détail
    │   │   ├── Psalm119/             ← grille 22 lettres + sections
    │   │   ├── Settings/             ← réglages + accessibilité
    │   │   ├── Favorites/            ← liste favoris
    │   │   ├── Onboarding/           ← 3 écrans premier lancement
    │   │   ├── Prayers/              ← PrayerView (avant/après)
    │   │   └── Sharing/              ← VerseShareCard (image 1080×1080)
    │   ├── Shared/
    │   │   ├── Components/           ← VerseRow, EmptyState, IluyNishmat...
    │   │   └── Theme/                ← Colors, Typography, ViewModifiers
    │   ├── Resources/
    │   │   ├── Info.plist            ← URL scheme tehilim://
    │   │   ├── Localizable.strings
    │   │   └── Assets.xcassets/      ← couleurs nommées + AppIcon
    │   └── Tehilim.entitlements      ← App Group
    ├── TehilimTests/                 ← 32 tests unitaires XCTest
    │   ├── HebrewNumeralsTests
    │   ├── HebrewTransliteratorTests
    │   ├── SearchInterpreterTests
    │   └── DailyEngineTests
    └── TehilimWidget/                ← extension WidgetKit
        ├── Info.plist
        ├── TehilimWidget.entitlements ← App Group
        ├── TehilimWidgetBundle.swift
        ├── DailyVerseWidget.swift
        ├── DailyVerseProvider.swift
        ├── DailyVerseWidgetView.swift
        ├── WidgetDataLoader.swift
        └── WidgetTheme.swift
```

---

## Roadmap

| Version | Périmètre | Statut |
|---------|-----------|--------|
| **V1.0** | MVP : 150 Tehilim, 5 livres, recherche, cas de la vie, Tehilim 119, favoris, accessibilité | ✅ Livrée |
| **V1.1** | Mode phonétique sépharade, tailles HE/FR séparées | ✅ Livrée |
| **V1.2** | Notifications quotidiennes paramétrables + deep link | ✅ Livrée |
| **V1.3** | Prières avant/après, navigation contextuelle, palette bleu d'eau | ✅ Livrée |
| **V1.4** | Tikkoun HaKlali, partage de verset, onboarding | ✅ Livrée |
| **V1.5** | Widget WidgetKit 3 tailles avec design enrichi | ✅ Livrée |
| **V1.6** | App Group : pref `dailyMode` partagée app↔widget | ✅ Livrée |
| **V2.0** | Mode Shabbat auto-détecté, fêtes juives, iPad universel | 📋 Planifiée |
| **V2.1** | Anglais UI, iCloud sync favoris/réglages | 📋 |
| **V2.5** | Apple Watch companion, audio récitation | 💡 |
| **V3.0** | Commentaires PaRDeS multi-couches | 💡 |

Détails par version dans `docs/V*.md`.

---

## Tests

```bash
make test
```

**32 tests unitaires** couvrant :
- `HebrewNumeralsTests` (15 tests) — gematria 1..400, cas spéciaux 15/16.
- `HebrewTransliteratorTests` (15 tests) — règles sépharades, tétragramme, mater lectionis.
- `SearchInterpreterTests` (8 tests) — saisies arabes/hébreues/mixtes/dégradées.
- `DailyEngineTests` (3 tests) — modes mensuel/hebdomadaire/custom.

Tests manuels documentés dans :
- `docs/07-qa/NAVIGATION_TEST_PLAN.md` (40 tests, 8 suites)
- `docs/07-qa/QA_CHECKLIST.md`
- `docs/07-qa/TEST_CASES.md`

---

## Confidentialité

- **Aucune donnée personnelle collectée.**
- **Aucun tracker, aucune analytics.**
- **Aucune connexion réseau** nécessaire au fonctionnement principal.
- Toutes les préférences et favoris sont stockés **localement** (UserDefaults / Application Support).
- Les notifications sont **locales** (pas de push remote).

Détails dans `Réglages → Confidentialité` de l'app et `docs/07-qa/ACCESSIBILITY.md`.

---

## Validation rabbinique

✅ **Validation reçue** par le porteur projet (2026-05-08) sur :
- Découpages des cycles mensuel et hebdomadaire.
- Liste des 17 catégories "cas de la vie" et leurs Tehilim associés.
- Choix du tétragramme rendu par "Adonaï" en phonétique.

Les notes éditoriales **ne contiennent aucune promesse** (médicale, juridique, financière, psychologique). Détails dans `docs/05-content/content_validation_notes.md`.

---

## Workflow Git

```bash
# état
git status

# nouveau commit
git add -A
git commit -m "Description"
git push
```

URL du repo : **https://github.com/Dadoou5/tehilim-ios**

---

## Crédits

- **Texte hébreu** : [Sefaria](https://www.sefaria.org)
- **Traduction française** : [Beth Loubavitch](https://le-tehilim.online)
- **Développement** : David Bouganim
