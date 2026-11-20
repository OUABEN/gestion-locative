# Compte Rendu des Tests d'Intégration, Tests Unitaires Renforcés & Tests Frontend — Module `Gestion des Biens`

**Projet** : Gestion Locative (Master d'Excellence Web Services — Sujet n° 16, Groupe 16)  
**Périmètre** : `backend/backend_gestion_biens` (Node.js / TypeScript / Express / PostgreSQL) & `frontend` (React 18 / Vite)  
**Date d'exécution réelle** : 19/11/2026  

---

## 1. Synthèse exécutive des résultats réels constatés

Toutes les suites de tests ont été exécutées réellement et ont généré leurs rapports JUnit XML, HTML et LCOV dans `docs/rapports/biens/` et `backend/backend_gestion_biens/target/surefire-reports/` :

| Suite de tests | Outil | Fichiers de test | Tests exécutés | Réussis | Échoués | Durée réelle |
|---|---|---|:---:|:---:|:---:|:---:|
| **Tests Unitaires Backend** (`npm test`) | Jest 30 + `ts-jest` | `bien.service.test.ts`, `validation.middleware.test.ts` | **37** | **37** | **0** | `13.95 s` |
| **Tests d'Intégration Backend** (`npm run test:integration:report`) | Jest 30 + `supertest` + PostgreSQL (`db_gestion_locative_test`) | `biens.integration.test.ts` | **19** | **19** | **0** | `13.45 s` |
| **Tests Unitaires & Composants Frontend** (`npm run test:coverage`) | Vitest 5 + React Testing Library + `jsdom` | `biens.frontend.test.jsx` | **14** | **14** | **0** | `11.74 s` |
| **TOTAL GLOBAL MODULE BIENS** | — | **4 suites** | **70** | **70** | **0** | **`39.14 s`** |

---

## 2. Renforcement des Tests Unitaires Backend (Étape 3)

### 2.1. Évolution de la couverture unitaire (Avant / Après)

| Fichier | Lignes (Avant) | Lignes (Après) | Branches (Avant) | Branches (Après) | Fonctions (Après) |
|---|:---:|:---:|:---:|:---:|:---:|
| `src/middlewares/validation.middleware.ts` | `59.25 %` | **`100.00 %`** | `65.21 %` | **`100.00 %`** | **`100.00 %`** |
| `src/services/bien.service.ts` | `91.89 %` | **`100.00 %`** | `84.21 %` | **`94.73 %`** | **`100.00 %`** |
| **Total périmètre unitaire** | `72.58 %` | **`100.00 %`** | `70.76 %` | **`99.09 %`** | **`100.00 %`** |

### 2.2. Nouveaux scénarios unitaires ajoutés (sans aucun accès base de données)
1. `validateUuidParam()` : test de l'argument par défaut `paramName = "id"`.
2. `validateCreateBien` : détection combinée d'une `ville` vide (`"   "`) et d'un `type` vide (`""`), ainsi que la gestion d'un `req.body` `undefined`.
3. `validateUpdateBien` : validation d'un payload complet avec `statut: "OCCUPE"`, cumul des 7 erreurs de validation lorsque tous les champs (`adresse`, `ville`, `type`, `surface`, `prixMensuel`, `charges`, `statut`) sont invalides, et gestion d'un `req.body` `undefined`.
4. `validatePatchStatut` : rejet d'un `req.body` `undefined`.
5. `BienService` (gardes défensives 500) :
   - `updateBien` lorsque `repository.update` renvoie `null` après vérification d'existence.
   - `updateStatut` lorsque `updateStatutAtomic` renvoie `null` sans 404 ni conflit 409.
   - `deleteBien` lorsque `repository.delete` renvoie `false` sur un bien `LIBRE`.

---

## 3. Tests d'Intégration Backend avec PostgreSQL réel (Étape 2)

### 3.1. Environnement et isolation de la base de données de test
- **Base de données dédiée** : `db_gestion_locative_test` (créée automatiquement sur PostgreSQL 16 et totalement distincte de `db_gestion_locative`).
- **Initialisation du schéma** : exécution en lecture seule des scripts `database/init/01_users.sql` et `database/init/02_biens.sql` dans `beforeAll`.
- **Isolation inter-tests** : `TRUNCATE TABLE biens RESTART IDENTITY CASCADE` exécuté dans `beforeEach`.
- **Exécution HTTP** : `supertest(createApp())` sans ouvrir de port réseau conflictuel, avec de vrais jetons JWT signés avec un secret de test dédié.

### 3.2. Détail des 19 tests d'intégration exécutés

| ID | Catégorie | Scénario testé | Code HTTP attendu / obtenu | Résultat |
|---|---|---|:---:|:---:|
| IT-01 | Auth & JWT | Requête `GET /api/biens` sans en-tête `Authorization` | `401 Unauthorized` | PASS |
| IT-02 | Auth & JWT | En-tête `Authorization` mal formé (`Token ...` au lieu de `Bearer ...`) | `401 Unauthorized` | PASS |
| IT-03 | Auth & JWT | Jeton signé avec une mauvaise clé secrète ou expiré | `401 Unauthorized` | PASS |
| IT-04 | RBAC Rôles | Rôle `LOCATAIRE` refusé sur `POST`, `PUT`, `PATCH`, `DELETE /api/biens` | `403 Forbidden` | PASS |
| IT-05 | RBAC Rôles | Rôle `LOCATAIRE` autorisé sur `GET /api/biens`, `GET /:id` et `GET /:id/disponibilite` | `200 OK` | PASS |
| IT-06 | Auth DB | Cycle complet `POST /api/auth/register` (201) et `POST /api/auth/login` (200, 401 mauvais mot de passe, 400 champs manquants) | `201 / 200 / 401 / 400` | PASS |
| IT-07 | CRUD Biens | `POST /api/biens` : statut `LIBRE` par défaut, `charges = 0`, UUID v4 généré et `imageUrl` persistée en base | `201 Created` | PASS |
| IT-08 | CRUD & Filtres | `GET /api/biens` avec filtres `?ville=Rabat`, `?statut=OCCUPE`, `?type=Studio` | `200 OK` | PASS |
| IT-09 | CRUD Biens | `GET /api/biens/:id`, `PUT /api/biens/:id` (mise à jour complète) et `DELETE /api/biens/:id` sur bien `LIBRE` | `200 / 200 / 204 / 404` | PASS |
| IT-10 | Règle Métier | `DELETE /api/biens/:id` refusé sur un bien au statut `OCCUPE` | `409 Conflict` | PASS |
| IT-11 | Règle Métier | `PATCH /api/biens/:id/statut` : 1er appel `OCCUPE` accepté (200), 2e appel identique refusé (409) | `200 / 409 Conflict` | PASS |
| IT-12 | Validation | `PATCH /api/biens/:id/statut` avec valeur invalide (`RESERVE`) | `400 Bad Request` | PASS |
| IT-13 | Disponibilité | `GET /api/biens/:id/disponibilite` : `true` si `LIBRE`, `false` si `OCCUPE`, `404` si UUID inconnu | `200 / 200 / 404` | PASS |
| IT-14 | Validation UUID | Identifiant non-UUID (`not-a-valid-uuid`) -> `400` ; UUID valide inexistant -> `404` | `400 / 404` | PASS |
| IT-15 | Cas limite | `surface = 0` rejeté avec tableau détaillé `errors` | `400 Bad Request` | PASS |
| IT-16 | Cas limite | `surface = 0.01 m²` et `prixMensuel = 0 DH` acceptés | `201 Created` | PASS |
| IT-17 | Cas limite | `adresse = "   "` (espaces) et `charges = -50` rejetés avec cumul des erreurs | `400 Bad Request` | PASS |
| IT-18 | Healthcheck | `GET /health` retourne `{ service: "biens", status: "UP" }` | `200 OK` | PASS |
| IT-19 | Route 404 | Route inconnue `GET /api/route-inconnue` | `404 Not Found` | PASS |

### 3.3. Couverture globale obtenue lors des tests d'intégration
- **`All files` (`src/**`)** : **`86.10 %` Lignes** (`85.44 %` Statements, `90.19 %` Fonctions, `71.32 %` Branches).
- `src/app.ts` : `100 %`
- `src/routes/bien.routes.ts` & `auth.routes.ts` : `100 %`
- `src/repositories/bien.repository.ts` : `98.24 %`
- `src/controllers/bien.controller.ts` : `93.61 %`

---

## 4. Tests Frontend (Vitest + React Testing Library — Étape 4)

Exécutés en environnement `jsdom` avec mock intégral des couches `biensApi` et `authApi` (`vi.mock`, aucun appel réseau) :

| Groupe de tests | Composants / Modules vérifiés | Scénarios validés | Résultat |
|---|---|---|:---:|
| **1. Utilitaires** | `format.js`, `validators.js`, `jwt.js` | Formatage `DH` (`fr-MA`), valeurs `null`/`NaN`, `surface > 0`, `prixMensuel >= 0`, `charges >= 0`, champ `imageUrl`, décodage JWT valide, expiré et corrompu. | **4 / 4 PASS** |
| **2. Gardes de routes** | `ProtectedRoute.jsx`, `RoleGuard.jsx` | Redirection vers `/login` si non connecté ; affichage de l'écran *« Accès réservé »* pour `LOCATAIRE` sur `/biens/nouveau` ; accès autorisé pour `BAILLEUR`. | **2 / 2 PASS** |
| **3. Authentification** | `LoginPage.jsx` | Affichage de l'alerte d'erreur sur échec `401` ; redirection immédiate en 1 clic vers le Dashboard (`/`) sur succès. | **2 / 2 PASS** |
| **4. Formulaire Bien** | `BienFormPage.jsx` | Blocage client si `surface = 0` ; saisie de la barre photo (`imageUrl`) et affichage de la liste d'erreurs `400` du serveur. | **2 / 2 PASS** |
| **5. Catalogue Biens** | `BiensListPage.jsx` | Rendu des cartes, filtrage dynamique par recherche texte, affichage de l'état vide (*Aucun bien trouvé*), affichage de l'erreur avec bouton *Réessayer* fonctionnel. | **3 / 3 PASS** |
| **6. Tableau de bord** | `DashboardPage.jsx` | Calcul exact des KPI sur 3 biens mockés (1 `LIBRE`, 2 `OCCUPE`) : Total = `3`, Disponibles = `1`, Loués = `2`, Taux d'occupation arrondi = `67%`. | **1 / 1 PASS** |

---

## 5. Anomalies détectées et Corrections appliquées

1. **Condition de course dans `AuthContext.jsx` au premier clic sur « Se connecter »** :
   - *Avant* : `loginWithToken()` appelait uniquement `setToken(rawToken)` et attendait un `useEffect` pour définir `user` et `sessionStorage`, ce qui provoquait un rebond sur `/login` lors du premier clic.
   - *Après* : Sauvegarde synchrone dans `sessionStorage` et `localStorage` + mise à jour synchrone de `user` et `token` dans `loginWithToken()`.
2. **Réécriture d'URL erronée dans `frontend/nginx.conf`** :
   - *Avant* : `location /api/biens/` avec `rewrite ^/api/biens/(.*)$ /$1 break;` supprimait le préfixe `/api/biens` attendu par Express et ignorait `/api/biens` sans slash final.
   - *Après* : `location /api/biens` et `location /api/auth` transmettent l'URI complète vers `${BIENS_SERVICE_URL}`.
3. **Ajout de la barre photo (`imageUrl`) persistée en base PostgreSQL** :
   - Ajout non destructif de la colonne `image_url TEXT` (`ALTER TABLE biens ADD COLUMN IF NOT EXISTS image_url TEXT`) dans `BienRepository` (sans modifier `database/init/02_biens.sql`).

---

## 6. Rapports générés et Commandes pour reproduire

### Fichiers de rapports générés dans `docs/rapports/biens/`
- `docs/rapports/biens/TEST-BiensIntegration.xml` (JUnit XML — Tests d'intégration Backend)
- `docs/rapports/biens/integration-report.html` (Rapport HTML — Tests d'intégration Backend)
- `docs/rapports/biens/coverage-integration/lcov-report/index.html` (Couverture HTML — Intégration Backend)
- `docs/rapports/biens/TEST-FrontendBiens.xml` (JUnit XML — Tests Frontend Vitest)
- `docs/rapports/biens/coverage-frontend/index.html` (Couverture HTML — Tests Frontend Vitest)
- `backend/backend_gestion_biens/target/surefire-reports/TEST-BienService.xml` & `report.html` (Tests Unitaires Backend)

### Commandes pour reproduire
```powershell
# 1. Backend : Lint, Build, Tests Unitaires (sans DB) et Tests d'Intégration (avec PostgreSQL)
cd backend/backend_gestion_biens
npm run lint
npm run build
npm test
npm run test:coverage
npm run test:integration:report

# 2. Frontend : Lint, Build de production et Tests Vitest + Couverture
cd ../../frontend
npm run lint
npm run build
npm test
npm run test:coverage
```
