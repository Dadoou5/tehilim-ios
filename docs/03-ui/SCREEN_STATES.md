# Screen States

Pour chaque écran : Loading / Empty / Error / Success.

## HomeView
- **Loading** : skeleton sur 3 cartes (1 s max).
- **Empty** : pas applicable (toujours du contenu : Tehilim du jour calculé localement).
- **Error** : si JSON corpus illisible → `ErrorBanner` + bouton "Réessayer".
- **Success** : version par défaut.

## PsalmListView
- **Loading** : `ProgressView` centré, 200ms grace.
- **Empty (Favoris)** : `EmptyStateView` "Aucun favori. Tape ♡ sur un psaume."
- **Error** : `ErrorBanner`.
- **Success** : liste.

## PsalmDetailView
- **Loading** : skeleton sur 5 lignes de versets.
- **Empty** : impossible.
- **Error** : "Tehilim introuvable" + bouton retour.
- **Success** : versets affichés.
- **Sub-state — Traduction OFF** : versets hébreux seuls.
- **Sub-state — Traduction ON, indispo** : verset hébreu + ligne grise « Traduction non disponible ».

## SearchView
- **Empty (avant saisie)** : sections "Suggestions" + "Récents".
- **No result** : `EmptyStateView` "Aucun Tehilim trouvé." + suggestions.
- **Error** : non applicable (recherche locale).
- **Loading** : non applicable (latence < 100 ms).
- **Success** : résultat principal + suggestions.

## DailyView
- **Loading** : skeleton 1 s.
- **Empty** : impossible (la règle calcule toujours quelque chose).
- **Error** : si règles corrompues → fallback "1, 2, 3" + `ErrorBanner`.
- **Success** : liste du jour.

## LifeCasesListView
- **Loading** : skeleton.
- **Empty** : si fichier absent → `EmptyStateView` "Catégories en cours de validation".
- **Error** : `ErrorBanner` + retry.
- **Success** : grille.

## LifeCaseDetailView
- **Empty** : impossible si la catégorie existe.
- **Error** : retour automatique.
- **Success** : note + liste.

## Psalm119HomeView
- **Empty** : impossible.
- **Success** : 22 tiles.

## SettingsView
- **Loading** : non applicable.
- **Success** : toujours.

## États transverses

- **Mode hors-ligne** : aucune communication réseau, aucun message d'erreur réseau possible.
- **Première ouverture** : tooltip discret sur la barre de recherche, dismissable.
- **Très grande taille de texte (AX5)** : tous les écrans testés sans clipping.
