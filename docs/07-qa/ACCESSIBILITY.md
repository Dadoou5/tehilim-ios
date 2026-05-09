# Accessibilité — Auto-audit RGAA / WCAG 2.1 AA

> Date : V0.1, app Tehilim iOS native (SwiftUI)
> Référentiel : RGAA 4.1.2 (volet mobile) → équivalence WCAG 2.1 AA
> Statut : auto-déclaration — audit externe à prévoir avant publication

---

## 1. Périmètre

L'application iPhone Tehilim — l'ensemble des écrans natifs SwiftUI livrés avec la version 1.0.

## 2. Méthodologie

- Revue manuelle du code SwiftUI.
- Vérification des contrastes par calcul (script Python, formule WCAG).
- Test prévu : VoiceOver, Dynamic Type AX1..AX5, Reduce Motion, Reduce Transparency.
- Test prévu : Switch Control basique, Voice Control basique.

## 3. Résultats

### 3.1 Contrastes (WCAG 1.4.3 / RGAA 3.2)

Tous les textes principaux passent **AA** (≥ 4.5:1) :

| Token | Mode clair | Mode sombre | Statut |
|-------|-----------|-------------|--------|
| textPrimary / bg | 16.0:1 | 16.8:1 | ✅ AAA |
| textSecondary / bg | 5.0:1 | 9.3:1 | ✅ AA |
| textTertiary / bg | 5.7:1 ¹ | 6.8:1 ¹ | ✅ AA |
| accentMain / bg | 7.0:1 | 9.6:1 | ✅ AA |
| errorToken / bg | 6.2:1 | 6.6:1 | ✅ AA |

¹ Valeur mise à jour : `#5E5E63` (clair) et `#989283` (sombre) après correction.

### 3.2 Taille de texte (WCAG 1.4.4 / RGAA 10.4)

- Toutes les `Font` utilisent les styles dynamiques système (`.body`, `.caption`, `.headline`).
- Échelle interne `TextSize` (.xs à .xl) pour le texte de psaume, **en plus** de Dynamic Type.
- ✅ Pas de taille fixe pour le texte hors UI compacte.

**À vérifier sur device** : pas de coupure à AX5 sur les écrans denses (HomeView, Daily, Réglages, grille 22 lettres).

### 3.3 Cibles tactiles (WCAG 2.5.5 / RGAA 13.6)

| Élément | Taille | Statut |
|---------|--------|--------|
| Ligne de liste (List) | iOS standard ≥ 44 pt | ✅ |
| Boutons toolbar | iOS standard ≥ 44 pt | ✅ |
| HebrewLetterTile | min 88 pt | ✅ |
| ExploreCard (accueil) | ≥ 80 pt | ✅ |
| Toggle / Picker | iOS standard | ✅ |

### 3.4 VoiceOver (WCAG 4.1.2 / RGAA 6 et 7)

| Composant | Label | Trait | Statut |
|-----------|-------|-------|--------|
| Bouton recherche header | "Rechercher" | bouton | ✅ |
| Cellule de psaume (liste) | "Tehilim N · lettre" | bouton | ✅ |
| VerseRowView | "Verset N. [hébreu]. [traduction]" | texte | ✅ |
| Favori (toolbar) | "Ajouter aux favoris" / "Retirer des favoris" | bouton | ✅ |
| Toggle traduction | "Afficher / Masquer la traduction française" | bouton | ✅ |
| Tile lettre Tehilim 119 | "Section N, lettre X" + hint | bouton | ✅ |
| Section headers | trait `.isHeader` | header | ✅ |
| Titre hébreu (psaume) | trait `.isHeader` | header | ✅ |
| Bouton prev/next psaume | "Tehilim précédent N" / "Tehilim suivant N" | bouton | ✅ |

### 3.5 Couleur seule (WCAG 1.4.1 / RGAA 3.1)

Aucune information n'est transmise uniquement par la couleur :
- Le favori utilise `heart` vs `heart.fill` (forme + couleur).
- Le toggle de traduction utilise `character.bubble` vs `character.bubble.fill`.
- Les états sélectionnés (Picker segmenté) sont natifs iOS.

### 3.6 Mouvement (WCAG 2.3.3 / RGAA 13.8)

- `@Environment(\.accessibilityReduceMotion)` lu dans `PsalmDetailView` → désactive les transitions.
- Pas d'animation auto-jouée nulle part.
- Pas de parallax.

### 3.7 Sens RTL hébreu (WCAG 1.3.2)

- Le texte hébreu force `.environment(\.layoutDirection, .rightToLeft)` localement.
- Le numéro de verset est en gauche (LTR) car en chiffre arabe ou en lettre hébraïque hors séquence RTL → cohérent.
- VoiceOver lit l'hébreu phonétiquement (capacité limitée par la voix système choisie).

### 3.8 Formulaires (WCAG 3.3.2 / RGAA 11)

L'app ne contient pas de formulaire utilisateur (aucune saisie sauf la recherche), donc pas de besoin d'étiquettes formulaires.

La barre de recherche utilise `.searchable(prompt:)` natif, qui inclut le label accessible.

### 3.9 Erreur de saisie (WCAG 3.3.1)

Recherche hors plage → message clair dans `EmptyStateView` : « Aucun Tehilim trouvé… 1 et 150 ».

### 3.10 Focus visible (WCAG 2.4.7)

Géré nativement par iOS pour Switch Control / clavier externe.

## 4. Points à vérifier sur device

- [ ] AX5 sur HomeView, DailyView, BookListView, grille 22 lettres.
- [ ] VoiceOver en hébreu : choisir une voix qui lit l'hébreu (Carmit / Yoav).
- [ ] VoiceOver : test de l'ordre de lecture verset-par-verset.
- [ ] Switch Control : navigation possible sur tous les écrans.
- [ ] Voice Control : "Tap Tehilim 23" doit fonctionner.
- [ ] Mode "Bold Text" système → tout reste lisible.
- [ ] Mode "Reduce Transparency" → contraste préservé.
- [ ] Mode "Increase Contrast" → couleurs renforcées.

## 5. Critères RGAA non applicables

- 1. Images : aucune image décorative complexe (uniquement SF Symbols + texte).
- 8. Éléments obligatoires : ne s'applique pas aux apps natives.
- 12. Navigation : pas de menu HTML, navigation native iOS.

## 6. Plan d'amélioration

| Priorité | Action |
|----------|--------|
| P0 | Audit externe (PMR, WCAG-EM) avant publication App Store |
| P1 | Test utilisateur VoiceOver avec personnes aveugles (au moins 1 séance) |
| P1 | Test Dynamic Type AX5 sur 3 modèles (SE, 13, 17 Pro Max) |
| P2 | Ajouter rotor VoiceOver pour passer de verset à verset rapidement |
| P2 | Ajouter raccourcis clavier (iPad / clavier externe) |
| P3 | Mode "Increase Contrast" : palette renforcée optionnelle |

## 7. Déclaration d'accessibilité (intégrée dans l'app)

Voir `Réglages → Accessibilité → Déclaration d'accessibilité` (écran `AccessibilityDeclarationView`).

## 8. Voies de recours

À définir avant publication. Doit inclure au minimum :
- Adresse e-mail de contact.
- Délai d'engagement de réponse.
- Lien vers le Défenseur des droits (obligation RGAA secteur public ; recommandé sinon).
