# Gestion Locative

Application web de gestion locative en **microservices REST** : biens, locataires, loyers et quittances, avec un frontend React commun et une base PostgreSQL partagée.

> Projet de module — **Master d'Excellence, Web Services** — **Sujet n° 16 — Groupe 16** — Année universitaire 2026-2027
> *(intitulé complet de la formation et établissement : à compléter)*

---

## 1. L'équipe

| Membre | Rôle | Module | Stack du module |
|---|---|---|---|
| **BOUTBIB Ayoub** | Chef de projet | Loyers et paiements, Docker et CI/CD du projet | Java, Spring Boot |
| **EL BARKAOUI Akram** | Développeur | Gestion des quittances | Java, Spring Boot, PostgreSQL (Flyway) |
| **LAARIF Ahmed** | Développeur | Gestion des locataires | Node.js, Express |
| **OUABEN Mohamed** | Développeur | Gestion des biens, authentification commune, frontend de base | Node.js, TypeScript, Express, React |

---

## 2. Objectif et périmètre

**Objectif.** Remplacer les suivis manuels (cahiers, tableurs, reçus papier) par un outil centralisé : gérer les biens, les locataires, les baux, les loyers et les quittances, et suivre en temps réel les loyers payés, en retard ou impayés. Chaque module expose une API REST/JSON documentée, utilisée par les autres modules et par le frontend.

**Inclus :** gestion des biens, des locataires, des baux, des loyers et des quittances ; authentification des utilisateurs (bailleur, gestionnaire, locataire) ; web services d'accès aux données.

**Exclus :** paiement en ligne, comptabilité complète, gestion des travaux et de la maintenance, application mobile native.

**Contraintes :** délai de 2 mois, équipe de 4 personnes, confidentialité des données personnelles (loi 09-08 au Maroc), compatibilité avec les principaux navigateurs et usage sur mobile (responsive).

---

## 3. Architecture

```
                         ┌────────────────────────────┐
                         │   Frontend React (Vite)    │
                         │  Biens · Locataires ·      │
                         │  Loyers · Quittances       │
                         └─────────────┬──────────────┘
                                       │ REST / JSON  (Authorization: Bearer <JWT>)
        ┌───────────────┬──────────────┼───────────────┬────────────────┐
        ▼               ▼              ▼               ▼
 ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
 │   Biens     │ │ Locataires  │ │   Loyers    │ │ Quittances  │
 │ Node.js/TS  │ │ Node.js     │ │ Spring Boot │ │ Spring Boot │
 │  :8081      │ │  :8082      │ │  :8083      │ │  :8084      │
 │ + login JWT │ │             │ │             │ │             │
 └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
        └───────────────┴───── REST entre les services ──┘
                                │
                      ┌─────────▼──────────┐
                      │ PostgreSQL 16      │
                      │ db_gestion_locative│
                      └────────────────────┘
```

| Composant | Technologie | Port |
|---|---|---|
| Frontend | React 18, Vite | 3000 (développement) |
| Biens | Node.js, TypeScript, Express | 8081 |
| Locataires | Node.js, Express | 8082 |
| Loyers et paiements | Java, Spring Boot | 8083 |
| Quittances | Java, Spring Boot | 8084 |
| Base de données | PostgreSQL 16 (`db_gestion_locative`) | 5432 en local |
| Service e-mail (développement) | Mailpit | voir le module Quittances |

Pour tester le module Biens seul avec Docker (`docker-compose.biens.yml`) : base sur `5433`, base de test sur `5434`, frontend sur `3005`.

**Principes :** une architecture MVC par service ; communication entre services uniquement par REST (pas d'accès direct aux tables d'un autre module) ; aucune clé étrangère entre les tables de modules différents (liaison par UUID) ; un seul JWT partagé.

---

## 4. Stack technique

| Domaine | Technologies |
|---|---|
| **Frontend** | React 18, Vite, react-router-dom, axios, react-hook-form + zod, recharts, lucide-react, jwt-decode, CSS Modules |
| **Backend Node.js** | Express, TypeScript (Biens), `pg`, Swagger/OpenAPI (Biens) |
| **Backend Java** | Spring Boot, Maven, Spring Data JPA, Flyway (Quittances) |
| **Base de données** | PostgreSQL 16 ; scripts d'initialisation dans `database/init/` |
| **Authentification** | JWT HS256 commun (émis par le module Biens), rôles `BAILLEUR`, `GESTIONNAIRE`, `LOCATAIRE` |
| **Conteneurisation** | Docker, Docker Compose, Nginx (frontend) |
| **CI/CD** | GitHub Actions (`.github/workflows/`) |
| **Tests** | Jest, Supertest, Vitest, React Testing Library, JUnit 5, Mockito |
| **Qualité logicielle** | Istanbul, Postman/Newman, JMeter, OWASP ZAP, Snyk, SonarQube, Cypress ou Selenium |

---

## 5. Les modules

### 5.1 Gestion des biens — OUABEN Mohamed
Dossier : `backend/backend_gestion_biens/` · Port : `8081` · Documentation Swagger : `/api-docs`

- **Rôle :** CRUD des biens, disponibilité, changement de statut, et **login commun** (`/api/auth/register`, `/api/auth/login`) qui émet le JWT.
- **Entité Bien :** id (UUID), adresse, ville, type, surface, prixMensuel, charges, statut (`LIBRE` ou `OCCUPE`), dates de création et de mise à jour.
- **Endpoints principaux :** `POST /api/biens`, `GET /api/biens` (filtres ville, statut, type), `GET /api/biens/{id}`, `PUT /api/biens/{id}`, `DELETE /api/biens/{id}`, `GET /api/biens/{id}/disponibilite`, `PATCH /api/biens/{id}/statut`, `GET /health`.
- **Règles de gestion :** surface > 0 ; prix mensuel ≥ 0 ; charges ≥ 0 (0 par défaut) ; statut `LIBRE` à la création ; suppression refusée (409) si le bien est `OCCUPE` ; changement de statut refusé (409) si le bien est déjà dans ce statut ; écriture réservée à `BAILLEUR` et `GESTIONNAIRE`.
- **Frontend :** connexion, tableau de bord, catalogue (filtres, tri, cartes ou liste), ajout, détail, modification, suppression, changement de statut.

### 5.2 Gestion des locataires — LAARIF Ahmed
Dossier : `backend/backend_gestion_locataires/` (code actuellement dans `backend-locataire/`, déplacement à finaliser) · Port : `8082`

- **Rôle :** référentiel des fiches locataires, données utilisées par Loyers et Quittances.
- **Règles de gestion :** CIN unique (normalisé en majuscules, doublon refusé avec 409) ; champs obligatoires (nom, prénom, CIN, e-mail, téléphone).
- **Endpoints :** création, consultation par identifiant, recherche par CIN, liste paginée avec filtre, mise à jour de l'e-mail et du téléphone, suppression, synthèse pour les quittances (`quittance-summary`). Les chemins exacts sont à confirmer avec le responsable du module.

### 5.3 Loyers et paiements — BOUTBIB Ayoub
Dossier : `backend/backend_gestion_loyers/` · Port : `8083`

- **Rôle :** baux, échéances mensuelles de loyer, enregistrement des paiements, statut des échéances (`A_PAYER`, `PAYEE`, `EN_RETARD`, `IMPAYEE`).
- **Endpoints (baux) :** `POST /api/baux`, `GET /api/baux`, `GET /api/baux/{id}`, `POST /api/baux/{id}/terminer`. Échéances et paiements : voir la documentation du module.
- **Liens avec les autres modules :** à la création d'un bail, consultation du locataire (module Locataires) et vérification de la disponibilité du bien (module Biens) ; à la fin du bail, remise du bien à `LIBRE`.
- **Responsabilité transverse :** Docker, `docker-compose.yml` racine et CI/CD du projet.

### 5.4 Gestion des quittances — EL BARKAOUI Akram
Dossier : `backend/backend_gestion_quittances/` · Port : `8084` · API : `docs/api/openapi-quittances.yaml`

- **Rôle :** génération, consultation, validation, annulation, remplacement, téléchargement (PDF) et envoi par e-mail des quittances à partir des paiements confirmés.
- **Intégration :** valide le JWT commun ; s'appuie sur Biens, Locataires et Loyers ; ses migrations de base de données sont gérées par Flyway.
- **Frontend :** pages Quittances intégrées dans l'application React commune.

---

## 6. Authentification et rôles

Le module Biens émet un JWT HS256 (claims `sub` = identifiant de l'utilisateur dans `users`, `role`). Tous les services valident la signature avec le **même `JWT_SECRET`** (variable d'environnement, jamais dans le dépôt). Le jeton est transmis dans l'en-tête `Authorization: Bearer <token>`, y compris entre services.

| Rôle | Droits |
|---|---|
| `BAILLEUR` | Lecture et écriture ; vue limitée à ses propres biens et documents *(règle décidée, en cours de mise en œuvre)* |
| `GESTIONNAIRE` | Vue administrateur : accès à l'ensemble des données |
| `LOCATAIRE` | Lecture seule ; ses propres informations et quittances |

---

## 7. Modèle de données

Entités principales : **Bien**, **Locataire**, **Bail**, **Échéance / Paiement**, **Quittance**, **User**.

- Un bien peut avoir plusieurs baux dans le temps, mais **un seul bail actif à la fois** ; un locataire peut avoir plusieurs baux ; un bail génère une échéance par mois et une quittance par période payée.
- Scripts d'initialisation : `database/init/01_users.sql` à `06_quittances.sql` (voir `database/README.md`).

---



## 8. Tests et qualité logicielle

| Module | Tests (déclarés par le responsable) |
|---|---|
| Biens | 37 unitaires, 19 intégration (base PostgreSQL réelle), 14 frontend |
| Locataires | 14 tests unitaires |
| Loyers | 37 tests unitaires (JUnit 5, Mockito) |
| Quittances | 145 tests backend, 42 tests frontend |

Commandes (Biens) :
```bash
cd backend/backend_gestion_biens
npm test                      # unitaires + couverture Istanbul
npm run test:integration      # intégration
cd ../../frontend && npm test # frontend (Vitest)
```

Outils de qualité demandés par l'enseignant : **Istanbul** (couverture JavaScript) ; **Postman / Newman** (API) ; **JMeter** (charge) ; **Cypress ou Selenium** (E2E) ; **OWASP ZAP** et **Snyk** (sécurité) ; **SonarQube** (analyse statique) ; **JaCoCo** et **Checkstyle** pour les modules Java. Les rapports du module Biens sont dans `docs/qualite/biens/` et `docs/rapports/biens/`, avec le rapport `docs/RAPPORT_QUALITE_LOGICIELLE_BIENS.docx`. Chaque responsable documente ses propres résultats.

---

## 10. CI/CD

Workflows GitHub Actions dans `.github/workflows/` : intégration continue (build et tests) et livraison continue (images Docker). Ils doivent couvrir les quatre modules et le frontend ; leur extension est suivie par le responsable Docker et CI/CD.

---


## 11. Structure du dépôt

```
gestion-locative/
├── backend/
│   ├── backend_gestion_biens/         # Node.js, TypeScript (Biens + login commun)
│   ├── backend_gestion_locataires/    # Locataires
│   ├── backend_gestion_loyers/        # Spring Boot (Loyers et paiements)
│   └── backend_gestion_quittances/    # Spring Boot (Quittances)
├── frontend/                          # React, une seule application pour les 4 modules
├── database/init/                     # scripts SQL (01 à 06)
├── docs/                              # SFD, rapports de tests, guides, déploiement
├── qualite-logicielle/                # outillage qualité (Postman, JMeter, E2E, Sonar)
├── .github/workflows/                 # CI/CD
├── docker-compose.yml                 # orchestration complète (en consolidation)
├── docker-compose.biens.yml           # environnement autonome du module Biens
└── README.md
```

---

## 12. Décisions et points ouverts

| Sujet | État |
|---|---|
| Propriétaire d'un bien : `bailleur_id = users.id` du compte `BAILLEUR`, colonne ajoutée à `biens` depuis le `sub` du JWT vérifié | Proposé et accepté entre Biens et Quittances ; implémentation à planifier |
| Traitement des biens existants sans propriétaire | À décider (Biens) |
| Claim `iss` absent du JWT, attendu par l'audit de Quittances | À décider en équipe (émission configurable ou audit sans `iss`) |
| Propriétaire de l'entité **Bail** (Loyers ou Locataires) | **EN ATTENTE** (Ayoub et Ahmed) |
| Lien `users.id` ↔ locataire (par exemple `locataires.user_id`) | À définir avec Ahmed |
| Chemin de l'API Biens : `/api/biens/{id}` (réel) contre `/api/v1/biens/{id}` (configuration par défaut de Quittances) | À corriger côté Quittances |
| Migrations : Flyway (Quittances) ou `database/init` pour la table des quittances | À trancher entre Ayoub et Akram |
| Déplacement de `backend-locataire/` vers `backend/backend_gestion_locataires/` | À faire (Ahmed) |
| `docker-compose.yml` racine et CI complets pour les 4 services | À faire (Ayoub) |

---

## 13. Documentation

| Document | Emplacement |
|---|---|
| Note de cadrage, compte rendu UML, SFD du module Biens | `docs/` |
| Rapports de tests et rapport de qualité (Biens) | `docs/rapports/biens/`, `docs/qualite/biens/` |
| Guide utilisateur et documentation Docker (Biens) | `docs/GUIDE_UTILISATEUR_BIENS.md`, `docs/DOCKER_BIENS.md` |
| Déploiement | `docs/DEPLOIEMENT.md` |
| API Biens (Swagger) | `/api-docs` du service Biens |
| API Quittances | `backend/backend_gestion_quittances/docs/api/` |

---

*Projet académique — Groupe 16 — Novembre 2026.*
