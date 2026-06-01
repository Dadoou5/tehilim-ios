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

# Résolution des packages SPM (Firebase) AVANT l'étape d'archive : Xcode Cloud
# désactive la résolution auto pendant l'archive et exige un Package.resolved.
# Comme le .xcodeproj est généré (gitignoré), on le produit ici.
echo "→ Résolution des dépendances SPM (Firebase)"
xcodebuild -resolvePackageDependencies \
  -project Tehilim.xcodeproj \
  -scheme Tehilim
echo "✓ Package.resolved généré"
ls -la Tehilim.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/ 2>/dev/null || true
