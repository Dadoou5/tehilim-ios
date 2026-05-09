# Risks & Edge Cases

## Risques produit

| ID | Risque | Probabilité | Impact | Mitigation |
|----|--------|-------------|--------|------------|
| R1 | Erreur dans le texte hébreu | Moyenne | Élevé | Validation humaine, hash du fichier de release |
| R2 | Catégorie "cas de la vie" mal interprétée | Moyenne | Élevé | Notes prudentes, validation rabbinique |
| R3 | Découpage "Tehilim du jour" non orthodoxe | Élevée | Moyen | Mode explicite, mode personnalisé V2 |
| R4 | Licence traduction non claire | Élevée | Bloquant | Pas de traduction sans contrat |
| R5 | Mauvaise réception App Store (sensibilité religieuse) | Faible | Élevé | Description neutre |

## Risques techniques

| ID | Risque | Probabilité | Impact | Mitigation |
|----|--------|-------------|--------|------------|
| T1 | RTL mixte FR/HE casse layout | Moyenne | Moyen | Bloc isolé par direction |
| T2 | `Calendar(.hebrew)` jours différents selon locale | Moyenne | Moyen | Tests dédiés sur dates clés |
| T3 | Tehilim 119 (176 versets) lent | Faible | Moyen | LazyVStack + paging par section |
| T4 | Police hébraïque indisponible / licence | Moyenne | Moyen | Fallback système |
| T5 | Dynamic Type AX5 casse layout | Moyenne | Moyen | Tests dédiés, layout flexible |

## Edge cases lecture

- Verset très long (Tehilim 119 versets longs).
- Verset avec ponctuation cantillaire complexe.
- Psaume 117 (deux versets seulement) → vérifier layout.
- Psaume 119 (176 versets) → vérifier perf, ouverture par section.
- Psaume 88 contenu sombre → s'assurer que la liste "Cas de la vie - Guérison" est cohérente.
- Tehilim avec en-tête (`לדוד`, `שיר המעלות`) → afficher comme métadonnée séparée.

## Edge cases recherche

- Saisie avec espaces multiples : `tehilim   23`.
- Saisie avec ponctuation : `23?`.
- Saisie avec parasites RTL/LTR (caractères de contrôle invisibles).
- Hébreu avec gershayim : `כ"ג` → doit être interprété comme 23.
- Tetragramme dans saisie : ne PAS proposer comme requête.
- Emojis : ignorés gracieusement.
- Saisie partielle `2` → suggérer 2, 12, 20–29, 121, 122 etc.

## Edge cases Tehilim du jour

- Mois hébraïque de 29 jours : jour 30 inexistant → règle de fallback (combine 29).
- Bissextile (Adar I / Adar II) : règle simple, ne change pas le découpage des Tehilim.
- Chabbat : conserve le mode (pas de spécificité V1).
- Roch Hodech : pas de spécificité V1.
- 9 Av / Kippour : pas de spécificité V1.

## Edge cases UI

- Très grande Dynamic Type sur grille 22 lettres → fallback en liste verticale.
- iPhone SE (petit écran) → 2 colonnes max sur grille.
- Mode sombre + AX5 + traduction ON → écran de psaume long, vérifier que toolbar reste lisible.
- Switch Control utilisateur → tous les éléments interactifs accessibles.
- Locale Hebrew system → l'app reste utilisable (UI déjà bilingue).

## Edge cases données

- JSON corrompu → `ErrorBanner` + bouton "Réessayer" + log.
- JSON manquant → fallback minimal embarqué (Tehilim 1 et 23 toujours dispo).
- Fichier de traduction absent → mode hébreu seul, toggle FR désactivé en grisé.
- Catégorie "Cas de la vie" sans psaumes → cellule masquée + log.

## Edge cases plateforme

- Lancement sans connexion → pas de problème (offline-first).
- Mémoire faible : `Tehilim 119` ne doit pas charger 176 versets en mémoire d'un coup → LazyVStack.
- Backgrounding pendant lecture → état restauré sans perte.
- Mise à jour iOS majeure : tester sur la nouvelle bêta avant chaque sortie iOS.
