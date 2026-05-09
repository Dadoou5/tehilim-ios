# PRD — Application Tehilim iPhone

> Auteur : Agent Chef de Projet
> Statut : V0.1 — base de discussion
> Cible technique : iOS 17+, SwiftUI, iPhone

---

## 1. Vision produit

Offrir une application iPhone native, sobre et premium, dédiée à la lecture quotidienne et contextuelle des Tehilim (Psaumes), avec un accès rapide au texte hébreu, une traduction française activable, et des entrées thématiques (cas de la vie, Tehilim du jour, Psaume 119 par lettres).

L'application met le texte au centre. La technologie disparaît au profit de la lecture.

## 2. Utilisateurs cibles

- **Lecteur quotidien** : récite les Tehilim chaque jour, veut un accès rapide et un cycle clair.
- **Lecteur ponctuel** : ouvre l'app dans un contexte de vie (santé, voyage, deuil, gratitude) et cherche les psaumes adaptés.
- **Étudiant** : veut le texte hébreu propre, parfois la traduction, sans bruit visuel.
- **Personne francophone non hébraïsante** : a besoin de la traduction française pour comprendre.

Hypothèse : majorité d'utilisateurs francophones. Validation humaine requise sur la cible géographique principale.

## 3. Objectifs produit

- Accès au texte en moins de 2 taps depuis l'accueil.
- Lecture confortable sur iPhone, longue durée, sans fatigue visuelle.
- Recherche multi-format (chiffres arabes, lettres hébraïques, saisie tolérante).
- Fonctionnement intégral hors ligne pour le corpus principal.
- Conformité aux conventions iOS (Dynamic Type, VoiceOver, Dark Mode).

## 4. Périmètre MVP (V1.0)

✅ Inclus :
- Lecture des 150 Tehilim, regroupés par 5 livres.
- Texte hébreu complet, embarqué localement.
- Traduction française activable/désactivable, **placeholder structurel** tant que la licence n'est pas validée.
- Recherche par numéro arabe et hébreu.
- Tehilim du jour : mode cycle mensuel + jour de la semaine.
- Cas de la vie (≥ 10 catégories éditoriales).
- Psaume 119 par 22 lettres hébraïques.
- Favoris locaux, reprise de lecture.
- Réglages : taille texte, mode sombre, traduction, mode lecture quotidienne.
- Accessibilité : VoiceOver, Dynamic Type, contraste AA.

## 5. Hors périmètre MVP

❌ Exclus :
- Audio / récitation enregistrée.
- Traduction multi-langue (anglais, hébreu phonétique, etc.).
- Synchronisation iCloud / multi-device.
- Compte utilisateur, login.
- Notifications push (envisagées V2).
- Partage social.
- Rashi, commentaires, traductions multiples côte à côte.
- Translittération phonétique.

## 6. Exigences fonctionnelles

| ID | Exigence | Priorité |
|----|----------|----------|
| F1 | Lire un psaume verset par verset, avec navigation prev/next | P0 |
| F2 | Naviguer par les 5 livres | P0 |
| F3 | Liste complète 1–150 | P0 |
| F4 | Recherche par numéro arabe (`23`) | P0 |
| F5 | Recherche par lettres hébraïques (`כג`) | P0 |
| F6 | Saisie tolérante (`tehilim 23`, `תהילים כג`) | P1 |
| F7 | Toggle traduction FR global et local | P0 |
| F8 | Tehilim du jour : mode mensuel | P0 |
| F9 | Tehilim du jour : mode hebdomadaire | P0 |
| F10 | Cas de la vie : liste + détail | P0 |
| F11 | Psaume 119 par lettres | P0 |
| F12 | Favoris locaux | P0 |
| F13 | Reprise de lecture (dernier psaume vu) | P0 |
| F14 | Réglages (taille, thème, traduction, mode) | P0 |
| F15 | Mode coutume configurable | P2 |
| F16 | Historique de lecture | P2 |

## 7. Exigences non fonctionnelles

- **Performance** : ouverture < 1.5 s sur iPhone 12+, recherche < 100 ms.
- **Offline-first** : 100 % du corpus hébreu local.
- **Accessibilité** : conforme WCAG AA, Dynamic Type jusqu'à AX5, VoiceOver complet.
- **Compatibilité** : iOS 17+, iPhone uniquement V1 (iPad envisagé V2).
- **Taille app** : < 30 Mo si possible (corpus hébreu seul).
- **Stabilité** : 0 crash sur les parcours principaux.
- **Confidentialité** : aucune collecte de données personnelles, aucun tracker.

## 8. Risques

| Risque | Impact | Mitigation |
|--------|--------|------------|
| Licence traduction française non claire | Bloquant | Architecture prête, contenu différé, validation humaine |
| Erreur dans le texte hébreu | Réputation | Source unique vérifiée (validation humaine) |
| Catégorisation "cas de la vie" sensible | Réputation | Notes éditoriales prudentes, pas de promesse |
| Recherche hébreu/arabe complexe | UX | Tests dédiés sur saisies mixtes |
| Mode "Tehilim du jour" : plusieurs coutumes | Religieux | Affichage explicite du mode choisi |
| Rejet App Store (contenu religieux) | Bloquant | Description neutre, pas de promesse de résultat |

## 9. Dépendances

- Source du texte hébreu (Sefaria, Mechon Mamre, ou autre) → **validation humaine requise**.
- Source d'une traduction française libre de droits → **validation humaine requise**.
- Validation rabbinique des catégories "cas de la vie" → **validation humaine requise**.
- Validation des règles de "Tehilim du jour" (cycle mensuel et hebdomadaire selon coutume).

## 10. Métriques de succès

- D7 retention ≥ 35 %.
- Temps moyen de session ≥ 4 min.
- ≥ 60 % d'utilisateurs ouvrent les Tehilim du jour au moins 1×/semaine.
- ≥ 30 % d'utilisateurs activent la traduction FR.
- 0 crash critique.
- Note App Store ≥ 4.5.

## 11. Hypothèses

- Le texte hébreu massorétique standard est suffisant en V1.
- La traduction française n'est pas bloquante pour la sortie : le texte hébreu seul a une valeur produit.
- Les utilisateurs accepteront de choisir explicitement leur mode de "Tehilim du jour".
- Les cas de la vie peuvent être présentés sans promesse, en tant que tradition.
