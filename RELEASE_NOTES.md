# Notes de version Tehilim

## V1.7.5 — 13 mai 2026 (build 8)

### Lecture
- **Texte hébreu justifié à droite** : numéro de verset sur le bord droit puis texte s'écoulant RTL, comme dans un Tehilim imprimé.
- **Toggle de traduction sur la section AlphaBeta** : il est maintenant possible d'afficher/masquer la traduction directement depuis chaque section du Tehilim 119.

### Design
- **Cohérence visuelle 119 – AlphaBeta** : le fond de section utilise désormais `bgPrimary` (même bleu d'eau que les autres écrans de lecture).
- **Pavés des 22 lettres** : remplacement du matériau translucide par la couleur de surface du système de design + bordure subtile pour un rendu identique aux cartes d'accueil.

---

## V1.7.4 — Ezra SIL SR + téamim
- Police hébraïque **Ezra SIL SR** (SIL OFL) supportant les téamim.
- Re-fetch de l'intégralité du corpus avec téamim préservés (2 527 versets).
- Widget carré : max 6 Tehilim affichés (au lieu de 8).
- Renommage **« Tehilim 119 » → « 119 - AlphaBeta »**.

## V1.7.3 — Frank Ruhl Libre + splash dédicace
- Police hébraïque Frank Ruhl Libre.
- Splash animé avec dédicace en hébreu et français.
- Comptages : nombre de Tehilim par livre, nombre de versets par Tehilim.
- Renommage « Taille français » → « Taille de la traduction ».
- Numérotation : « Arabe » → « Numérique ».
- Banner Ilouy nichmat : « ג׳והאן » (à la place de « יוחנן »).

## V1.7.2 — Localisation unifiée
- Sélecteur de langue unique (Système / Français / English) contrôlant à la fois l'interface et la traduction.

## V1.7 — Multilingue anglais
- Traduction anglaise (JPS 1917 via Sefaria).
- UI bilingue fr/en.

## V1.6 — App Group
- Préférences partagées app ↔ widget via `group.com.david.tehilim`.

## V1.5 — Widget Tehilim du jour
- WidgetKit (small/medium/large) avec deep-link `tehilim://`.

## V1.0 → V1.4 — MVP
- 150 Tehilim avec hébreu + français (Beth Loubavitch).
- Cas de la vie (18 catégories).
- Tehilim du jour (cycle mensuel, jour de la semaine).
- Prières avant / après lecture.
- Notifications quotidiennes.
- Onboarding, favoris, recherche.
