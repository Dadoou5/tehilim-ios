# Roadmap — Application Tehilim

## V0.9 — Internal Alpha (T0 + 3 semaines)

- Scaffold SwiftUI complet
- Modèles de données + chargement JSON
- Lecture des 150 psaumes (texte hébreu)
- Navigation 5 livres
- Recherche basique (numéro arabe)
- Réglages basiques (thème, taille texte)

## V1.0 — MVP App Store (T0 + 8 semaines)

- Toutes les fonctionnalités F1–F14 du PRD
- Recherche complète (arabe + hébreu + tolérante)
- Tehilim du jour (mensuel + hebdomadaire)
- Cas de la vie (≥ 10 catégories validées)
- Psaume 119 par 22 lettres
- Favoris + reprise de lecture
- Traduction FR (si licence validée, sinon placeholder)
- Accessibilité complète, Dark Mode
- Tests unitaires + UI
- Validation humaine du contenu (rabbinique + éditoriale)

## V1.1 — Phonétique + polish (T0 + 11 semaines)

- ✅ **Mode phonétique sépharade** (réglages → Mode du texte). Voir [`V1.1_PHONETIC.md`](../V1.1_PHONETIC.md).
- Améliorations recherche (suggestions, fautes de frappe)
- Animations de page
- Onboarding court (3 écrans)
- Performance / froid

## V1.2 — Rappel quotidien (T0 + 12 semaines)

- ✅ **Notification locale quotidienne** paramétrable, deep link vers l'onglet Aujourd'hui. Voir [`V1.2_NOTIFICATIONS.md`](../V1.2_NOTIFICATIONS.md).

## V1.4 — Quick wins

- ✅ Tikkoun HaKlali (Rabbi Nachman, 10 Tehilim).
- ✅ Partage de verset (image card 1080×1080 + ShareLink iOS).
- ✅ Onboarding 3 écrans premier lancement.
- ✅ Dédicaces personnelles (Iluy Nishmat configurable).

## V1.5 — Widget "Tehilim du jour"

- ✅ Extension WidgetKit (3 tailles : small/medium/large).
- ✅ Refresh quotidien à minuit.
- ✅ Tap → deep link `tehilim://daily` → onglet Aujourd'hui.

## V1.6 — App Group : préférences partagées

- ✅ Le widget respecte le mode utilisateur (mensuel ou hebdomadaire) via `UserDefaults` partagé `group.com.david.tehilim`.
- ✅ Reload automatique du widget quand l'utilisateur change de mode dans Réglages ou Aujourd'hui.
- ⚠ Déploiement device : nécessite Apple Developer Program (99 €/an) pour l'enregistrement de l'App Group.

## V1.7 — Multilingue (anglais)

- ✅ Traduction JPS 1917 ajoutée (Sefaria, domaine public). 2527 versets.
- ✅ Picker langue de traduction FR/EN dans Réglages.
- ✅ Localisation UI partielle (tab bar, sections, boutons, onboarding) via Localizable.strings en.lproj.
- ⚠ V1.7.1 : finir la localisation (cas de la vie, pages Privacy/Sources/Accessibilité), sélecteur manuel de langue UI.

## V2.0 — Expansion (T0 + 4 mois)

- iPad universel
- Mode "coutume configurable" (F15)
- Historique de lecture (F16)
- Synchronisation iCloud des favoris et réglages
- Partage de versets (image card)

## V3.0 — Approfondissement (T0 + 8 mois)

- Audio (récitation)
- Traduction multi-langue
- Commentaires courts (Rashi en option)
- Apple Watch companion (Tehilim du jour rapide)
- Widgets (verset du jour)

## Indicateurs de Go / No-Go par phase

- **V1.0 Go** : licence traduction OU décision de partir hébreu seul + corpus relu humainement.
- **V2.0 Go** : ≥ 5 000 utilisateurs actifs + retours réels.
- **V3.0 Go** : équipe ≥ 2 personnes ou contributions externes.
