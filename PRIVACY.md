---
title: Politique de confidentialité — Tehilim
---

# Politique de confidentialité — Tehilim

**Dernière mise à jour : 2026-06-08**

La **lecture** des Tehilim (cœur de l'application) ne collecte **aucune donnée
personnelle** et fonctionne entièrement hors ligne. L'éditeur n'exploite aucun
serveur à des fins de mesure d'audience, de profilage ou de publicité.

Une **fonctionnalité collaborative optionnelle** — la **Chaîne de Tehilim** —
fait exception : uniquement si l'utilisateur **crée ou rejoint une chaîne**, un
minimum de données qu'il saisit lui-même transite par un service d'hébergement
(**Supabase**), le temps de la chaîne. Détails dans la section
[Chaîne de Tehilim](#chaîne-de-tehilim--fonctionnalité-collaborative-optionnelle).

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

## Chaîne de Tehilim — fonctionnalité collaborative optionnelle

La « Chaîne de Tehilim » permet à plusieurs personnes de se répartir la lecture
des 150 Tehilim pour une intention commune. Elle n'est utilisée **que si
l'utilisateur la choisit** (créer ou rejoindre une chaîne via un lien/QR).

**Hébergeur (sous-traitant) :** [Supabase](https://supabase.com) (Postgres,
Realtime, authentification, fonction d'envoi de notifications).

**Données traitées, et uniquement le temps de la chaîne :**
- un **identifiant anonyme** d'appareil (généré aléatoirement ; aucun compte,
  e-mail ou identité réelle) ;
- le **prénom d'affichage** librement saisi pour participer (peut être un
  pseudonyme) ;
- le **type et le détail de l'intention** saisis par le créateur ;
- la **répartition des Tehilim** (qui lit quoi) ;
- un **jeton de notification push** de l'appareil (APNs sur iOS, FCM sur
  Android), pour avertir les participants de l'avancement de la chaîne.

**Conservation et suppression :**
- chaque chaîne est automatiquement **supprimée du serveur** au plus tard
  *fin de lecture + 7 jours* ;
- un participant peut **retirer une chaîne de sa liste** à tout moment ;
- le créateur peut **supprimer** sa chaîne pour tous ;
- les jetons push devenus invalides sont automatiquement purgés.

**Sécurité :** accès régi par des règles serveur (*Row Level Security*) — chaque
utilisateur ne peut écrire que ses propres données ; un Tehilim déjà pris ne peut
être réservé par un autre.

**Désactivation :** ne pas créer ni rejoindre de chaîne suffit à n'envoyer aucune
donnée. Si l'application est distribuée sans configuration backend, la
fonctionnalité est purement et simplement absente.

## Connexions réseau

La lecture, la recherche, le widget et les rappels fonctionnent **entièrement
hors ligne** : le corpus des Tehilim (texte hébreu et traductions) est embarqué
dans le bundle de l'application.

Des connexions sortantes n'ont lieu **que pour la Chaîne de Tehilim** (vers
Supabase), lorsque l'utilisateur l'utilise. La synchronisation iCloud,
lorsqu'elle a lieu, est opérée exclusivement par iOS au niveau système.

## Notifications

- Les **rappels quotidiens** de lecture sont **locaux**, programmés par le
  système (`UNUserNotificationCenter` / WorkManager). Aucun serveur impliqué.
- Les **notifications de chaîne** (avancement, distribution, rappels) sont des
  **notifications push** envoyées via APNs (iOS) et FCM (Android), **uniquement**
  aux participants d'une chaîne. Le jeton push n'est enregistré que si
  l'utilisateur participe à une chaîne, et purgé ensuite.

## Tiers

- Pour la **lecture** : **aucun SDK tiers**, aucun tracking, aucune analytics,
  aucune publicité.
- Pour la **Chaîne de Tehilim** uniquement :
  - **Supabase** (hébergement des données de chaîne) ;
  - **Apple Push Notification service (APNs)** et **Google Firebase Cloud
    Messaging (FCM)** pour l'acheminement des notifications push.
  - Aucun de ces services n'est utilisé à des fins de profilage publicitaire.

## Sources de contenu

L'application embarque deux corpus de texte :

- **Texte hébreu** : *Miqra according to the Masorah* via [Sefaria](https://www.sefaria.org), domaine public.
- **Traduction française** : [Beth Loubavitch — le-tehilim.online](https://le-tehilim.online), 8 rue Lamartine 75009 Paris, utilisée avec **autorisation expresse de l'éditeur**.

L'attribution est affichée dans **Réglages → Sources du contenu** de l'application.

## Droits

Conformément au RGPD :

- Pour la **lecture**, l'éditeur ne collecte aucune donnée : les droits
  d'accès / rectification / suppression sont sans objet.
- Les données **locales** et **iCloud** restent sous le contrôle de l'utilisateur :
  - Désinstaller l'application supprime les données locales.
  - Supprimer Tehilim depuis **Réglages iOS → [ton nom] → iCloud → Gérer le
    stockage du compte → Tehilim** supprime également les données iCloud.
- Pour la **Chaîne de Tehilim**, les données saisies sont supprimées
  automatiquement (*fin de lecture + 7 jours*) ; l'utilisateur peut aussi
  retirer/supprimer une chaîne à tout moment. Pour toute demande de suppression
  anticipée, contacter l'éditeur (ci-dessous) ; l'identité étant anonyme, fournir
  l'identifiant ou le lien de la chaîne concernée facilite le traitement.

## Contact

Pour toute question sur cette politique :

- Email : [david.bouganim@gmail.com](mailto:david.bouganim@gmail.com)
- GitHub : [Dadoou5/tehilim-ios](https://github.com/Dadoou5/tehilim-ios)
