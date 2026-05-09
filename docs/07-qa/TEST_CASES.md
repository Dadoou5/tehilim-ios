# Test Cases

Format : `TC-XXX | Titre | Préconditions | Étapes | Résultat attendu`.

## Lecture

**TC-001** Lire Tehilim 1 depuis l'accueil.
- Pré : 1ère ouverture.
- Étapes : Accueil → "Tous (1–150)" → tap 1.
- Attendu : `PsalmDetailView` affichée, texte hébreu visible.

**TC-002** Naviguer prev / next.
- Pré : Tehilim 23 ouvert.
- Étapes : tap "23 →" puis "← 23".
- Attendu : Tehilim 24 puis Tehilim 23.

**TC-003** Borne supérieure.
- Pré : Tehilim 150 ouvert.
- Attendu : bouton "suivant" désactivé.

**TC-004** Favori persistant.
- Étapes : Tehilim 23 → tap ♡ → forcer fermeture app → relancer → onglet Tehilim → Favoris.
- Attendu : Tehilim 23 listé.

## Recherche

**TC-010** Numéro arabe.
- Étapes : Recherche → `23`.
- Attendu : 1 résultat exact "Tehilim 23".

**TC-011** Lettres hébraïques.
- Étapes : Recherche → `כג`.
- Attendu : 1 résultat exact "Tehilim 23".

**TC-012** Saisie mixte.
- Étapes : Recherche → `tehilim 23`.
- Attendu : 1 résultat "Tehilim 23".

**TC-013** Recherche FR.
- Étapes : `psaume 23`.
- Attendu : 1 résultat.

**TC-014** Hors plage.
- Étapes : `0` puis `151`.
- Attendu : "Aucun résultat" + plage rappelée.

**TC-015** Saisie dégradée.
- Étapes : `abc` puis `🤖`.
- Attendu : pas de crash, suggestions affichées.

**TC-016** 15 et 16 (cas spéciaux).
- Étapes : `טו` → 15 ; `טז` → 16.
- Attendu : Tehilim 15 et 16 respectivement.

## Traduction FR

**TC-020** Toggle local.
- Étapes : Tehilim 23 → tap "FR" → vérifier.
- Attendu : traduction visible (ou message si absente).

**TC-021** Toggle global.
- Étapes : Réglages → activer FR → ouvrir n'importe quel psaume.
- Attendu : FR affichée par défaut.

**TC-022** Persistance.
- Étapes : activer global → relancer.
- Attendu : pref conservée.

## Tehilim du jour

**TC-030** Mensuel un jour donné.
- Étapes : forcer date hébraïque jour 7 → Daily.
- Attendu : liste correspondante au jour 7 du JSON.

**TC-031** Hebdomadaire mardi.
- Étapes : Settings → Hebdomadaire → forcer mardi.
- Attendu : liste mardi.

**TC-032** Changement de mode immédiat.
- Attendu : pas de relance nécessaire, ≤ 200 ms.

## Cas de la vie

**TC-040** Liste catégories.
- Attendu : 10 catégories minimum, ordonnées.

**TC-041** Détail "Guérison".
- Attendu : note explicative + 10 psaumes.

**TC-042** Aucune promesse.
- Vérification manuelle texte de la note pour chaque catégorie.

## Psaume 119

**TC-050** Liste lettres.
- Attendu : 22 cellules de א à ת.

**TC-051** Section Lamed.
- Attendu : versets 89..96 affichés.

**TC-052** Bornes navigation.
- Attendu : Aleph sans "lettre précédente", Tav sans "suivante".

## Réglages

**TC-060** Tailles.
- Étapes : passer S → XL.
- Attendu : aperçu live + écran de psaume reflète.

**TC-061** Thème.
- Étapes : Clair → Sombre → Système.
- Attendu : changement immédiat.

**TC-062** Numérotation versets.
- Étapes : Hébreu → Arabe.
- Attendu : numéros versets passent à 1, 2, 3 etc.

## Accessibilité

**TC-070** VoiceOver Tehilim 23.
- Attendu : "Tehilim 23, livre 1, verset 1, [hébreu]".

**TC-071** Dynamic Type AX5.
- Attendu : aucun élément coupé.

## Offline

**TC-080** Mode avion.
- Attendu : tous les parcours fonctionnent.

## Performance

**TC-090** Cold start.
- Attendu : < 1.5 s sur iPhone 13.

**TC-091** Tehilim 119 fluide.
- Attendu : scroll 60 fps continu.
