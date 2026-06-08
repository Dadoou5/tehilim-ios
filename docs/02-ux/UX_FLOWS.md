# UX Flows

## Flow 1 — Lire le Tehilim du jour

```
[Accueil]
   │ tap "Tehilim du jour"
   ▼
[Liste du jour] (mode actif visible)
   │ tap psaume #
   ▼
[Détail psaume]
   │ swipe / tap "suivant"
   ▼
[Détail psaume suivant]
```

États clés :
- Mode actif visible en haut ("Cycle mensuel — jour 7").
- Bouton "Changer de mode" → sheet.

## Flow 2 — Recherche par numéro

```
[N'importe où avec barre de recherche]
   │ focus → clavier
   │ saisit "23" OU "כג" OU "tehilim 23"
   ▼
[Résultat unique : Tehilim 23] → tap
   ▼
[Détail Tehilim 23]
```

Variantes :
- Saisie ambiguë → résultats multiples.
- Hors plage → état "Aucun résultat" + suggestions (Tehilim populaires, du jour).
- Saisie vide → suggestions par défaut (récents + favoris + du jour).

## Flow 3 — Cas de la vie

```
[Onglet Cas de la vie]
   │ tap "Guérison"
   ▼
[Détail catégorie]
    - Note éditoriale courte (sans promesse)
    - Liste ordonnée de psaumes
   │ tap psaume #
   ▼
[Détail du psaume avec breadcrumb "Guérison › Tehilim N"]
```

## Flow 4 — Psaume 119 par lettres

```
[Accueil]
   │ tap carte "Psaume 119"
   ▼
[Liste 22 lettres] (grille ou liste)
   │ tap "ל Lamed"
   ▼
[Section Lamed (versets 89–96)]
   │ swipe gauche → section Mem
   │ swipe droit → section Kaf
```

## Flow 5 — Activer la traduction française

```
[Détail psaume]
   │ tap toggle FR (en haut)
   ▼
[Détail psaume avec traduction sous chaque verset]
   │ (préférence sauvegardée)
```

OU :
```
[Réglages]
   │ active "Traduction française par défaut"
   ▼ (tous les écrans psaume hériter du toggle)
```

## Flow 6 — Reprendre la lecture

```
[Lance app]
   ▼
[Accueil]
    - Carte "Reprendre Tehilim 67"
   │ tap
   ▼
[Détail Tehilim 67 — défilement à la position mémorisée]
```

## Flow 7 — Marquer un favori

```
[Détail psaume]
   │ tap icône cœur (toolbar)
   ▼
[Confirmation visuelle légère (haptique + couleur)]
   │
[Onglet Tehilim → Favoris affiche le psaume]
```

## Flow 8 — Changer la taille de texte

```
[Réglages]
   │ tap "Taille de texte"
   ▼
[Sheet avec slider + aperçu live d'un verset]
   │ ajuste
   ▼
[Validation auto à la fermeture]
```

## Flow 9 — Premier lancement

```
[Splash bref (logo + nom)]
   ▼
[Accueil] (pas d'onboarding bloquant en V1)
    - Tooltip optionnel sur la barre de recherche (1×)
```

## Principes UX retenus

- Pas de modal qui interrompt la lecture.
- Pas plus de 2 niveaux entre l'accueil et un verset.
- Geste retour iOS standard partout.
- Toolbar minimale dans le détail (favori + traduction + partage V2).
- Pas de pop-up de demande de notes / d'avis → respect du lecteur.

## Flow — Chaîne de Tehilim (V1.14)

```
[Créer une chaîne] (intention + durées)
   │ créer
   ▼
[Détail chaîne] ──partage──> lien WhatsApp / QR code
   │
   ├─ (autre appareil) ouvre le lien ─> [Détail] ─ "Rejoindre" (prénom) ─> participant
   │      │ tap numéro libre ─> verrouillé à mon nom (temps réel)
   │
   ├─ (maître) "Prolonger la sélection" (durée ≤ 48 h)
   │           "M'attribuer les restants" / "Clôturer & distribuer"
   ▼
[Distribuée] ─ notifications push aux participants
   │ tap un de mes Tehilim ─> [Détail psaume] (lecture)
   │ (hors-ligne) Mes chaînes ─> lecteur hors-ligne ─> lecture
   ▼
[Fin de lecture +7 j] ─ suppression serveur automatique
```

Catégorisation « Mes chaînes » : Sélection en cours → (distribution OU échéance)
→ Lecture en cours → (échéance de lecture) → Terminées (bascule automatique).
