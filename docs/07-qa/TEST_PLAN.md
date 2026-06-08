# Test Plan

## Périmètre

- 100 % des fonctionnalités V1 du PRD.
- Devices cibles : iPhone SE (3e gen), iPhone 13/14/15, iPhone 15 Pro Max.
- iOS : 17.x, 18.x, 19.x.

## Stratégie

### 1. Tests unitaires (XCTest)

Couverture cible : ≥ 80 % sur `Core/`.

- `HebrewNumeralsTests` — conversions 1..400, cas spéciaux 15/16.
- `SearchInterpreterTests` — saisies arabes, hébreues, mixtes, sales, hors plage.
- `DailyEngineTests` — mensuel et hebdomadaire, transitions de jours, mois 29 vs 30 jours.
- `ContentLoaderTests` — décodage JSON OK/KO, résilience.
- `PreferencesTests` — défauts, persistance.
- `FavoritesStoreTests` — ajout/retrait, persistance disque.

### 2. Tests UI (XCUITest)

Parcours critiques :
- Parcours 1 : Accueil → Tehilim 23 → ajouter en favori.
- Parcours 2 : Recherche `כג` → ouvrir résultat.
- Parcours 3 : Tehilim du jour → mode mensuel → ouvrir 1er psaume.
- Parcours 4 : Cas de la vie → Guérison → ouvrir Tehilim 20.
- Parcours 5 : Toggle traduction FR.
- Parcours 6 : Psaume 119 → Lamed → Mem (navigation).
- Parcours 7 : Réglages → changer thème → relancer → vérifier persistance.

### 3. Tests d'accessibilité

- VoiceOver : tous les écrans navigables sans piège.
- Dynamic Type AX5 : aucune coupe.
- Contraste : audit Xcode Accessibility Inspector.
- Switch Control : navigation possible.

### 4. Tests de performance

- Cold launch < 1.5 s sur iPhone 12+.
- Décodage JSON corpus < 500 ms.
- Recherche < 100 ms.
- Mémoire < 80 Mo en lecture.
- Profiling Instruments avant chaque release.

### 5. Tests de régression

Avant chaque release :
- Suite XCTest verte.
- Suite XCUITest verte.
- QA manuelle sur la `QA_CHECKLIST.md`.

### 6. Tests offline

Mode avion ON pendant tous les tests fonctionnels — comportement identique attendu.

## Outils

- XCTest, XCUITest
- Xcode Accessibility Inspector
- Instruments (Time Profiler, Allocations, Leaks)
- TestFlight pour beta interne (≥ 5 testeurs avant release)

## Reporting

- Suivi des bugs : Linear (à confirmer) / GitHub Issues.
- Sévérité : Blocker / Critical / Major / Minor / Cosmetic.
- Tout bug sécurité (rare ici) → patch immédiat.
- Tout bug de contenu (texte hébreu erroné) → escalade humaine + patch immédiat.

## Addendum V1.14 — Chaîne de Tehilim (réseau / temps réel / push)

- **Multi-appareils** : 2 simulateurs/devices ouvrant la même chaîne via le lien →
  vérifier join, sélection, verrou (un Tehilim pris ne peut l'être 2×), compteurs
  et compte à rebours qui se propagent en temps réel.
- **Catégories** : distribution → quitte « Sélection en cours » ; échéance de
  lecture atteinte → bascule « Terminées » (recalcul live).
- **Prolongation** : repousse l'échéance, réarme les rappels, re-notifie ;
  l'invitation redevient possible.
- **Notifications push** : seuils 70/80/90/100 %, distribution, suppression,
  rappels 80 %/95 % (vérifier la livraison APNs + FCM ; petite icône correcte).
- **Hors-ligne** : chaîne distribuée ouverte une fois en ligne → lisible en mode
  avion via le lecteur hors-ligne.
- **Dégradation** : build sans config Supabase → la chaîne est absente, le reste
  de l'app fonctionne.
- **Sécurité** : RLS (un user n'écrit que ses données ; double-booking refusé).
- **Confidentialité** : suppression automatique *fin de lecture + 7 j* ; retrait
  local d'une chaîne.

## Addendum V1.14 — Lecture
- Bouton « Aa » : A−/A+ borné (8 paliers), persistant, présent sur Tehilim,
  sections 119 et lecteur de chaîne.
- Numéro du Tehilim visible ; compte à rebours qui défile y compris au retour sur l'écran.
