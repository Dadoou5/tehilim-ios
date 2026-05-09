# Revue croisée multi-agents

> Synthèse des incohérences, trous fonctionnels, risques et points à valider, avant clôture du chantier de spécification.

## 1. Incohérences détectées

| Origine | Description | Action |
|---------|-------------|--------|
| UX vs UI | UX prévoit recherche en barre permanente sur Accueil ; UI Spec utilise un bouton + sheet. | Conserver les deux : sheet pour la recherche profonde, raccourci en topbar trailing. UI Spec fait foi. |
| Contenu vs Tech | Le PRD parle de "tags" sur les psaumes ; le modèle `Psalm` les expose mais aucune feature ne les consomme. | Conserver le champ pour l'évolutivité (recherche sémantique V2+), documenté dans `ARCHITECTURE.md`. |
| Daily vs Reglages | Le mode "custom" est désactivé en V1 mais visible en sheet. | Désactivé visuellement (Picker `disabled`). Cohérent. |

## 2. Trous fonctionnels

- Pas de partage d'un verset (V2 explicite).
- Pas d'onboarding, accepté car non bloquant.
- Pas de mémoire fine de position dans un long psaume (Tehilim 119 surtout) : seul le psaume actif est mémorisé. À reconsidérer V1.1.
- Mode "Tehilim du jour" hebdomadaire sans option Shabbat spécifique → conforme V1.

## 3. Risques de design

- Mode sombre sur écran de psaume avec traduction FR : tester contraste de la traduction (gris secondaire).
- Grille des 22 lettres en AX5 : prévoir fallback liste verticale (déjà noté dans `RISKS_AND_EDGE_CASES.md`).
- Taille minimale tactile 44 × 44 pt pour les chips de psaumes du jour.

## 4. Risques techniques

- `Calendar(.hebrew)` : variabilité selon locale. Vérifier sur Israel + monde.
- Polices hébraïques : si fallback système, le rendu peut être moins beau. Pré-tester sur device.
- LazyVStack + 176 versets : OK testé sur LazyVStack mais surveiller le scroll fps en debug.

## 5. Risques de contenu

- Toute publication sans validation humaine du corpus est bloquée.
- Toute traduction sans licence claire est bloquée.
- Toute catégorie "cas de la vie" sans validation rabbinique doit être affichée avec un état neutre ("en cours de validation") plutôt qu'avec des psaumes potentiellement faux.

## 6. À valider humainement (récap)

- [ ] Source du texte hébreu.
- [ ] Source de la traduction française et licence.
- [ ] Découpage exact "Tehilim du mois".
- [ ] Découpage exact "Tehilim de la semaine".
- [ ] Liste finale des cas de la vie + psaumes associés.
- [ ] Notes éditoriales par catégorie (zéro promesse).
- [ ] Inclusion ou non des téamim.
- [ ] Police hébraïque (si non système) et licence d'embarquement.

## 7. Inconnues bloquantes pour App Store

| ID | Inconnue | Décideur |
|----|----------|----------|
| BL1 | Identité juridique de l'éditeur (compte App Store Connect) | Porteur projet |
| BL2 | Source du texte hébreu retenue | Comité éditorial / rabbinique |
| BL3 | Licence traduction française | Juridique |
| BL4 | Validation des "cas de la vie" | Rabbinique |
| BL5 | Politique de confidentialité publiée (URL) | Porteur projet |
| BL6 | App Icon finale (artwork) | Designer |
| BL7 | Texte de description App Store relu | Porteur projet |
| BL8 | Captures d'écran finales (6.7" minimum) | Designer + dev |
