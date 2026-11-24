# Module 02 — Gestion des Locataires

**Responsable :** LAARIF Ahmed  
**Projet :** Gestion locative (Master d'Excellence Web Services - Sujet n° 16, Groupe 16)  
**Technologies :** Java 17, Spring Boot 3, Spring Data JPA, PostgreSQL, JUnit Jupiter 5, AssertJ, Docker

---

## 1. Description du module
Le module « Gestion des locataires » constitue le référentiel centralisé pour la gestion des fiches locataires. Il expose une API REST pour les opérations CRUD et pour alimenter les autres microservices, notamment le module **Gestion des Quittances** d'Akram.

### Règles de gestion clés :
1. **Unicité absolue du CIN :** Normalisation `cin.trim().toUpperCase()` et rejet systématique de tout doublon avec levée de `CinDejaUtiliseException` (HTTP 409 Conflict).
2. **Intégrité des données :** Validation Jakarta Validation sur tous les champs obligatoires (nom, prénom, CIN, email et téléphone).
3. **Interopérabilité documentaire :** Endpoint optimisé `/api/v1/locataires/{id}/quittance-summary` exposant uniquement les données non sensibles nécessaires aux quittances.

---

## 2. Lancement des tests unitaires
Les 14 scénarios de test (UT-LOC-01 à UT-LOC-14) s'exécutent avec Maven :
```bash
mvn test
```

---

## 3. Déploiement avec Docker Compose
Pour démarrer le conteneur du microservice avec sa base PostgreSQL :
```bash
docker-compose up --build -d
```
L'API REST est alors accessible sur : `http://localhost:8082/api/v1/locataires`

---

## 4. Endpoints REST
- `POST /api/v1/locataires` : Créer un nouveau locataire
- `GET /api/v1/locataires/{id}` : Consulter un locataire par identifiant UUID
- `GET /api/v1/locataires/cin/{cin}` : Rechercher par CIN
- `GET /api/v1/locataires` : Lister avec pagination et filtre par nom/prénom
- `PUT /api/v1/locataires/{id}` : Mettre à jour l'email et le téléphone
- `DELETE /api/v1/locataires/{id}` : Supprimer une fiche locataire
- `GET /api/v1/locataires/{id}/quittance-summary` : Synthèse locataire pour le module Quittances

## Décision à prendre : responsabilité du bail

Le code présent aujourd'hui place la création, la lecture, la liste et la mise à jour de statut des baux dans `backend_gestion_locataires` (modèle, service, contrôleur et routes Bail). Le contrôleur de bail interroge aussi Biens pour la disponibilité et la mise à jour du statut du logement. Le service Loyers conserve des identifiants `bailId` sur les échéances et paiements et consulte les informations du bail via son client externe. Quittances, de son côté, consomme une référence de bail pour préparer ses documents.

Cette description reflète l'organisation actuelle du code. La décision de conserver la responsabilité du bail dans Locataires ou de la déplacer vers Loyers reste à prendre par Ayoub et Ahmed.
