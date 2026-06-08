# Information Architecture

```
Tehilim
├── Accueil
│   ├── Tehilim du jour (raccourci)
│   ├── Reprendre la lecture
│   ├── 5 livres (raccourci)
│   ├── Cas de la vie (raccourci)
│   ├── Psaume 119 (raccourci)
│   └── Recherche (raccourci)
│
├── Tehilim
│   ├── Liste des 5 livres
│   │   └── Liste des psaumes du livre
│   │       └── Détail du psaume
│   ├── Liste complète 1–150
│   │   └── Détail du psaume
│   └── Favoris
│       └── Détail du psaume
│
├── Tehilim du jour
│   ├── Liste des psaumes du jour (selon mode)
│   │   └── Détail du psaume
│   └── Sélecteur de mode (mensuel / hebdomadaire)
│
├── Cas de la vie
│   ├── Liste des catégories
│   │   └── Détail catégorie (note + psaumes)
│   │       └── Détail du psaume
│
├── Psaume 119
│   ├── Liste des 22 lettres
│   │   └── Détail section
│   │       └── (navigation lettre précédente / suivante)
│
├── Recherche
│   └── Résultats → Détail du psaume
│
└── Réglages
    ├── Affichage
    │   ├── Taille de texte
    │   ├── Thème (Système / Clair / Sombre)
    │   ├── Traduction française (toggle)
    │   └── Numérotation versets (Hébreu / Arabe)
    ├── Lecture quotidienne
    │   └── Mode (Mensuel / Hebdomadaire / [V2 Personnalisé])
    ├── À propos
    │   ├── Sources du contenu
    │   ├── Licences
    │   └── Confidentialité
```

## Navigation principale (TabView)

5 onglets racine :
1. **Accueil** (icône `house`)
2. **Tehilim** (icône `book.closed`) → sous-navigation 5 livres / Liste / Favoris
3. **Aujourd'hui** (icône `sun.max`)
4. **Cas de la vie** (icône `heart.text.square`)
5. **Réglages** (icône `gearshape`)

La recherche est accessible via une barre persistante en haut de l'Accueil et de l'onglet Tehilim, plus un raccourci global.
Le Psaume 119 est accessible via Accueil + via l'onglet Tehilim (carte spéciale en tête).

## Hiérarchie de contenu

- **Niveau 1** : Onglets racine.
- **Niveau 2** : Listes (livres, catégories, lettres, jours).
- **Niveau 3** : Détail d'un psaume.
- **Modal / sheet** : Recherche, sélecteur de mode quotidien, sélecteur de taille texte.

## Addendum V1.14 — Chaîne de Tehilim

```
Cas de la vie / Accueil
└── Chaîne de Tehilim
    ├── Mes chaînes
    │   ├── Sélection en cours   (compte à rebours → fin de sélection)
    │   ├── Lecture en cours      (compte à rebours → fin de lecture)
    │   └── Terminées             (suppression locale ; Lecture aussi)
    ├── Créer une chaîne          (intention, durées) → partage lien/QR
    └── Détail d'une chaîne
        ├── En-tête (intention, compte à rebours, participants, progression)
        ├── Inviter (lien WhatsApp + QR)   [tant que sélection ouverte]
        ├── Grille 1→150 (filtres Tous/Libres/À moi ; sélection temps réel)
        ├── Contrôles maître (éditer · prolonger · attribuer · distribuer · retirer · supprimer)
        └── Lecteur hors-ligne (chaîne distribuée → mes Tehilim)
```

Lecture (transverse) : bouton « Aa » (taille du texte) dans la barre de lecture
d'un Tehilim, des sections du Tehilim 119, et du lecteur de chaîne.
