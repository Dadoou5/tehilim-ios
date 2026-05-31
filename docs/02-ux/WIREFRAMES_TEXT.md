# Wireframes textuels

Convention : `[ ]` = bouton/tap, `( )` = composant container, `<<>>` = navigation, `…` = scrollable.

## Écran 1 — Accueil

```
┌────────────────────────────────────────┐
│  Tehilim                          [🔍] │
│                                        │
│  ( Recherche Tehilim, ex. 23 ou כג  )  │
│                                        │
│  ▶ Reprendre la lecture                │
│  ┌───────────────────────────────────┐ │
│  │ Tehilim 67 · livre 2 · v. 4       │ │
│  └───────────────────────────────────┘ │
│                                        │
│  ▶ Tehilim du jour                     │
│  ┌───────────────────────────────────┐ │
│  │ Cycle mensuel — jour 7            │ │
│  │ 35 · 36 · 37 · 38                 │ │
│  └───────────────────────────────────┘ │
│                                        │
│  ▶ Explorer                            │
│  ┌────────────┐  ┌────────────┐       │
│  │ 5 livres   │  │ Cas de la vie     │
│  └────────────┘  └────────────┘       │
│  ┌────────────┐  ┌────────────┐       │
│  │ Psaume 119 │  │ Tous (1–150)      │
│  └────────────┘  └────────────┘       │
└────────────────────────────────────────┘
[Tab: Accueil] [Tehilim] [Du jour] [Cas] [Réglages]
```

## Écran 2 — Liste 5 livres

```
┌────────────────────────────────────────┐
│  ← Tehilim                             │
│                                        │
│  Livres        |  Tous  |  Favoris    │
│  ─────────────────────────────────────│
│  [ Livre 1 · 1–41        ] >          │
│  [ Livre 2 · 42–72       ] >          │
│  [ Livre 3 · 73–89       ] >          │
│  [ Livre 4 · 90–106      ] >          │
│  [ Livre 5 · 107–150     ] >          │
└────────────────────────────────────────┘
```

## Écran 3 — Liste psaumes d'un livre

```
┌────────────────────────────────────────┐
│  ← Livre 1                             │
│  ( Recherche dans le livre )           │
│  ─────────────────────────────────────│
│  1   א   Tehilim 1                  ♡  │
│  2   ב   Tehilim 2                  ─  │
│  3   ג   Tehilim 3                  ─  │
│  ...                                   │
│  41  מא  Tehilim 41                 ─  │
└────────────────────────────────────────┘
```

## Écran 4 — Détail d'un psaume

```
┌────────────────────────────────────────┐
│  ← 23      [♡] [Aa] [FR ⏻] [⋯]         │
│  Tehilim 23 · כג · Livre 1             │
│  ─────────────────────────────────────│
│                                        │
│   1.   מִזְמוֹר לְדָוִד יְהוָה רֹעִי …  │
│        Le Seigneur est mon berger…     │
│                                        │
│   2.   בִּנְאוֹת דֶּשֶׁא יַרְבִּיצֵנִי …│
│        Il me fait reposer…             │
│                                        │
│   …                                    │
│                                        │
│  [< Tehilim 22]      [Tehilim 24 >]    │
└────────────────────────────────────────┘
```

Toolbar :
- ♡ Favori (rempli si actif)
- Aa Taille texte (sheet)
- FR ⏻ Toggle traduction FR (local)
- ⋯ Plus (partager V2, copier référence)

## Écran 5 — Recherche

```
┌────────────────────────────────────────┐
│  [Annuler]  [ saisie : 23           ]  │
│  ─────────────────────────────────────│
│  Résultat                              │
│  ┌───────────────────────────────────┐ │
│  │ Tehilim 23 · כג · Livre 1         │ │
│  └───────────────────────────────────┘ │
│                                        │
│  Suggestions                           │
│  · Tehilim 27                          │
│  · Tehilim 91                          │
│  · Tehilim 121                         │
│                                        │
│  Récents                               │
│  · Tehilim 67                          │
│  · Tehilim 23                          │
└────────────────────────────────────────┘
```

État vide : pas de saisie → "Suggestions" + "Récents".
État erreur : `151` → "Aucun Tehilim trouvé pour 151. Plage valide : 1–150."

## Écran 6 — Tehilim du jour

```
┌────────────────────────────────────────┐
│  Aujourd'hui                  [Mode ▾] │
│  Cycle mensuel — jour 7                │
│  ─────────────────────────────────────│
│   35   לה   Tehilim 35              >  │
│   36   לו   Tehilim 36              >  │
│   37   לז   Tehilim 37              >  │
│   38   לח   Tehilim 38              >  │
│                                        │
│  [ Lire à la suite ]                   │
└────────────────────────────────────────┘
```

Sheet "Mode" :
```
( • ) Cycle mensuel
( ○ ) Jour de la semaine
( ○ ) Personnalisé   [V2]
[Annuler]   [Valider]
```

## Écran 7 — Cas de la vie (liste)

```
┌────────────────────────────────────────┐
│  Cas de la vie                         │
│  ─────────────────────────────────────│
│  ❤️  Guérison                          │
│  🛡  Protection                        │
│  ✈  Voyage                             │
│  💼  Parnassa                          │
│  🤰  Femme enceinte                    │
│  ⚖  Avant un procès                    │
│  🙏  Repentance                         │
│  🌅  Remerciement                       │
│  …                                     │
└────────────────────────────────────────┘
```

## Écran 8 — Cas de la vie (détail)

```
┌────────────────────────────────────────┐
│  ← Guérison                            │
│  ─────────────────────────────────────│
│  Selon la tradition, on a coutume      │
│  de réciter les Tehilim suivants       │
│  pour la guérison d'un proche.         │
│                                        │
│  20 · כ                              > │
│  30 · ל                              > │
│  41 · מא                             > │
│  ...                                   │
└────────────────────────────────────────┘
```

## Écran 9 — Psaume 119 (liste lettres)

```
┌────────────────────────────────────────┐
│  ← Tehilim 119                         │
│  ─────────────────────────────────────│
│  ┌────┐┌────┐┌────┐┌────┐              │
│  │ א  ││ ב  ││ ג  ││ ד  │              │
│  └────┘└────┘└────┘└────┘              │
│  ┌────┐┌────┐┌────┐┌────┐              │
│  │ ה  ││ ו  ││ ז  ││ ח  │              │
│  └────┘└────┘└────┘└────┘              │
│   ...                                  │
│  ┌────┐┌────┐                          │
│  │ ש  ││ ת  │                          │
│  └────┘└────┘                          │
└────────────────────────────────────────┘
```

## Écran 10 — Psaume 119 (section)

```
┌────────────────────────────────────────┐
│  ← ל Lamed (versets 89–96)             │
│  ─────────────────────────────────────│
│   89.  לעולם ה' דברך נצב בשמים…        │
│   90.  לדור ודור אמונתך…               │
│   ...                                  │
│  [< כ Kaf]              [מ Mem >]      │
└────────────────────────────────────────┘
```

## Écran 11 — Réglages

```
┌────────────────────────────────────────┐
│  Réglages                              │
│  ─────────────────────────────────────│
│  AFFICHAGE                             │
│  Taille de texte             [Aa+]     │
│  Thème                       [Système]│
│  Traduction française        [ ⏻ ]    │
│  Numérotation versets        [Hébreu ▾]│
│                                        │
│  LECTURE QUOTIDIENNE                   │
│  Mode                        [Mensuel ▾]│
│                                        │
│  À PROPOS                              │
│  Sources du contenu          >         │
│  Confidentialité             >         │
│  Version                     1.0.0     │
└────────────────────────────────────────┘
```
