---
title: Politique de confidentialité — Tehilim
---

# Politique de confidentialité — Tehilim

**Dernière mise à jour : 2026-05-18**

L'application **Tehilim** ne collecte **aucune donnée personnelle**.
L'éditeur n'exploite aucun serveur stockant des informations sur ses
utilisateurs.

## Stockage local

Les préférences et l'état de lecture restent sur l'appareil :

- **Préférences** (taille de texte hébreu, taille de texte français, mode
  quotidien, thème, traduction, mode du texte, heure du rappel) : stockées
  via `UserDefaults` iOS.
- **Dernière position de lecture** : `UserDefaults` iOS.
- **Mode de lecture quotidienne** (mensuel / hebdomadaire) : partagé via
  App Group entre l'app et le widget, sur l'appareil uniquement. Si l'App
  Group n'est pas accessible (configuration de déploiement), l'application
  bascule silencieusement sur les préférences standard, sans aucun envoi
  distant.

## Synchronisation iCloud (depuis V1.10.5)

Certaines données peuvent être synchronisées **entre les appareils
connectés au même identifiant Apple** via **iCloud Key-Value Store**
(`NSUbiquitousKeyValueStore`). Cette synchronisation est gérée directement
par Apple — **l'éditeur n'a aucun accès à ce contenu** :

- **Favoris** (liste des Tehilim marqués ♡).
- **Lelouy Nichmat sauvegardés** (prénom + lien de parenté + prénom de la
  mère, saisis librement par l'utilisateur pour la lecture personnalisée).

Cette synchronisation :

- Utilise exclusivement l'infrastructure iCloud du compte Apple de
  l'utilisateur, chiffrée et opérée par Apple.
- Ne quitte jamais l'écosystème Apple ; aucun serveur de l'éditeur n'est
  impliqué.
- Peut être désactivée en se déconnectant d'iCloud dans les Réglages iOS —
  l'application continue de fonctionner en local sans erreur.
- Maintient un cache local en miroir pour le mode hors-ligne.

Aucune information personnelle identifiante automatiquement attribuée par
l'OS (identifiant Apple, email, nom complet, IDFA, etc.) n'est lue,
exportée ou transmise par l'application.

## Connexions réseau

L'application fonctionne **entièrement hors ligne**. Le corpus des Tehilim
(texte hébreu et traduction française) est embarqué dans le bundle de
l'application.

Aucune connexion sortante n'est effectuée par l'application en condition
d'usage normal. La synchronisation iCloud, lorsqu'elle a lieu, est opérée
exclusivement par iOS au niveau système.

## Notifications

Les rappels quotidiens sont **locaux**, programmés directement par iOS
(`UNUserNotificationCenter`). Aucun serveur distant n'est impliqué. Aucun
token push n'est envoyé.

## Tiers

- **Aucun SDK tiers** n'est intégré.
- **Aucun outil de tracking** (Firebase, Google Analytics, Facebook SDK, etc.).
- **Aucun outil de mesure d'audience**.
- **Aucune publicité**.

## Sources de contenu

L'application embarque deux corpus de texte :

- **Texte hébreu** : *Miqra according to the Masorah* via [Sefaria](https://www.sefaria.org), domaine public.
- **Traduction française** : [Beth Loubavitch — le-tehilim.online](https://le-tehilim.online), 8 rue Lamartine 75009 Paris, utilisée avec **autorisation expresse de l'éditeur**.

L'attribution est affichée dans **Réglages → Sources du contenu** de l'application.

## Droits

Conformément au RGPD :

- L'éditeur ne collectant aucune donnée, les droits d'accès, de
  rectification ou de suppression exercés auprès de lui sont sans objet.
- Les données stockées localement et dans iCloud restent entièrement sous
  le contrôle de l'utilisateur :
  - Désinstaller l'application supprime les données locales.
  - Supprimer Tehilim depuis **Réglages iOS → [ton nom] → iCloud → Gérer le
    stockage du compte → Tehilim** supprime également les données
    synchronisées dans iCloud.

## Contact

Pour toute question sur cette politique :

- Email : [david.bouganim@gmail.com](mailto:david.bouganim@gmail.com)
- GitHub : [Dadoou5/tehilim-ios](https://github.com/Dadoou5/tehilim-ios)
