# Tehilim — App iPhone

Application iOS native (SwiftUI) dédiée à la lecture des Tehilim (Psaumes).

## Pour tester l'app sur ton Mac

### 1. Pré-requis (à faire une fois)

```bash
# Accepter la licence Xcode
sudo xcodebuild -license

# Outil pour générer le projet Xcode
brew install xcodegen
```

### 2. Générer le projet Xcode

```bash
cd /Users/dadoou/TEHILIM
make project
make open
```

`make project` exécute `xcodegen generate` à partir de `project.yml`. `make open` ouvre `Tehilim.xcodeproj` dans Xcode.

### 3. Lancer dans le simulateur

Dans Xcode : choisir un simulateur iPhone 15 (iOS 17+), puis ⌘R.

### 4. (Optionnel) Re-télécharger le corpus

Le fichier `data/psalms.json` est déjà rempli (150 psaumes hébreu + traduction française Louis Segond 1910). Pour le régénérer :

```bash
pip3 install --user -r scripts/requirements.txt
make fetch
```

### 5. (Optionnel) Lancer les tests unitaires

```bash
make test
```

## Sources de contenu (intégrées)

- **Hébreu** : Sefaria — *Miqra according to the Masorah*, te'amim retirés, nikud conservé. Texte massorétique, **domaine public**.
- **Français** : bolls.life — *Bible Louis Segond 1910* (FRLSG), **domaine public**.
- 100 % des 2 527 versets ont une traduction française.

> ⚠️ La traduction Louis Segond est protestante. Si tu obtiens plus tard une licence pour *La Bible du Rabbinat* (Zadoc Kahn 1899) ou une autre traduction juive, le schéma JSON et le code SwiftUI sont prêts à l'accueillir : il suffit de remplacer le contenu de `data/psalms.json` (champ `translationFR` par verset).

## Structure du dépôt

```
TEHILIM/
├── README.md
├── Makefile                      ← raccourcis : setup, fetch, project, open, test
├── project.yml                   ← config XcodeGen
├── docs/                         ← spécifications produit / UX / UI / tech / contenu / design / QA
├── data/                         ← contenu JSON embarqué dans l'app
│   ├── psalms.json               ← 150 psaumes HE+FR (généré par scripts/fetch_psalms.py)
│   ├── translations_fr.schema.json
│   ├── life_cases.json
│   ├── psalm_119_sections.json
│   └── daily_reading_rules.json
├── scripts/
│   ├── fetch_psalms.py           ← script de génération du corpus
│   └── requirements.txt
└── ios/
    ├── Tehilim/                  ← scaffold SwiftUI
    │   ├── App/                  ← TehilimApp, AppContainer, RootTabView
    │   ├── Core/                 ← Models, Services, Persistence, Search
    │   ├── Features/             ← Home, Psalms, Search, Daily, LifeCases, Psalm119, Settings, Favorites
    │   ├── Shared/               ← Components, Theme
    │   └── Resources/            ← Info.plist, Localizable.strings
    └── TehilimTests/             ← tests unitaires (HebrewNumerals, SearchInterpreter, DailyEngine)
```

## Confidentialité

Aucune donnée personnelle n'est collectée. Aucune connexion réseau n'est nécessaire au fonctionnement principal.
