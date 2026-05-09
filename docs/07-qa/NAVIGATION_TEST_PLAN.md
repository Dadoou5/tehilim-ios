# Navigation — plan de tests poussés

> Suite à des bugs reportés où "la page du Tehilim reste". Couverture exhaustive de tous les chemins possibles.

---

## Règles attendues (référentiel)

1. **Tap sur un onglet inactif** → bascule vers cet onglet, en **préservant** son état (pile de navigation, segment, etc.). Comportement iOS standard.
2. **Tap sur l'onglet actif (re-tap)** → **pop-to-root** de cet onglet (vide la pile). Comportement iOS standard.
3. **Tap sur une carte de l'accueil qui bascule d'onglet** (5 livres / Cas de la vie / Mes favoris / Tehilim du jour / Tous) → bascule **avec reset** de la pile de l'onglet cible.
4. **Carte Reprendre** → push dans la pile Accueil (pas de bascule d'onglet).
5. **Carte Tehilim 119** → push dans la pile Accueil (pas d'onglet dédié).
6. **Tap notification** → bascule sur Aujourd'hui **avec reset** de sa pile.
7. **prev/next dans un Tehilim** → reste dans le contexte fourni (favoris / daily / cas / corpus complet).

---

## Suite TC-500 : tap sur onglet actif (pop-to-root)

| TC | Pré-condition | Action | Attendu |
|----|---------------|--------|---------|
| TC-500 | Sur Aujourd'hui → Tehilim 35 ouvert | Tap onglet **Aujourd'hui** | Pile Daily reset → liste du jour visible |
| TC-501 | Sur Accueil → Tehilim 23 ouvert (via Reprendre) | Tap onglet **Accueil** | Pile Home reset → écran Home visible |
| TC-502 | Sur Accueil → Tehilim 119 → Lamed | Tap onglet **Accueil** | Pile Home reset → écran Home visible |
| TC-503 | Sur Tehilim → Livre 1 → Tehilim 5 | Tap onglet **Tehilim** | Pile Psalms reset → écran segmenté visible |
| TC-504 | Sur Cas de la vie → Guérison → Tehilim 20 | Tap onglet **Cas de la vie** | Pile reset → liste catégories visible |
| TC-505 | Sur Réglages → Confidentialité | Tap onglet **Réglages** | Pile reset → liste de réglages visible |

---

## Suite TC-510 : changement d'onglet (préservation d'état)

| TC | Pré-condition | Action | Attendu |
|----|---------------|--------|---------|
| TC-510 | Sur Aujourd'hui → Tehilim 35 ouvert | Tap onglet **Tehilim** puis re-tap **Aujourd'hui** | Tehilim 35 toujours visible (état préservé) |
| TC-511 | Sur Tehilim → Livre 1 → Tehilim 5 | Tap **Aujourd'hui** puis **Tehilim** | Tehilim 5 toujours visible |
| TC-512 | Sur Cas → Guérison → Tehilim 20 | Tap **Accueil** puis **Cas de la vie** | Tehilim 20 toujours visible |

---

## Suite TC-520 : cartes d'accueil → bascule + reset

| TC | Pré-condition | Action | Attendu |
|----|---------------|--------|---------|
| TC-520 | Tehilim ouvert dans Tehilim tab → Tap Accueil | Tap carte **5 livres** | Bascule Tehilim tab segment Livres, pile **vide** (pas l'ancien Tehilim) |
| TC-521 | Tehilim 50 dans Daily tab → Tap Accueil | Tap carte **Tehilim du jour** | Bascule Daily, pile **vide** (pas Tehilim 50) |
| TC-522 | Tehilim 119 → Beth dans Cas tab → Tap Accueil | Tap carte **Cas de la vie** | Bascule Cas, pile **vide** (pas la lettre Beth) |
| TC-523 | Tehilim 30 dans Tehilim tab → Tap Accueil | Tap carte **Mes favoris** | Bascule Tehilim segment Favoris, pile **vide** |
| TC-524 | Aucun pré-état | Tap carte **Tous (1–150)** | Bascule Tehilim segment Tous |

---

## Suite TC-530 : pile Accueil — push interne

| TC | Pré-condition | Action | Attendu |
|----|---------------|--------|---------|
| TC-530 | Sur Home avec dernier Tehilim 23 connu | Tap carte **Reprendre** | PsalmDetailView(23) poussé dans homePath |
| TC-531 | Sur Home | Tap carte **Tehilim 119** | Psalm119HomeView poussé dans homePath |
| TC-532 | Home → Tehilim 119 → Aleph | Bouton retour iOS | Retour à Psalm119HomeView |
| TC-533 | Home → Reprendre → Tehilim 23 → next | PsalmDetailView(24) poussé | Bouton retour iOS revient à 23 puis Home |

---

## Suite TC-540 : navigation contextuelle prev/next

| TC | Pré-condition | Action | Attendu |
|----|---------------|--------|---------|
| TC-540 | Favoris [10, 23, 50] → Tap 23 | Bouton "next" | Tehilim 50 (pas 24) |
| TC-541 | Favoris [10, 23, 50] → Tap 50 | Bouton "next" | **Caché** (50 est dernier des favoris) |
| TC-542 | Daily [35,36,37,38] → Tap 36 | Bouton "next" | Tehilim 37 (pas suivant chronologique) |
| TC-543 | Cas Guérison → Tap 20 | Bouton "prev" | Le Tehilim précédent **dans la liste de la catégorie** |
| TC-544 | Tous (1–150) → Tap 23 | Bouton "next" | Tehilim 24 (corpus complet) |

---

## Suite TC-550 : prières — accès depuis tous les flux

| TC | Pré-condition | Action | Attendu |
|----|---------------|--------|---------|
| TC-550 | Sur Home | Tap carte **Prière avant** | Sheet de la prière s'affiche |
| TC-551 | Sur Daily list | Tap section **Prière avant** en haut | Sheet de la prière s'affiche |
| TC-552 | Sur Daily list | Tap section **Prière après** en bas | Sheet de la prière s'affiche |
| TC-553 | Sur Cas → Guérison | Tap section **Prière avant** | Sheet s'affiche |
| TC-554 | Sur Mes favoris (≥1 favori) | Tap section **Prière après** | Sheet s'affiche |
| TC-555 | Sur Tehilim 23 (n'importe où) | Toolbar `⋯` → **Prière avant** | Sheet s'affiche |
| TC-556 | Sur Tehilim 119 → Aleph | Toolbar `⋯` → **Prière après** | Sheet s'affiche |
| TC-557 | Sheet de prière ouverte | Bouton **Fermer** | Sheet disparaît, vue précédente intacte |

---

## Suite TC-560 : notifications

| TC | Pré-condition | Action | Attendu |
|----|---------------|--------|---------|
| TC-560 | App fermée, notification livrée | Tap sur la notification | App s'ouvre sur Aujourd'hui (pile vide) |
| TC-560bis | App ouverte sur Tehilim 50 dans Tehilim tab, notification reçue | Tap sur la notification | Bascule sur Aujourd'hui, pile vide. État de Tehilim tab préservé. |

---

## Suite TC-570 : edge cases pile profonde

| TC | Pré-condition | Action | Attendu |
|----|---------------|--------|---------|
| TC-570 | Home → Reprendre → 23 → next → 24 → next → 25 | Re-tap **Accueil** | Pop-to-root → écran Home (pile vide) |
| TC-571 | Daily → 35 → next → 36 → next → 37 | Re-tap **Aujourd'hui** | Pop-to-root → liste du jour |
| TC-572 | Cas → Guérison → Tehilim 6 → 13 → 20 | Re-tap **Cas de la vie** | Pop-to-root → catégories |

---

## Statut des tests (à cocher après vérification device)

- [ ] TC-500 à TC-505 (re-tap pop-to-root) — **fix V1.x.+ après refonte tabBinding**
- [ ] TC-510 à TC-512 (préservation d'état)
- [ ] TC-520 à TC-524 (cartes d'accueil avec reset)
- [ ] TC-530 à TC-533 (push interne accueil)
- [ ] TC-540 à TC-544 (navigation contextuelle)
- [ ] TC-550 à TC-557 (prières)
- [ ] TC-560, TC-560bis (notifications)
- [ ] TC-570 à TC-572 (edge cases pile profonde)
