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
