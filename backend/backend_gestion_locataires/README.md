# Module 02 — Gestion des Locataires & Baux

- **Responsable** : Ahmed LAARIF
- **Module** : `backend_gestion_locataires` (Port **8082**)
- **Stack** : Node.js 20, Express, Jest
- **Documentation OpenAPI / Swagger** : `http://localhost:8082/api-docs`

---

## 1. Décision d'architecture : Propriété de l'entité Bail (Ahmed + Ayoub)

Conformément à la coordination technique du projet et aux scripts d'initialisation de base de données (`database/init/03_locataires.sql` et `database/init/04_baux.sql`) :

> **L'entité `Bail` est hébergée, gérée et persistée par le service `backend_gestion_locataires`.**

### Rôles et flux inter-services :
1. **Module Biens (8081)** : Lors de la création d'un bail (`POST /api/baux`), le service Locataires interroge `GET /api/biens/{id}/disponibilite`. Si le bien est disponible (`LIBRE`), le bail est créé et le bien passe en `OCCUPE` via `PATCH /api/biens/{id}/statut`. À la résiliation du bail, le bien repasse automatiquement en `LIBRE`.
2. **Module Loyers (8083 - Ayoub)** : Le client Spring `LocatairesRestClient` de Loyers consomme directement les endpoints :
   - `GET /api/baux/{id}` pour récupérer le `BailDto` (montant du loyer, locataire_id, bien_id).
   - `GET /api/baux?statut=ACTIF` pour déclencher la génération automatique des échéances mensuelles de paiement.
3. **Module Quittances (8084 - Akram)** : Le service Quittances consomme :
   - `GET /api/locataires/{id}` ou `GET /api/v1/locataires/{id}/quittance-summary` pour renseigner l'identité officielle (Nom, Prénom, CIN vérifié) sur les quittances PDF.

---

## 2. Authentification & Sécurité (JWT & RBAC)

Toutes les routes de l'API sont protégées par le middleware d'authentification JWT partageant le secret `JWT_SECRET` de la plateforme (émis par `backend_gestion_biens` lors du login `/api/auth/login`) :
- **BAILLEUR** et **GESTIONNAIRE** : Accès complet en lecture et écriture (création, mise à jour, suppression de fiches locataires et baux).
- **LOCATAIRE** : Accès restreint en lecture seule à sa propre fiche (`sub` correspondant à son identifiant) et à ses contrats de bail.
- Rejet immédiat avec code **401 Unauthorized** si le jeton est absent ou invalide.
- Rejet avec code **403 Forbidden** si un rôle tente une opération non autorisée.

---

## 3. Contrat d'API REST

| Méthode | Route | Description | Rôles autorisés |
|---|---|---|---|
| `GET` | `/health` | État de santé du microservice | Public |
| `GET` | `/api-docs` | Documentation interactive Swagger UI | Public |
| `GET` | `/api/locataires` | Liste filtrée des locataires | Tous (filtrage auto pour LOCATAIRE) |
| `GET` | `/api/locataires/{id}` | Détail d'une fiche locataire par UUID | Tous (vérification propriétaire pour LOCATAIRE) |
| `GET` | `/api/locataires/cin/{cin}` | Recherche de fiche par CIN | BAILLEUR, GESTIONNAIRE |
| `POST` | `/api/locataires` | Création d'une fiche locataire (**409 si CIN déjà utilisé**) | BAILLEUR, GESTIONNAIRE |
| `PUT` | `/api/locataires/{id}` | Mise à jour des coordonnées | BAILLEUR, GESTIONNAIRE |
| `DELETE` | `/api/locataires/{id}` | Suppression d'un locataire | BAILLEUR, GESTIONNAIRE |
| `GET` | `/api/baux` | Liste des baux (`?statut=ACTIF`, `?locataireId=...`) | Tous |
| `GET` | `/api/baux/{id}` | Lecture d'un contrat de bail (`BailDto`) | Tous |
| `POST` | `/api/baux` | Création d'un contrat de bail (**409 si bien déjà loué**) | BAILLEUR, GESTIONNAIRE |
| `PATCH`| `/api/baux/{id}/statut` | Mise à jour du statut (`ACTIF` / `TERMINE`) | BAILLEUR, GESTIONNAIRE |
| `GET` | `/api/v1/locataires/{id}/quittance-summary` | Synthèse identité pour le module Quittances | Tous |

---

## 4. Tests Unitaires & Intégration

Les 24 tests automatisés avec Jest couvrent l'intégralité du cahier des charges :
- **UT-LOC-01 à UT-LOC-14** : Les 14 cas de test du dossier de spécification (unicité CIN, formats, recherche, suppression, comptage).
- **UT-BAIL-01 à UT-BAIL-05** : Gestion des baux, unicité du bail actif par bien, statuts et interopérabilité.
- **UT-AUTH-01 à UT-AUTH-05** : Validation des signatures JWT, expiration et contrôle de rôles RBAC.

```bash
# Exécution des tests unitaires Jest
npm test
```

---

## 5. Lancement Local & Docker

```bash
# En local :
npm install
npm run dev

# En conteneur Docker :
docker build -t backend_gestion_locataires .
docker run -p 8082:8082 -e JWT_SECRET="mon_secret_jwt" backend_gestion_locataires
```