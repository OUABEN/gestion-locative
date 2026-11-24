# Module 02 — Gestion des Locataires & Baux

- **Responsable** : Ahmed LAARIF
- **Module** : `backend_gestion_locataires` (Port **8082**)
- **Stack** : Node.js 20, Express, Jest
- **Documentation OpenAPI / Swagger** : `http://localhost:8082/api-docs`

---

## 1. Décision d'architecture : Évolution de l'entité Bail (Lecture seule)

Conformément aux nouvelles directives d'intégration de l'équipe :
> **La création et la clôture des baux sont désormais gérées par le module Loyers (`backend_gestion_loyers`).**
> **Le module Locataires conserve l'entité `Bail` en LECTURE SEULE (`GET /api/baux` et `GET /api/baux/:id`) pour les besoins de consultation, de traçabilité et d'interopérabilité.**

### Rôles et flux inter-services :
1. **Module Loyers (8083 - Ayoub)** : Gère le cycle de vie complet des baux (création, activation, clôture). Consomme :
   - `GET /api/locataires/{id}` pour récupérer l'identité du locataire lors de la création d'un bail.
   - `GET /api/baux/{id}` pour la consultation du contrat de bail (`BailDto`).
2. **Module Quittances (8084 - Akram)** : Consomme :
   - `GET /api/locataires/{id}` ou `GET /api/locataires/{id}/quittance-summary` pour renseigner l'identité officielle (Nom, Prénom, CIN vérifié) sur les quittances PDF.
3. **Module Biens (8081)** : Fournit la disponibilité des biens loués.
4. **Appels inter-services sécurisés** : Tous les échanges entre microservices transmettent le jeton dans l'en-tête `Authorization: Bearer <token>`.

---

## 2. Authentification JWT & Sécurité RBAC

Toutes les routes de l'API sont protégées par le middleware `authenticate` :
- **Algorithme & Signature** : Validation stricte avec la clé `JWT_SECRET` en algorithme `HS256`.
- **Extraction des claims** : Extraction automatique de `{ sub, role, email }` depuis le payload du token et injection dans `req.user`.
- **Rôle LOCATAIRE** :
  - Accède à `GET /api/locataires/me` pour récupérer sa fiche profil à partir du claim `email`.
  - Sur `GET /api/locataires/:id`, ne peut consulter que sa propre fiche (vérification de `sub === id` ou correspondance email), sinon **403 Forbidden**.
  - Sur `GET /api/locataires` et `GET /api/baux`, filtrage automatique restreint à ses propres données.
- **Rôles BAILLEUR et GESTIONNAIRE** : Accès complet en consultation et administration des locataires.
- Rejet immédiat avec code **401 Unauthorized** si le jeton est manquant, invalide ou expiré.
- Rejet avec code **403 Forbidden** si un rôle tente une opération non autorisée.

---

## 3. Contrat d'API REST

| Méthode | Route | Description | Rôles autorisés |
|---|---|---|---|
| `GET` | `/health` | État de santé du microservice (Healthcheck Docker) | Public |
| `GET` | `/api-docs` | Documentation interactive Swagger UI | Public |
| `GET` | `/api/locataires/me` | Profil du locataire connecté (résolu via le claim `email` du JWT) | LOCATAIRE, BAILLEUR, GESTIONNAIRE |
| `GET` | `/api/locataires` | Liste filtrée des locataires (auto-filtrée pour LOCATAIRE) | Tous |
| `GET` | `/api/locataires/{id}` | Détail d'une fiche locataire par UUID (sécurisé RBAC) | BAILLEUR, GESTIONNAIRE, LOCATAIRE (propre fiche) |
| `GET` | `/api/locataires/cin/{cin}` | Recherche de fiche par numéro CIN | BAILLEUR, GESTIONNAIRE |
| `GET` | `/api/locataires/{id}/quittance-summary` | Synthèse identité pour le module Quittances | Tous |
| `POST` | `/api/locataires` | Création d'une fiche locataire (**409 si CIN déjà utilisé**) | BAILLEUR, GESTIONNAIRE |
| `PUT` | `/api/locataires/{id}` | Mise à jour des coordonnées | BAILLEUR, GESTIONNAIRE |
| `DELETE` | `/api/locataires/{id}` | Suppression d'un locataire | BAILLEUR, GESTIONNAIRE |
| `GET` | `/api/baux` | **Lecture seule** : Liste des baux (`?statut=ACTIF`, `?locataireId=...`) | Tous |
| `GET` | `/api/baux/{id}` | **Lecture seule** : Consultation d'un contrat de bail (`BailDto`) | Tous |

*Remarque : Les routes d'écriture (`POST`, `PUT`, `PATCH`, `DELETE`) sur `/api/baux` ont été désactivées côté Locataires, la gestion des baux étant dévolue au module Loyers.*

---

## 4. Variables d'environnement standardisées (`src/config/env.js`)

Le module lit les variables d'environnement standardisées suivantes :

| Variable | Description | Valeur par défaut |
|---|---|---|
| `PORT` | Port d'écoute HTTP du microservice | `8082` |
| `NODE_ENV` | Environnement d'exécution (`production`, `development`, `test`) | `development` |
| `JWT_SECRET` | Clé secrète de validation des tokens JWT (HS256). **Jamais commité ni stocké en dur**. | Obligatoire en production |
| `BIENS_SERVICE_URL` | URL de base du microservice Biens | `http://localhost:8081` |
| `LOYERS_SERVICE_URL` | URL de base du microservice Loyers | `http://localhost:8083` |
| `QUITTANCES_SERVICE_URL` | URL de base du microservice Quittances | `http://localhost:8084` |
| `DB_HOST` | Hôte PostgreSQL (défaut `db` en environnement Docker) | `localhost` (ou `db` en Docker) |
| `DB_PORT` | Port PostgreSQL | `5432` |
| `DB_NAME` | Nom de la base de données PostgreSQL | `db_gestion_locative` (ou `POSTGRES_DB`) |
| `DB_USER` | Utilisateur PostgreSQL | `gl_user` (ou `POSTGRES_USER`) |
| `DB_PASSWORD` | Mot de passe PostgreSQL | `""` (ou `POSTGRES_PASSWORD`) |
| `DATABASE_URL` | Chaîne de connexion PostgreSQL complète | Optionnelle (déduite de DB_*) |

---

## 5. Tests Unitaires & Intégration

Les tests automatisés avec Jest vérifient :
- **Authentification JWT** : Signature HS256, extraction `{ sub, role, email }`, rejets 401/403.
- **Service Locataires** : Unicité CIN (409), validation email, CRUD, recherche par email et par ID.
- **Routes Locataires** : `GET /api/locataires/me` avec claim email, sécurité RBAC de `GET /api/locataires/:id`.
- **Lecture seule Baux** : Absence totale de routes d'écriture (`POST`, `PUT`, `PATCH`, `DELETE`) sur `/api/baux`.

```bash
# Exécution des tests unitaires Jest
npm test
```

---

## 6. Lancement Local & Docker

```bash
# En local :
npm install
npm run dev

# En conteneur Docker :
docker build -t backend_gestion_locataires .
docker run -p 8082:8082 -e JWT_SECRET="votre_secret_jwt_32_caracteres_min" backend_gestion_locataires
```
