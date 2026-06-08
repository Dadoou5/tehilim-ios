# Tehilim — Applications iOS & Android

Applications natives dédiées à la lecture quotidienne et contextuelle des Tehilim.

- **iOS** : SwiftUI · iOS 17+ — V1.14.0 (build 65).
- **Android** : Jetpack Compose · Material 3 · min SDK 26 (Android 8) — V1.14.0 (versionCode 48).

> Sobre, premium, le texte comme objet principal. UI bilingue **FR + EN** (hébreu + translittération + traduction).
>
> **La lecture reste 100 % locale et sans collecte** (corpus embarqué, préférences sur l'appareil). Une **fonctionnalité collaborative optionnelle** — la **Chaîne de Tehilim** — repose, elle seule, sur un backend **Supabase** (Postgres + Realtime + auth anonyme + notifications push) et ne stocke que ce que l'utilisateur saisit pour participer (prénom d'affichage + intention), supprimé automatiquement après la lecture. Voir [Confidentialité](#confidentialité).

---

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Sources de contenu](#sources-de-contenu)
- [Pour lancer l'app (iOS)](#pour-lancer-lapp-ios)
- [Port Android](#port-android)
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
- Tailles de texte hébreu et français **paramétrables séparément** (8 paliers : Très petit → Maximum), réglables **en cours de lecture** via le bouton « Aa » (popover/menu A− / A+, persisté) — disponible aussi sur les sections du Tehilim 119 et la lecture depuis une chaîne.
- **Numéro du Tehilim toujours visible** dans le contenu (en plus du titre de la barre).
- Numérotation des versets en lettres hébraïques ou chiffres arabes.
- Mode clair / sombre / système.
- Navigation prev/next **contextuelle** : depuis Favoris ou Cas de la vie, on reste dans la liste correspondante.

### Découverte
- **5 livres** classiques (1–41 / 42–72 / 73–89 / 90–106 / 107–150).
- **18 cas de la vie** (dont **Réussite**) organisés en 4 sections (Cycle de vie, Santé et épreuves, Spiritualité, Communauté et calendrier) avec **validation rabbinique reçue**.
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

### Chaîne de Tehilim (collaboratif — optionnel)
Lecture collective des 150 Tehilim répartie entre participants, pour une intention (Lelouy Nichmat / Refoua Chelema / Réussite).
- **Création** : intention + détail, durée de sélection, échéance de lecture ; partage par **lien WhatsApp** + **QR code** aux couleurs de l'app.
- **Sélection en temps réel** : chacun rejoint avec un prénom, choisit les Tehilim qu'il s'engage à lire ; un Tehilim pris est **verrouillé** (PK exclusive côté Postgres). Sélection optimiste (réactivité immédiate).
- **Liste « Mes chaînes »** en 3 catégories claires : **Sélection en cours** · **Lecture en cours** · **Terminées**, avec compte à rebours `HH:MM:SS` (gros + gras), bascule automatique et suppression locale.
- **Maître de la chaîne** : éditer, **prolonger la sélection** (par durée, ≤ 48 h), m'attribuer les restants, **clôturer & distribuer**, retirer un participant, supprimer.
- **Notifications push** (APNs + FCM) aux participants : seuils 70/80/90/100 %, distribution, suppression, rappels « il reste N à prendre » (80 %) et « dernière chance » (95 %), prolongation.
- **Lecture hors-ligne** : une chaîne distribuée est mise en cache localement → on peut lire ses Tehilim en **mode avion**.
- **Cycle de vie** : conservation Supabase jusqu'à *fin de lecture + 7 jours*, puis suppression automatique (pg_cron). L'app reste 100 % locale si le backend n'est pas configuré (dégradation propre).

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
| Traduction anglaise | [Sefaria — JPS 1917 Tanakh](https://www.sefaria.org) | Domaine public |
| Catégorisation cas de la vie | Compilations classiques (*Tehillot Hashem*) | Validé rabbiniquement (cf. `docs/05-content/content_validation_notes.md`) |

Toutes les sources sont citées dans l'app : **Réglages → Sources du contenu**.

---

## Pour lancer l'app (iOS)

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

## Port Android

Port natif **Jetpack Compose** + **Material 3** mirror 1:1 de l'app iOS. Mêmes données (les JSON sont partagés depuis `data/`), même comportement, même typographie.

### Pré-requis
- **Android Studio Hedgehog+** (AGP 8.5+, Kotlin 2.0+).
- **JDK 17** (`brew install --cask temurin@17`).
- Min SDK **26** (Android 8) — Target SDK 34.

### Lancer
```bash
cd android
./gradlew installDebug          # build + push sur device/émulateur connecté
# ou ouvrir le dossier `android/` dans Android Studio puis ⌃R.
```

### Parité fonctionnelle avec iOS

| Domaine | iOS | Android | Notes |
|---------|-----|---------|-------|
| 150 psaumes HE + traduction | ✅ | ✅ | JSON `data/psalms.json` partagé |
| Cas de la vie (18) | ✅ | ✅ | JSON `data/life_cases.json` partagé, EN inclus |
| Tehilim 119 (22 lettres) | ✅ | ✅ | Toggle traduction local en V1.3.12 |
| Tikkoun HaKlali | ✅ | ✅ | |
| Recherche tolérante FR/HE/mixte | ✅ | ✅ | |
| Favoris locaux | ✅ | ✅ | DataStore Preferences |
| Mode phonétique sépharade | ✅ | ✅ | Algorithme partagé en Kotlin |
| Notifications quotidiennes | ✅ | ✅ | AlarmManager + WorkManager |
| Widget date hébraïque | ✅ (WidgetKit) | ✅ (Glance) | Tailles small/medium/large |
| Partage de verset (image) | ✅ | ✅ | Bitmap 1080×1080 + FileProvider |
| Onboarding 1ʳᵉ utilisation | ✅ | ✅ | |
| Iluy Nishmat dédicace | ✅ | ✅ | |
| Lelouy Nichmat (séquences) | ✅ | ✅ | |
| Taille du texte en lecture (Aa, 8 paliers) | ✅ | ✅ | Persistée via préférences |
| **Chaîne de Tehilim** (collaboratif) | ✅ | ✅ | Backend Supabase ; QR + lien ; lecture hors-ligne |
| Notifications push de chaîne | ✅ (APNs) | ✅ (FCM) | Via Edge Function `notify` |

### Spécificités Android
- **Langue UI** : FR / EN / Système via `LocaleManager.applicationLocales` (API 33+, fallback `AppCompatDelegate` pour API 26–32). Voir `MainActivity.attachBaseContext()`.
- **Date hébraïque** : `android.icu.HebrewCalendar` (ICU/CLDR officiel), pas de dépendance tierce.
- **Polices** : Frank Ruhl Libre + Pinyon Script bundlées en `res/font/` (mêmes fichiers OFL qu'iOS pour parité visuelle), Ezra SIL SR pour le texte hébreu, `FontFamily.Serif` (Noto Serif) pour le corps FR/EN, `FontFamily.Monospace` pour les numéros de verset.
- **Sauvegarde cloud** : Google Play Auto Backup (DataStore + favorites.json) — pas d'iCloud KVS côté Android, l'OS gère.
- **Pas de framework DI** : container manuel `AppContainer` injecté dans les Composables (mirror du conteneur SwiftUI).

### Roadmap Android

| Version | Périmètre | Statut |
|---------|-----------|--------|
| **V1.0–V1.2** | Mirror MVP iOS, navigation 5 onglets, widget Glance | ✅ Livrée |
| **V1.3.0** | UI bilingue FR + EN (~265 string keys) | ✅ Livrée |
| **V1.3.1** | Typographie alignée iOS (Frank Ruhl Libre + Pinyon Script) | ✅ Livrée |
| **V1.3.2** | Cas de la vie traduits EN | ✅ Livrée |
| **V1.3.3–V1.3.11** | Itérations bascule de langue à chaud (Android 13–16) | ✅ Livrée |
| **V1.3.12** | Toggle traduction sur sections Tehilim 119 + séparateurs hairline | ✅ Livrée |
| **V1.4** | Publication Play Store closed testing | 📋 Planifiée |

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
- **Chaîne de Tehilim** → cache local des chaînes connues + instantané hors-ligne (`ChainArchiveStore`).

**Backend (Chaîne de Tehilim uniquement)** — **Supabase** :
- **Postgres** : `chains` / `chain_participants` / `chain_assignments` (PK `(chain_id, psalm_id)` = verrou exclusif) + `device_tokens`. RLS active.
- **Realtime** : propagation live des sélections / participants.
- **Auth** anonyme.
- **Edge Function `notify`** (Deno) : envoi APNs + FCM, parallélisé, auto-purge des jetons morts.
- **pg_cron** : rappels de sélection (toutes les 5 min) + nettoyage quotidien des chaînes expirées.
- Config injectée hors dépôt (`Supabase-Info.plist` iOS / `supabase.properties` Android) ; absente → feature désactivée.
- Migrations dans `supabase/migrations/`, fonction dans `supabase/functions/notify/`.

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
│   ├── V1.6_APP_GROUP.md             ← partage app↔widget
│   ├── V1.14_CHAIN.md                ← Chaîne de Tehilim + lecture + cas de la vie
│   ├── supabase-setup.md             ← config backend chaîne
│   └── push-setup.md                 ← secrets APNs/FCM (Edge Function)
├── data/                             ← contenu JSON embarqué dans l'app
│   ├── psalms.json                   ← 150 psaumes HE+FR (Sefaria + Beth Loubavitch)
│   ├── life_cases.json               ← 18 cas validés (4 sections)
│   ├── psalm_119_sections.json       ← 22 sections du Tehilim 119
│   ├── daily_reading_rules.json      ← cycles mensuel + hebdo
│   └── translations_fr.schema.json   ← schéma traductions
├── scripts/                          ← scripts Python pour régénérer les JSONs
│   ├── fetch_psalms.py               ← Sefaria
│   ├── fetch_letehilim.py            ← Beth Loubavitch
│   ├── merge_letehilim.py            ← merge des deux sources
│   └── requirements.txt
├── supabase/                         ← backend Chaîne de Tehilim (optionnel)
│   ├── migrations/                   ← schéma Postgres + RLS + RPC + pg_cron
│   └── functions/notify/             ← Edge Function push (APNs + FCM)
├── android/                          ← port Jetpack Compose (parité iOS)
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

### iOS

| Version | Périmètre | Statut |
|---------|-----------|--------|
| **V1.0** | MVP : 150 Tehilim, 5 livres, recherche, cas de la vie, Tehilim 119, favoris, accessibilité | ✅ Livrée |
| **V1.1** | Mode phonétique sépharade, tailles HE/FR séparées | ✅ Livrée |
| **V1.2** | Notifications quotidiennes paramétrables + deep link | ✅ Livrée |
| **V1.3** | Prières avant/après, navigation contextuelle, palette bleu d'eau | ✅ Livrée |
| **V1.4** | Tikkoun HaKlali, partage de verset, onboarding | ✅ Livrée |
| **V1.5** | Widget WidgetKit 3 tailles avec design enrichi | ✅ Livrée |
| **V1.6** | App Group : pref `dailyMode` partagée app↔widget | ✅ Livrée |
| **V1.9.5** | **iPad universel** — NavigationSplitView 2 colonnes (`PsalmsTabView`), sidebar `IPadPsalmsSidebar`, side-by-side translation (`VerseRowView.sideBySideLayout`), `AdaptiveLayout` (breakpoint 900pt), `@Environment(\.horizontalSizeClass)` sur tous les écrans | ✅ Livrée |
| **V1.10.5** | Lelouy Nichmat, séquences personnalisées, polish UI | ✅ Livrée |
| **V1.10.6** | Robustesse App Group, privacy iCloud KVS, build 23 sur App Store | ✅ Publiée |
| **V2.1.a** | iCloud KVS étendu — snapshot Codable des 14 prefs (textSize HE/FR, theme, textMode, appLanguage, dailyMode, notifs, lastRead, onboarding…) synchronisé via `Preferences.swift`. Favoris + Lelouy Nichmat déjà en V1.10.5. | ✅ Livrée |
| **V2.1.b** | Anglais UI iOS — bascule à chaud via swizzle `Bundle.main` + helper `L()` (LocalizedBundle.swift) + `.id(appLanguage)` sur racine SwiftUI, widget bilingue (lproj + AppGroup pour la pref partagée), pickers/prières suivent la locale, trous `en.lproj` comblés, alerte « Redémarrage requis » supprimée | ✅ Livrée |
| **V1.11.0** | **Release publique** de V2.1.a + V2.1.b — bump 1.10.6→1.11.0, build 23→24, What's New, soumission App Store | 📋 ~0,5 j |
| **V2.0.a** | Mode Shabbat — détection auto Shabbat / Yom Tov / Roch Hodech via calendrier hébraïque + sets de Tehilim associés (Hallel, Shabbat, fêtes) | 📋 ~4–5 j |
| **V2.0.b** | Zmanim — liste 30 villes pré-définies + désactivation notifs pendant Shabbat | 📋 ~3 j |
| **V2.0.d** | CoreLocation pour zmanim précis (opt-in) | 💡 ~2 j |
| **V2.5** | Apple Watch companion, audio récitation | 💡 |
| **V3.0** | Commentaires PaRDeS multi-couches | 💡 |

### Android

Voir le tableau dédié dans la section [Port Android](#port-android) ci-dessus.

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

### Lecture (cœur de l'app)
- **Aucune donnée personnelle collectée**, **aucun tracker / analytics**, **aucune connexion réseau** nécessaire.
- Préférences et favoris stockés **localement** (UserDefaults / Application Support ; DataStore côté Android), avec synchronisation **iCloud KVS** des favoris/Lelouy Nichmat (gérée par Apple, hors de portée de l'éditeur).
- Le rappel quotidien est une **notification locale**.

### Chaîne de Tehilim (collaboratif, opt-in)
La seule fonctionnalité qui sort de l'appareil. Elle utilise **Supabase** :
- **Auth anonyme** (identifiant aléatoire par appareil, aucun compte, aucun e-mail).
- Données stockées **uniquement le temps de la chaîne** : prénom d'affichage choisi, type + détail d'intention, sélection des Tehilim, jeton de notification push de l'appareil.
- **Suppression automatique** *fin de lecture + 7 jours* (pg_cron) ; un participant peut retirer une chaîne de sa liste à tout moment.
- Sécurité serveur par **Row Level Security** (un utilisateur n'écrit que ses propres données ; verrou exclusif par Tehilim).
- Si le backend n'est pas configuré dans la build, la feature est **désactivée** et l'app reste 100 % locale.

Détails complets : [`PRIVACY.md`](./PRIVACY.md), `docs/V1.14_CHAIN.md`, `docs/supabase-setup.md`.

---

## Validation rabbinique

✅ **Validation reçue** par le porteur projet (2026-05-08) sur :
- Découpages des cycles mensuel et hebdomadaire.
- Liste des cas "cas de la vie" et leurs Tehilim associés (18 cas en V1.14).
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
