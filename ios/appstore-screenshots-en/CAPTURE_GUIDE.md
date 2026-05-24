# Guide de capture iOS Simulator — Anglais V1.10.7

Procédure pour générer les 10 captures EN à uploader sur App Store Connect.
Total ~30 min de boulot (15 min iPhone + 15 min iPad).

---

## Pré-requis

- App Tehilim 1.10.7 (build 24) compilée → l'archive Xcode a déjà été
  faite OU on rebuild à la volée
- Simulateurs disponibles : iPhone 17 Pro Max + iPad Pro 13" (M5)

---

## A. iPhone 17 Pro Max (couvre 6.7"/6.9" iPhone)

### 1. Boot le simulateur en anglais

```bash
# Boot
xcrun simctl boot "iPhone 17 Pro Max"
open -a Simulator

# Wait it ready
xcrun simctl bootstatus "iPhone 17 Pro Max" -b

# Bascule iOS en anglais (US)
xcrun simctl spawn booted defaults write -g AppleLanguages -array en-US
xcrun simctl spawn booted defaults write -g AppleLocale -string en_US

# Restart Springboard pour appliquer la langue
xcrun simctl spawn booted launchctl stop com.apple.SpringBoard
```

Vérification : ouvre le simu, Settings doit afficher "Settings" et pas
"Réglages". Si la bascule n'a pas pris, fais-le manuellement via
Settings > General > Language & Region > Add Language > English.

### 2. Build + installer Tehilim

```bash
cd /Users/dadoou/TEHILIM

# Build pour le simulateur
xcodebuild -project Tehilim.xcodeproj -scheme Tehilim \
    -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max' \
    -configuration Debug \
    -derivedDataPath build/derived \
    build

# Install l'app
xcrun simctl install booted \
    build/derived/Build/Products/Debug-iphonesimulator/Tehilim.app

# Launch
xcrun simctl launch booted com.david.tehilim
```

### 3. Avant les captures — préparer un état "showcase"

Dans l'app :
- Active un favori (Tehilim 23 par exemple — tap le cœur)
- Note un Lelouy Nichmat avec un nom (יוסף בן שרה) pour avoir
  l'écran "Mes prières" non-vide
- Va dans Settings > Hebrew text size = Medium, Translation = Medium

### 4. Captures (10 écrans)

Navigue manuellement à chaque écran ci-dessous, puis lance la commande
correspondante depuis ce terminal :

```bash
OUT=/Users/dadoou/TEHILIM/ios/appstore-screenshots-en/iphone-6.7

# 01 — Home (Aujourd'hui tab)
xcrun simctl io booted screenshot $OUT/01_home.png

# 02 — Tehilim 23 detail (tap on a chip)
xcrun simctl io booted screenshot $OUT/02_psalm_detail.png

# 03 — 5 Books list (Explore tab > 5 Books)
xcrun simctl io booted screenshot $OUT/03_five_books.png

# 04 — Life Cases (Explore > Life Cases)
xcrun simctl io booted screenshot $OUT/04_life_cases.png

# 05 — Tehilim 119 grid (Explore > 119 AlphaBeta)
xcrun simctl io booted screenshot $OUT/05_psalm_119_grid.png

# 06 — Aleph section (tap on first letter)
xcrun simctl io booted screenshot $OUT/06_aleph_section.png

# 07 — Lelouy Nichmat form (119 > Lelouy Nichmat > New reading)
xcrun simctl io booted screenshot $OUT/07_lelouy_form.png

# 08 — Favorites (Favorites tab)
xcrun simctl io booted screenshot $OUT/08_favorites.png

# 09 — Search (Search tab, type "23" or "shir")
xcrun simctl io booted screenshot $OUT/09_search.png

# 10 — Settings (Settings tab, scroll mid)
xcrun simctl io booted screenshot $OUT/10_settings.png
```

---

## B. iPad Pro 13" (couvre 13" iPad)

### 1. Shutdown iPhone + boot iPad

```bash
xcrun simctl shutdown "iPhone 17 Pro Max"
xcrun simctl boot "iPad Pro 13-inch (M5)"
xcrun simctl bootstatus "iPad Pro 13-inch (M5)" -b

# Locale anglais
xcrun simctl spawn booted defaults write -g AppleLanguages -array en-US
xcrun simctl spawn booted defaults write -g AppleLocale -string en_US
xcrun simctl spawn booted launchctl stop com.apple.SpringBoard
```

### 2. Build pour iPad + install

```bash
cd /Users/dadoou/TEHILIM
xcodebuild -project Tehilim.xcodeproj -scheme Tehilim \
    -destination 'platform=iOS Simulator,name=iPad Pro 13-inch (M5)' \
    -configuration Debug \
    -derivedDataPath build/derived \
    build

xcrun simctl install booted \
    build/derived/Build/Products/Debug-iphonesimulator/Tehilim.app

xcrun simctl launch booted com.david.tehilim
```

### 3. Mode paysage pour montrer la lecture parallèle hébreu/anglais

Dans le simulateur : ⌘ → (rotation horloge) pour paysage. Reste en
landscape pour les captures iPad — c'est ce que les utilisateurs veulent
voir sur iPad.

### 4. Captures iPad (5-7 écrans suffisent)

```bash
OUT=/Users/dadoou/TEHILIM/ios/appstore-screenshots-en/ipad-12.9

# 01 — Home iPad (sidebar + content)
xcrun simctl io booted screenshot $OUT/01_home_ipad.png

# 02 — Tehilim 23 parallel reading (Hebrew + English)
xcrun simctl io booted screenshot $OUT/02_psalm_parallel.png

# 03 — 119 AlphaBeta grid iPad (6 cols au lieu de 4)
xcrun simctl io booted screenshot $OUT/03_psalm_119_ipad.png

# 04 — Life Cases sidebar
xcrun simctl io booted screenshot $OUT/04_life_cases_ipad.png

# 05 — Settings iPad
xcrun simctl io booted screenshot $OUT/05_settings_ipad.png
```

---

## C. Vérification des tailles

Apple requiert :
- iPhone 6.7" : 1290×2796 ou 1320×2868
- iPad 13" : 2064×2752

Vérifie après capture :

```bash
cd /Users/dadoou/TEHILIM/ios/appstore-screenshots-en
for f in iphone-6.7/*.png ipad-12.9/*.png; do
    sips -g pixelWidth -g pixelHeight "$f" | tail -2 | tr '\n' ' '
    echo "  $f"
done
```

Si les dimensions ne matchent pas EXACTEMENT (ex. 1320×2868 au lieu de
1290×2796), App Store Connect peut quand même les accepter — il a une
tolérance. Sinon utilise `sips` pour resizer :

```bash
sips -z 2796 1290 iphone-6.7/01_home.png --out iphone-6.7/01_home.png
```

---

## D. Upload sur App Store Connect

1. Sur la page **iOS App 1.10.7 → English (U.S.)** :
2. Section **iPhone Screenshots** → drag-and-drop les 10 PNG du dossier
   `iphone-6.7/`
3. Section **iPad Screenshots** → drag-and-drop les 5 PNG du dossier
   `ipad-12.9/`
4. L'ordre dans App Store Connect = l'ordre d'apparition. Tu peux
   réorganiser par drag-and-drop.
5. **Save** en haut à droite.

Une fois les captures uploadées + tous les champs EN remplis (description,
keywords, etc.), tu peux **Submit for Review** la V1.10.7.
