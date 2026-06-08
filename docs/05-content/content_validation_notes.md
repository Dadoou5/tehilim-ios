# Notes de validation humaine — Contenu

> Ce fichier liste l'état des éléments de contenu qui ont nécessité une validation humaine avant publication App Store. Statut mis à jour au fur et à mesure des décisions du porteur projet.

---

## Statut global

✅ **Validation rabbinique reçue par le porteur projet (2026-05-08)** sur les éléments listés ci-dessous (catégories, listes de Tehilim, règles de lecture quotidienne).

✅ **Autorisation de l'éditeur de la traduction française** : Beth Loubavitch (8 rue Lamartine 75009 Paris — chabad@loubavitch.fr). Traduction et mention obligatoire affichée dans Réglages → Sources.

---

## Bloquants App Store — état

### B1. Source du texte hébreu — ✅ OK
- Source : Sefaria, édition « Miqra according to the Masorah ». Texte massorétique avec nikud, sans téamim. Domaine public.

### B2. Licence de la traduction française — ✅ OK
- Traduction Beth Loubavitch — `le-tehilim.online`. Autorisation expresse reçue. Mention obligatoire affichée dans Réglages → Sources.

### B3. Cas de la vie — ✅ OK
- Catégories validées rabbiniquement (cf. V5 et V5bis ci-dessous).

## Items validés

### V1. Découpage "Tehilim du mois" — ✅ OK
- Validation rabbinique reçue. Découpage en 29 jours du calendrier hébraïque, fallback du jour 30 vers le jour 29 documenté dans `daily_reading_rules.json`.

### V2. Découpage "Tehilim de la semaine" — ✅ OK
- Validation rabbinique reçue. Découpage en 7 jours.

### V3. Présence des téamim (ta'amei ha-mikra) — décision
- Nikoud : oui par défaut.
- Téamim : retirés (option visuelle plus sobre, lisibilité accrue).

### V4. Notation orthographique sensible — ✅ OK
- 15 = טו (et non יה).
- 16 = טז (et non יו).
- Vérifié algorithmiquement via `HebrewNumerals` + tests unitaires.

### V5. Notes éditoriales "cas de la vie" (catégories de base) — ✅ OK
- Validation rabbinique reçue sur :
  - `healing` (Guérison) → 6, 13, 20, 30, 41, 88, 103, 121, 130, 142
  - `protection` → 3, 27, 91, 121, 124
  - `travel` (Voyage) → 121, 122, 124, 134
  - `parnassa` → 23, 34, 67, 121, 145
  - `pregnancy` → 1, 2, 3, 4, 5, 20, 121
  - `before_trial` (Avant un procès) → 7, 17, 26, 35, 109
  - `anxiety` (Inquiétude) → 13, 22, 27, 42, 121, 130
  - `gratitude` → 9, 30, 100, 103, 145, 150
  - `teshuva` → 6, 25, 32, 38, 51, 102, 130, 143
  - `mourning` (Deuil) → 16, 17, 33, 72, 91, 104, 130
- Aucune note ne contient de promesse médicale, juridique, financière ou psychologique.

### V5bis. Cas de la vie — catégories ajoutées V1.x — ✅ OK

Validation rabbinique reçue sur :

- `find_partner` (Trouver son mazal) → 32, 38, 70, 71, 72, 121, 124
- `wedding_day` (Jour du mariage) → 19, 20, 33, 47, 121, 145
- `brit_mila` (Brith Mila) → 12, 100, 121, 128 *(le Tehilim 12 commence par « Lamenatzeach al hashminit » — pour le huitième jour)*
- `peace` (Pour la paix) → 29, 34, 122, 125, 128, 133
- `israel` (Pour l'État d'Israël) → 20, 83, 121, 122, 130, 137
- `rosh_chodesh` (Roch Hodech) → 104, 113, 114, 115, 116, 117, 118 *(104 = Borkhi Nafchi ; 113-118 = Hallel)*
- `children` (Pour avoir des enfants) → 20, 102, 113, 121, 128

### V6. Couverture des cas spécifiques — ✅ OK
- Tefilat haDerekh : NON inclus (ce n'est pas un Tehilim).
- Vidouï : NON inclus.
- Selihot : hors périmètre V1.

## Workflow recommandé pour les évolutions futures

1. Toute nouvelle catégorie ou modification de liste de Tehilim **doit** passer par une revue rabbinique avant publication.
2. Tout changement de wording de note doit garder la formulation type « Tradition » sans promesse.
3. Hash du fichier publié à conserver dans `CHANGELOG_CONTENT.md` (à créer si besoin).
4. Spot-check par psaume aléatoire avant chaque release.

## Responsabilité

Le porteur projet (David) est responsable de la signature de la validation rabbinique. Aucune publication App Store ne peut se faire sans cette signature documentée.

---

## Addendum V1.14 — Cas de la vie

- **18 cas** désormais (ajout de **Réussite** — section « Santé et épreuves »,
  Tehilim 1·20·57·90·112·121). Données : `data/life_cases.json` v1.3.0.
- **Notes éditoriales recentrées** (décision porteur projet) : retrait du préfixe
  « Tradition. » et de **tous** les avertissements « ne remplace pas un avis… »
  (professionnel / médical / juridique / psychologique). Le footer disclaimer
  global a été supprimé de l'écran d'un cas.
- Reformulations **religieuses** pour « Avant un procès » (justice et miséricorde
  du Ciel) et « Inquiétude » (apaiser le cœur, bitahon) ; disclaimer médical
  retiré de « Pour avoir des enfants » (bénédiction conservée).
- Les autres notes conservent leur contenu factuel, sans le préfixe « Tradition. ».
