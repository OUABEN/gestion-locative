# Compte Rendu des Tests Unitaires — Microservice `backend_gestion_biens`

**Projet** : Gestion Locative  
**Module** : `backend/backend_gestion_biens` (Node.js 20, TypeScript, Express, PostgreSQL)  
**Framework de test** : Jest 30 + `ts-jest`  
**Date d'exécution** : 07/11/2026  

---

## 1. Contexte et classe métier choisie

Dans l'architecture MVC du microservice `backend_gestion_biens`, la couche **Service** (`BienService`) concentre l'intégralité des règles métier applicatives :
- Validation de l'existence des entités avant modification ou suppression.
- Application de la contrainte métier interdisant la suppression d'un bien en statut `OCCUPE` (règle 409).
- Règle d'unicité et de transition d'état atomique du statut du bien (`LIBRE` <-> `OCCUPE`), garantissant l'adéquation avec la règle de la base : un seul bail actif par bien.
- Calcul de la disponibilité opérationnelle du bien (`disponible = true` si et seulement si `statut === 'LIBRE'`).

Pour garantir des tests unitaires stricts, **aucun serveur Express n'est démarré et aucun accès réel à PostgreSQL n'est réalisé**. La dépendance d'accès aux données `BienRepository` est entièrement simulée à l'aide de mocks Jest (`jest.fn()`), permettant d'isoler la logique pure et de vérifier les interactions (`toHaveBeenCalledWith`, nombre d'appels, codes HTTP d'`AppError`).  
En complément, les règles de validation aux limites (surface, prix, format UUID, enum de statut) ont été testées de façon unitaire sur `validation.middleware.ts`.

---

## 2. Méthodes testées

| Méthode | Rôle | Règle métier associée |
|---|---|---|
| `createBien(dto)` | Création d'un bien | Persiste le bien avec statut `LIBRE` et charges à 0 par défaut si non spécifiées. Surface > 0, prixMensuel ≥ 0. |
| `getBienById(id)` | Consultation par identifiant | Retourne le bien si trouvé ; lève une `AppError` avec code 404 si introuvable. |
| `getDisponibilite(id)` | Consultation de disponibilité | Appelé notamment par le module Loyers : `disponible: true` si et seulement si `statut === 'LIBRE'`. 404 si inexistant. |
| `updateStatut(id, statut)` | Transition d'état atomique | Mise à jour atomique. Lève 409 de conflit si le bien est déjà dans le statut demandé. Lève 404 si le bien est introuvable. |
| `deleteBien(id)` | Suppression définitive | Suppression autorisée si `statut === 'LIBRE'`. Lève une `AppError` 409 si le bien est `OCCUPE`. Lève 404 si introuvable. |
| `updateBien(id, dto)` | Modification complète | Vérifie l'existence préalable (404 si absent), puis transmet les champs mis à jour. |
| `getAllBiens(filters)` | Liste filtrée | Transmet les critères de filtrage au repository (`ville`, `statut`, `type`). |

---

## 3. Tests réalisés

Tous les tests respectent scrupuleusement le patron **Arrange-Act-Assert (AAA)** et portent des noms explicites en français.

| N° | Méthode / Cible | Type de cas | Description du test | Résultat attendu | Statut |
|:---:|---|:---:|---|---|:---:|
| 1 | `createBien` | Normal | Création d'un bien valide avec statut `LIBRE` et charges = 0 par défaut | Entité retournée avec `statut: LIBRE` et `charges: 0` | **PASS** |
| 2 | `createBien` | Limite | Surface très petite positive (0.01 m²) | Création acceptée, `surface === 0.01` | **PASS** |
| 3 | `createBien` | Limite | Prix mensuel nul (0 €) | Création acceptée, `prixMensuel === 0` | **PASS** |
| 4 | `createBien` | Limite | Absence du champ charges | Persisté avec charges = 0 | **PASS** |
| 5 | `getBienById` | Normal | Récupération d'un bien existant par son UUID | Retourne l'objet `Bien` complet | **PASS** |
| 6 | `getBienById` | Erreur | Recherche d'un bien dont l'identifiant n'existe pas | Lève `AppError` (code 404) avec message explicite | **PASS** |
| 7 | `getDisponibilite` | Normal | Disponibilité d'un bien dont le statut est `LIBRE` | Retourne `{ disponible: true, statut: 'LIBRE' }` | **PASS** |
| 8 | `getDisponibilite` | Normal | Disponibilité d'un bien dont le statut est `OCCUPE` | Retourne `{ disponible: false, statut: 'OCCUPE' }` | **PASS** |
| 9 | `getDisponibilite` | Erreur | Disponibilité demandée sur un ID inexistant | Lève `AppError` (code 404) | **PASS** |
| 10 | `updateStatut` | Normal | Changement atomique de statut vers `OCCUPE` | Entité retournée avec statut mis à jour, 1 seul appel SQL conditionnel | **PASS** |
| 11 | `updateStatut` | Normal | Changement atomique de statut vers `LIBRE` | Entité retournée avec statut mis à jour | **PASS** |
| 12 | `updateStatut` | Erreur | Changement vers le même statut que l'actuel | Lève `AppError` (code 409 Conflit) | **PASS** |
| 13 | `updateStatut` | Erreur | Modification de statut sur un bien introuvable | Lève `AppError` (code 404) | **PASS** |
| 14 | `deleteBien` | Normal | Suppression d'un bien existant ayant le statut `LIBRE` | Méthode `delete` du repository appelée, résolution sans erreur | **PASS** |
| 15 | `deleteBien` | Erreur | Tentative de suppression d'un bien avec statut `OCCUPE` | Lève `AppError` (code 409), `delete` du repository JAMAIS appelé | **PASS** |
| 16 | `deleteBien` | Erreur | Suppression demandée sur un bien introuvable | Lève `AppError` (code 404), `delete` non appelé | **PASS** |
| 17 | `updateBien` | Normal | Modification complète d'un bien existant | Entité modifiée retournée, repository appelé | **PASS** |
| 18 | `updateBien` | Erreur | Modification d'un bien inexistant | Lève `AppError` (code 404), pas d'UPDATE | **PASS** |
| 19 | `getAllBiens` | Normal | Récupération de la liste filtrée | Retourne le tableau des biens du repository | **PASS** |
| 20 | `validateUuidParam` | Normal | Validation d'un paramètre d'URL au format UUID v4 | `next()` appelé sans erreur | **PASS** |
| 21 | `validateUuidParam` | Limite / Erreur | Refus d'un identifiant non-UUID (`invalid-uuid-12345`) | Lève `AppError` (code 400) | **PASS** |
| 22 | `validateCreateBien`| Normal | Validation d'un corps de requête complet et conforme | `next()` appelé | **PASS** |
| 23 | `validateCreateBien`| Limite / Erreur | Refus d'une surface égale à 0 (règle surface > 0) | Lève `AppError` (code 400) avec détail dans `errors` | **PASS** |
| 24 | `validateCreateBien`| Limite / Erreur | Refus d'une adresse vide ou constituée d'espaces | Lève `AppError` (code 400) | **PASS** |
| 25 | `validateCreateBien`| Erreur | Refus d'un prix mensuel négatif (< 0) | Lève `AppError` (code 400) | **PASS** |
| 26 | `validateCreateBien`| Erreur | Refus de charges négatives (< 0) | Lève `AppError` (code 400) | **PASS** |
| 27 | `validatePatchStatut`| Normal | Acceptation des valeurs énumérées `LIBRE` et `OCCUPE` | `next()` appelé pour les 2 statuts | **PASS** |
| 28 | `validatePatchStatut`| Erreur | Refus d'une valeur de statut invalide (`EN_TRAVAUX`) | Lève `AppError` (code 400) | **PASS** |

---

## 4. Résultats d'exécution réels

Sortie de la commande `npm run test:report` :
- **Suites de tests** : 2 passées, 2 au total
- **Nombre total de tests** : **28**
- **Tests réussis** : **28**
- **Tests échoués** : **0**
- **Durée totale d'exécution** : 7.80 secondes

### Métriques de couverture de code (Coverage)

| Fichier / Module | % Instructions (Stmts) | % Branches | % Fonctions | % Lignes |
|---|:---:|:---:|:---:|:---:|
| **Ensemble des fichiers testés** | **73.68 %** | **54.05 %** | **92.30 %** | **72.52 %** |
| `src/services/bien.service.ts` | **91.89 %** | **78.94 %** | **100.00 %** | **91.89 %** |
| `src/middlewares/validation.middleware.ts` | **62.06 %** | **48.91 %** | **80.00 %** | **59.25 %** |

> *Note sur les lignes non couvertes dans `bien.service.ts` (lignes 38, 66, 93)* : Il s'agit des gardes de secours 500 (`Impossible de mettre à jour...`) déclenchées si le driver SQL renvoyait 0 ligne sans erreur, ce qui ne se produit pas en condition normale de mock.

---

## 5. Anomalies détectées et analyse

### Constat honnête lors de l'exécution
1. **Échec de chargement initial en environnement de test Jest** :  
   - **Comportement observé** : Lors du premier lancement, `tests/unit/bien.service.test.ts` a échoué à l'import car `env.ts` levait une exception `Variable d'environnement obligatoire manquante : JWT_SECRET`.
   - **Cause** : Le module `env.ts` s'exécute à l'évaluation du module, or Jest s'exécute en isolation sans charger automatiquement le `.env` de production.
   - **Correction appliquée** : Définition des variables d'environnement de test (`process.env.JWT_SECRET` et `process.env.DATABASE_URL`) en tête de la suite de test avant l'import de `BienService`.

2. **Absence d'anomalie métier dans le code de `BienService`** :  
   - Les 19 tests unitaires de `BienService` ont **tous réussi dès leur première exécution**.
   - Aucune divergence métier n'a été détectée : la gestion du statut `OCCUPE` à la suppression (409), l'atomicité du statut avec détection de conflit (409), et la conversion de disponibilité sont strictement conformes.

### Démarche de détection : Quels bugs auraient été interceptés ?
- Si un développeur avait utilisé `bienRepository.updateStatut(...)` sans vérifier si le statut était déjà `OCCUPE`, le test **N°12** aurait échoué en recevant `200` au lieu de l'exception 409 attendue.
- Si la condition de suppression avait omis la vérification `statut === 'OCCUPE'`, le test **N°15** aurait échoué car le mock `mockBienRepository.delete` aurait été invoqué.
- Si le calcul de disponibilité retournait `true` même pour un bien `OCCUPE`, le test **N°8** aurait immédiatement levé une assertion failure.

### Points faibles relevés (recommandations d'amélioration)
- Dans `src/services/bien.service.ts`, les vérifications de validation (surface, format UUID) sont déléguées à la couche middleware Express (`validation.middleware.ts`). Pour une architecture hexagonale ou DDD encore plus découplée, ces validations de format pourraient être encapsulées directement dans une fabrique ou une classe Value Object (ex: `Surface`, `BienId`).

---

## 6. Analyse et limites des tests unitaires

Bien que les tests unitaires valident parfaitement l'algorithmique et les règles métier en mémoire :
1. **Absence de validation SQL réelle** : Les tests unitaires ne valident pas les vraies requêtes SQL sur PostgreSQL (ex: types `NUMERIC(10,2)`, triggers, extensions `gen_random_uuid()`).
2. **Absence de test du protocole HTTP et de la chaîne de middlewares** : L'intégration globale (en-têtes HTTP, sérialisation JSON, middleware JWT avec signature réelle, codes de retour HTTP effectifs) relève des tests d'intégration (prévus dans `tests/integration/`).
3. **Cas de concurrence réseau** : L'atomicité SQL réelle sous forte charge concurrente (transactions PostgreSQL et verrous) ne peut être observée que sur une instance de base de données en direct.

---

## 7. Annexe : Reproduction et chemins des rapports

### Commandes pour reproduire
```bash
# Se placer dans le répertoire du service
cd backend/backend_gestion_biens

# Lancer la suite de tests et générer les rapports
npm run test:report

# Lancer avec affichage de la couverture uniquement
npm run test:coverage
```

### Chemins des livrables générés
- **Rapport XML JUnit** : `docs/rapports/biens/TEST-BienService.xml`
- **Rapport HTML interactif** : `docs/rapports/biens/report.html`
- **Rapport de couverture HTML LCOV** : `docs/rapports/biens/coverage/lcov-report/index.html`
- **Rapports de build locaux** : `backend/backend_gestion_biens/target/surefire-reports/`

