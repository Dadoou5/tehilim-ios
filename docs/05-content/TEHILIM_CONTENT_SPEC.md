# Spécification du contenu Tehilim

> Auteur : Agent Expert en Tehilim
> Statut : V0.1 — schéma stable, contenu à valider humainement

---

## 1. Corpus

### 1.1 Les 5 livres

Découpage classique :

| Livre | Psaumes |
|-------|---------|
| 1 (Sefer Rishon) | 1 – 41 |
| 2 (Sefer Sheni) | 42 – 72 |
| 3 (Sefer Shlishi) | 73 – 89 |
| 4 (Sefer Revi'i) | 90 – 106 |
| 5 (Sefer Hamishi) | 107 – 150 |

Chaque livre se termine traditionnellement par une doxologie (« Baroukh… »). Le découpage est massorétique standard.

### 1.2 Source du texte hébreu

**Validation humaine requise** : choix d'une source unique, intégrale, et vérifiable. Candidats :
- Mechon Mamre (texte massorétique avec teamim).
- Sefaria (TaNaKh standard).
- Texte d'un édition imprimée scannée + relecture.

Décision à prendre AVANT le sprint de chargement de contenu.

### 1.3 Traduction française

**Validation humaine requise** : aucune traduction n'est intégrée sans clarté de licence.
- Option A : traduction libre de droits (ex. Rabbinat 1899, Crampon 1923 — domaine public mais style daté).
- Option B : traduction commissionnée / autorisée.
- Option C : pas de traduction en V1 — ne pas casser le produit pour autant.

Le schéma reste prêt à recevoir une traduction par verset.

## 2. Numérotation hébraïque

Conversion arabe ↔ hébreu (gematria standard, sans interpoler les ortographes problématiques 15/16) :

```
1 → א     11 → יא    21 → כא    50 → נ      90 → צ
2 → ב     12 → יב    22 → כב    60 → ס     100 → ק
3 → ג     13 → יג    23 → כג    70 → ע     119 → קיט
4 → ד     14 → יד    24 → כד    80 → פ     150 → קנ
5 → ה     15 → טו (! pas יה)  
6 → ו     16 → טז (! pas יו)
7 → ז     17 → יז
8 → ח     18 → יח
9 → ט     19 → יט
10 → י    20 → כ
```

⚠ **Règle critique** : 15 = טו, 16 = טז (pas יה / יו pour respect du nom divin). Cette règle est implémentée dans `HebrewNumerals.swift`.

## 3. Psaume 119

22 sections, une par lettre de l'alphabet hébreu. Chaque section = 8 versets (alphabétisme initial : tous les versets d'une section commencent par la même lettre).

| # | Lettre | Versets |
|---|--------|---------|
| 1 | א Aleph | 1–8 |
| 2 | ב Bet | 9–16 |
| 3 | ג Gimel | 17–24 |
| 4 | ד Dalet | 25–32 |
| 5 | ה Hé | 33–40 |
| 6 | ו Vav | 41–48 |
| 7 | ז Zayin | 49–56 |
| 8 | ח Het | 57–64 |
| 9 | ט Tet | 65–72 |
| 10 | י Yod | 73–80 |
| 11 | כ Kaf | 81–88 |
| 12 | ל Lamed | 89–96 |
| 13 | מ Mem | 97–104 |
| 14 | נ Noun | 105–112 |
| 15 | ס Samech | 113–120 |
| 16 | ע Ayin | 121–128 |
| 17 | פ Pé | 129–136 |
| 18 | צ Tsadé | 137–144 |
| 19 | ק Qof | 145–152 |
| 20 | ר Resh | 153–160 |
| 21 | ש Shin | 161–168 |
| 22 | ת Tav | 169–176 |

## 4. Tehilim du jour — règles

### 4.1 Mode mensuel (cycle hébraïque)

Distribution classique des 150 Tehilim sur les jours du mois (1 à 29 ou 30). Le découpage de référence le plus courant :

| Jour | Psaumes |
|------|---------|
| 1 | 1–9 |
| 2 | 10–17 |
| 3 | 18–22 |
| 4 | 23–28 |
| 5 | 29–34 |
| ... | ... |
| 29 | 140–150 |

**Validation humaine requise** : confirmer la version du découpage (Roudnik / Tehilim standard / autre).
Le fichier `daily_reading_rules.json` contient un découpage candidate ; la version finale doit être relue.

### 4.2 Mode jour de la semaine

Distribution des 150 sur 7 jours :

| Jour | Psaumes (proposition) |
|------|----------------------|
| Dimanche | 1–29 |
| Lundi | 30–50 |
| Mardi | 51–72 |
| Mercredi | 73–89 |
| Jeudi | 90–106 |
| Vendredi | 107–119 |
| Chabbat | 120–150 |

**Validation humaine requise**.

### 4.3 Mode coutume configurable (V2)

L'utilisateur peut définir manuellement, par jour, la liste de psaumes. Persistance locale.

## 5. Cas de la vie — catégorisation

Liste minimale (≥ 10) :

| Catégorie | Slug | Notes |
|-----------|------|-------|
| Avant un procès | `before_trial` | Tradition |
| Femme enceinte | `pregnancy` | Tradition |
| Guérison / malade | `healing` | Pas de promesse médicale |
| Protection | `protection` | Voyage compris ? non — séparé |
| Voyage | `travel` | Tefilat haDerekh n'est PAS dans l'app |
| Parnassa (subsistance) | `parnassa` | |
| Inquiétude / angoisse | `anxiety` | Pas de promesse psychologique |
| Remerciement | `gratitude` | |
| Repentance / techouva | `teshuva` | |
| Deuil | `mourning` | Sensibilité élevée |

Catégories additionnelles candidates (V1 ou V2) :
- Avant un examen / étude
- Mariage / shidouch
- Naissance
- Anniversaire de décès (yahrzeit)

Chaque catégorie contient :
- Un titre.
- Une description courte (≤ 200 caractères).
- Une liste ordonnée de psaumes recommandés (références numériques).
- **Validation humaine requise** sur chaque liste.

## 6. Hypothèses éditoriales

- Le texte hébreu seul a une valeur produit complète.
- La traduction FR est un confort, pas un bloquant.
- Le marquage des paroles divines (Adonaï) suit le standard du texte source.
- Aucune translittération n'est ajoutée en V1.
- Les téamim peuvent être conservés mais désactivables (V2).
- Le code "psaume" est l'identifiant principal ; la lettre hébraïque est dérivée.

## 7. Points nécessitant validation humaine

- [ ] Source de référence du texte hébreu.
- [ ] Source de la traduction française et licence.
- [ ] Découpage exact "Tehilim du mois".
- [ ] Découpage exact "Tehilim de la semaine".
- [ ] Liste finale des cas de la vie + psaumes associés.
- [ ] Validation rabbinique des catégories sensibles (deuil, maladie, procès).
- [ ] Présence ou non des téamim et nikoud.
- [ ] Notation des sections du psaume 119 (ש vs שׁ vs שׂ).
