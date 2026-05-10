---
title: Politique de confidentialité — Tehilim
---

# Politique de confidentialité — Tehilim

**Dernière mise à jour : 2026-05-10**

L'application **Tehilim** ne collecte **aucune donnée personnelle**.

## Stockage local

Toutes les données configurées par l'utilisateur restent sur l'appareil :

- **Préférences** (taille de texte hébreu, taille de texte français, mode quotidien, thème, traduction, mode du texte, heure du rappel) : stockées via `UserDefaults` iOS, sur l'appareil uniquement.
- **Favoris** : fichier JSON local dans le dossier *Application Support* de l'app, sur l'appareil uniquement.
- **Dernière position de lecture** : `UserDefaults` iOS.
- **Mode de lecture quotidienne** (mensuel/hebdomadaire) : partagé via App Group entre l'app et le widget, toujours local.

## Connexions réseau

L'application fonctionne **entièrement hors ligne**. Le corpus des Tehilim (texte hébreu et traduction française) est embarqué dans le bundle de l'application.

Aucune connexion sortante n'est effectuée par l'application en condition d'usage normal.

## Notifications

Les rappels quotidiens sont **locaux**, programmés directement par iOS (`UNUserNotificationCenter`). Aucun serveur distant n'est impliqué. Aucun token push n'est envoyé.

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

- L'absence de collecte de données rend le droit d'accès, de rectification, ou de suppression sans objet.
- Toute donnée locale sur ton appareil est entièrement sous ton contrôle. Désinstaller l'application supprime toutes les données.

## Contact

Pour toute question sur cette politique :

- Email : [david.bouganim@gmail.com](mailto:david.bouganim@gmail.com)
- GitHub : [Dadoou5/tehilim-ios](https://github.com/Dadoou5/tehilim-ios)
