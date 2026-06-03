#!/bin/sh

# Xcode Cloud post-clone : régénère Tehilim.xcodeproj depuis project.yml
# (le .xcodeproj est gitignoré, on le construit à la demande via XcodeGen).

set -e

echo "→ Installation de XcodeGen via Homebrew"
brew install xcodegen

echo "→ Génération de Tehilim.xcodeproj"
cd "$CI_PRIMARY_REPOSITORY_PATH"
xcodegen generate

echo "✓ Projet généré"
ls -la Tehilim.xcodeproj

# Xcode Cloud DÉSACTIVE la résolution SPM automatique pendant l'archive et exige
# un Package.resolved. Le .xcodeproj étant généré (gitignoré), on injecte le
# Package.resolved épinglé (commité dans ci_scripts/) APRÈS xcodegen — plus
# fiable qu'une résolution réseau live (qui échouait, code 74).
echo "→ Injection du Package.resolved (Supabase + dépendances)"
mkdir -p Tehilim.xcodeproj/project.xcworkspace/xcshareddata/swiftpm
cp ci_scripts/Package.resolved \
   Tehilim.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved

# Rafraîchissement best-effort (non bloquant si le réseau/registre hoquette).
xcodebuild -resolvePackageDependencies -project Tehilim.xcodeproj -scheme Tehilim || true

echo "✓ Package.resolved en place"
ls -la Tehilim.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/ 2>/dev/null || true
