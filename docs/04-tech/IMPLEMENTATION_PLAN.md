# Plan d'Implémentation

## Prérequis

- Xcode 15+
- iOS 17 SDK
- Validation humaine du corpus avant intégration en bundle (cf. `content_validation_notes.md`)

## Sprint 1 (1 semaine) — Fondations

**Objectif** : projet compilable, accueil + lecture d'un psaume mock.

- [ ] Bootstrapping Xcode (target + bundle id `com.tehilim.app`)
- [ ] Structure de dossiers `App / Core / Features / Shared / Resources`
- [ ] Tokens design (Colors, Typography) en SwiftUI
- [ ] Modèles `Psalm`, `Verse`
- [ ] `ContentLoader` minimal sur 5 psaumes d'exemple
- [ ] `RootTabView` (5 onglets, contenus mock)
- [ ] `HomeView` simplifiée (carte 5 livres, carte recherche)
- [ ] `PsalmDetailView` minimal (versets hébreu)
- [ ] Mode sombre / clair fonctionnel
- [ ] Tests unitaires : `ContentLoaderTests`, `HebrewNumeralsTests`

Sortie : APK interne installable.

## Sprint 2 (1 semaine) — Recherche, navigation complète

**Objectif** : recherche fonctionnelle, navigation 5 livres + détail.

- [ ] `HebrewNumerals.toInt / toHebrew` complet, tests 1..400
- [ ] `SearchInterpreter` complet
- [ ] `SearchView` (.searchable + suggestions + récents)
- [ ] `BookListView`, `PsalmListView` complète (1..150 + headers livres)
- [ ] Navigation prev/next dans `PsalmDetailView`
- [ ] Toggle traduction FR (local + global)
- [ ] `Preferences` (UserDefaults via @AppStorage)
- [ ] Reprise de lecture (`LastReadStore`)
- [ ] Tests unitaires : `SearchInterpreterTests`

## Sprint 3 (1 semaine) — Tehilim du jour, Cas de la vie, Psaume 119

**Objectif** : toutes features V1 en place avec données semi-réelles.

- [ ] `DailyEngine` (mensuel + hebdomadaire)
- [ ] `DailyView` + sheet de mode
- [ ] `LifeCasesListView`, `LifeCaseDetailView`
- [ ] `Psalm119HomeView` (22 tiles), `Psalm119SectionView`
- [ ] `FavoritesStore` + UI favoris
- [ ] `SettingsView` complet (taille, thème, traduction, mode quotidien, numérotation)
- [ ] Tests unitaires : `DailyEngineTests`

## Sprint 4 (1 semaine) — Contenu réel, accessibilité, polish

**Objectif** : intégration corpus validé + a11y + perf.

- [ ] Intégration `psalms.json` validé humainement
- [ ] Intégration `life_cases.json` validé
- [ ] Intégration `daily_reading_rules.json` validé
- [ ] `psalm_119_sections.json` validé
- [ ] Accessibilité : labels VoiceOver complets
- [ ] Test à AX5 sur tous les écrans
- [ ] Profiling Instruments (mémoire, CPU)
- [ ] Empty / Loading / Error states sur tous les écrans
- [ ] Localizable.strings complet FR + HE

## Sprint 5 (3 jours) — QA + Release

- [ ] QA manuelle complète (`QA_CHECKLIST.md`)
- [ ] Tests UI XCUITest sur parcours critiques
- [ ] App Icon + LaunchScreen
- [ ] App Store Connect : page + screenshots + politique de confidentialité
- [ ] Build TestFlight interne
- [ ] Soumission App Store

## Plan de sprint condensé (3 itérations focalisées demandées par l'énoncé)

### Itération 1 — "Lire un Tehilim"
- Scaffold + lecture des 150 psaumes (texte hébreu)
- Liste 5 livres + détail
- Préférences de base (thème + taille texte)
- Reprise de lecture

### Itération 2 — "Trouver un Tehilim"
- Recherche complète (arabe + hébreu + tolérante)
- Tehilim du jour (mensuel + hebdo)
- Favoris

### Itération 3 — "Vivre les Tehilim"
- Cas de la vie
- Psaume 119 par lettres
- Traduction FR (placeholder ou réel selon licence)
- Accessibilité finale + polish

## Risques techniques

| Risque | Mitigation |
|--------|------------|
| Calendrier hébraïque iOS incomplet | Encapsuler dans `DailyEngine`, tests sur dates clés |
| Polices hébraïques licence | Fallback système, validation très tôt |
| Performance LazyVStack sur Tehilim 119 (176 versets) | `LazyVStack` + paging par section |
| RTL mixte FR/HE | Bloc isolé par langue |
