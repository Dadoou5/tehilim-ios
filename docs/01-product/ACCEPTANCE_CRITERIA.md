# Critères d'Acceptation

## Lecture (US-01..06)
- [ ] Tous les 150 psaumes sont accessibles et lisibles intégralement.
- [ ] Le texte hébreu s'affiche en RTL avec ponctuation massorétique préservée.
- [ ] Chaque verset est numéroté (chiffres hébreux ou arabes selon réglage).
- [ ] Boutons "psaume précédent" et "psaume suivant" présents et fonctionnels (sauf bornes 1 et 150).
- [ ] Marquage favori en 1 tap, persistant après redémarrage.
- [ ] Reprise de lecture : l'accueil affiche "Reprendre Tehilim N".

## Recherche (US-10..13)
- [ ] Saisie `23` → ouvre Tehilim 23.
- [ ] Saisie `כג` → ouvre Tehilim 23.
- [ ] Saisie `tehilim 23`, `psaume 23`, `תהילים כג` → ouvre Tehilim 23.
- [ ] Saisie hors plage (`151`) → message "Aucun résultat" + suggestions.
- [ ] Saisie vide → état initial (suggestions populaires + récents).
- [ ] Latence saisie → résultat < 100 ms.

## Traduction FR (US-20..22)
- [ ] Toggle global présent dans Réglages.
- [ ] Toggle local présent en haut/bas de chaque écran de psaume.
- [ ] Préférence conservée après redémarrage.
- [ ] Si traduction absente → état "Traduction non disponible pour ce verset" sans casser la lecture.

## Tehilim du jour (US-30..32)
- [ ] L'écran "Du jour" affiche au minimum la liste de psaumes.
- [ ] Le mode actif est explicitement indiqué (mensuel, hebdomadaire).
- [ ] Le changement de mode est immédiat (≤ 200 ms).
- [ ] Si la date locale change pendant l'usage, la liste est mise à jour à la prochaine ouverture.

## Cas de la vie (US-40..42)
- [ ] Au moins 10 catégories visibles sur l'écran liste.
- [ ] Tap sur une catégorie → liste de psaumes ordonnée + note éditoriale courte.
- [ ] La note ne contient aucune promesse de résultat (médical, juridique, etc.).
- [ ] Tap sur un psaume → ouvre le détail avec retour cohérent.

## Psaume 119 (US-50..51)
- [ ] 22 sections visibles (א à ת).
- [ ] Tap sur une lettre → ouvre la section correspondante.
- [ ] Navigation prev/next entre lettres disponible.

## Réglages (US-60..62)
- [ ] Choix taille de texte : au minimum 4 paliers + Dynamic Type respecté.
- [ ] Choix de thème : Système / Clair / Sombre.
- [ ] Toutes préférences persistantes après redémarrage.

## Accessibilité (US-70..72)
- [ ] VoiceOver lit chaque verset correctement (label en hébreu et FR si activé).
- [ ] Aucun élément interactif sans label accessible.
- [ ] Tous les textes scalent jusqu'à AX5 sans clipping ni overlap.
- [ ] Contraste ≥ 4.5:1 pour le texte courant.

## Performance (US-80..82)
- [ ] Lancement à froid < 1.5 s sur iPhone 12+.
- [ ] Aucune requête réseau sortante sur les parcours principaux.
- [ ] Aucun tracker tiers ; conformité ATT (App Tracking Transparency) sans demande.

## Acceptation finale (Go App Store)
- [ ] 0 crash sur les parcours nominaux.
- [ ] Validation humaine du corpus hébreu signée.
- [ ] Validation humaine des cas de la vie signée.
- [ ] Validation humaine des règles "Tehilim du jour" signée.
- [ ] Description App Store relue (pas de promesse).
- [ ] Confidentialité conforme : aucune donnée collectée.
