# QA Checklist (avant release)

## Lancement
- [ ] Lance < 1.5 s à froid (iPhone 12+).
- [ ] Pas de splash écran trop long.
- [ ] Pas de crash au lancement (3 essais).

## Accueil
- [ ] Toutes les cartes affichées correctement.
- [ ] "Reprendre lecture" affiché si applicable.
- [ ] "Tehilim du jour" cohérent avec le mode actif.
- [ ] Tap sur chaque carte → écran cible.

## Lecture
- [ ] 150 psaumes accessibles.
- [ ] Texte hébreu RTL.
- [ ] Pas de coupures de mots étranges.
- [ ] Numérotation cohérente (hébreu OU arabe selon réglage).
- [ ] Prev/next bornes (1 sans prev, 150 sans next).
- [ ] Favori : tap → cœur rempli → persiste après relance.
- [ ] Reprise : ouvre dernier psaume + bonne position.

## Recherche
- [ ] `23` → Tehilim 23.
- [ ] `כג` → Tehilim 23.
- [ ] `tehilim 23` → Tehilim 23.
- [ ] `psaume 23` → Tehilim 23.
- [ ] `תהילים כג` → Tehilim 23.
- [ ] `0` → Aucun résultat + message.
- [ ] `151` → Aucun résultat + message.
- [ ] `abc` → Aucun résultat + suggestions.
- [ ] Récents persistent.

## Traduction FR
- [ ] Toggle global Settings fonctionnel.
- [ ] Toggle local fonctionnel.
- [ ] Préférence persistée.
- [ ] Si traduction absente → message gris ("Traduction non disponible").

## Tehilim du jour
- [ ] Mode mensuel : la liste correspond au jour hébraïque actuel.
- [ ] Mode hebdomadaire : la liste correspond au jour de la semaine actuel.
- [ ] Changement de mode immédiat.
- [ ] Avant/après minuit (test Sandbox) : la liste se met à jour.
- [ ] "Lire à la suite" enchaîne les psaumes.

## Cas de la vie
- [ ] ≥ 10 catégories visibles.
- [ ] Tap → détail avec note + liste.
- [ ] Aucune note ne contient une promesse (médicale, juridique, financière).
- [ ] Tap psaume → ouvre détail.

## Psaume 119
- [ ] 22 lettres affichées.
- [ ] Tap → bonne plage de versets.
- [ ] Navigation prev/next entre lettres.
- [ ] Aux bornes (Aleph / Tav) : boutons appropriés désactivés.

## Réglages
- [ ] Taille texte : 4+ paliers.
- [ ] Aperçu live.
- [ ] Thème Système / Clair / Sombre.
- [ ] Toggle traduction FR.
- [ ] Mode quotidien.
- [ ] Numérotation versets.
- [ ] Toutes prefs persistent après relance.

## Accessibilité
- [ ] VoiceOver : ordre de lecture correct.
- [ ] Tous les boutons ont un label.
- [ ] Dynamic Type jusqu'à AX5 sans clipping.
- [ ] Contraste AA partout.
- [ ] Mode sombre vérifié sur tous les écrans.

## Offline
- [ ] Mode avion : tous les parcours fonctionnent.
- [ ] Aucun message d'erreur réseau.

## Apple Store
- [ ] Logo / icône validés (1024px + adaptés).
- [ ] Screenshots faits sur 6.7", 6.5", 5.5", iPad si universel.
- [ ] Description App Store relue (zéro promesse religieuse / médicale).
- [ ] Privacy : "Data not collected".
- [ ] Pas de tracker SDK.
- [ ] Build TestFlight signé et installable.

## Chaîne de Tehilim (V1.14)
- [ ] Créer une chaîne → partage lien WhatsApp + QR (logo non rogné).
- [ ] Rejoindre depuis un autre appareil (prénom) → bascule participant immédiate.
- [ ] Sélection temps réel ; un Tehilim pris est verrouillé pour les autres.
- [ ] « Mes chaînes » : 3 catégories correctes + compte à rebours qui défile.
- [ ] Distribution → quitte « Sélection en cours » ; fin de lecture → « Terminées ».
- [ ] Prolonger la sélection (durée ≤ 48 h) → rappels réarmés + re-notif.
- [ ] Notifications push reçues (seuils, distribution, rappels) iOS + Android.
- [ ] Lecture hors-ligne d'une chaîne distribuée (mode avion).
- [ ] Suppression locale (Lecture / Terminées) ; suppression chaîne par le maître.
- [ ] Build sans config backend → chaîne absente, app 100 % locale OK.

## Lecture (V1.14)
- [ ] Bouton « Aa » présent (Tehilim, sections 119, lecteur de chaîne).
- [ ] Taille persistée et synchronisée avec Réglages ; 8 paliers.
- [ ] Numéro du Tehilim visible dans le contenu.
