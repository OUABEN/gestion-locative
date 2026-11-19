# Documentation Docker & Conteneurisation — Module Gestion des Biens

**Projet** : Gestion Locative  
**Module** : `backend/backend_gestion_biens` (port `8081`) + `frontend` (port `3000` -> `80`)  
**Fichier d'orchestration autonome** : `docker-compose.biens.yml`  
**Fichier d'environnement exemple** : `backend/backend_gestion_biens/.env.biens.example`

---

## 1. Architecture de la stack autonome `docker-compose.biens.yml`

Afin d'isoler et de valider de manière autonome le module **Gestion des Biens** sans dépendre de l'état d'avancement des autres microservices, le fichier **`docker-compose.biens.yml`** orchestre 4 conteneurs sur le réseau bridge dédié `gl_biens_net` :

| Service | Image / Build | Port Hôte → Conteneur | Rôle & Healthcheck |
|---|---|---|---|
| `db` | `postgres:16-alpine` | `5433:5432` | Base principale `db_gestion_locative`. Monte en lecture seule (`:ro`) `database/init/01_users.sql` et `02_biens.sql`. Healthcheck `pg_isready`. |
| `db_test` | `postgres:16-alpine` | `5434:5432` | Base dédiée aux tests d'intégration `db_gestion_locative_test`. Monte en lecture seule `01_users.sql` et `02_biens.sql`. Healthcheck `pg_isready`. |
| `biens` | `./backend/backend_gestion_biens` (Multi-stage Node 20 Alpine) | `8081:8081` | Microservice Express/TypeScript compilé en JS (`dist/server.js`), exécuté sous l'utilisateur non-root `node`. Dépend de `db` (`service_healthy`). Healthcheck `wget -qO- http://localhost:8081/health`. |
| `frontend` | `./frontend` (Build Vite Node 20 → Runtime `nginx:alpine`) | `3000:80` | Application React servie par Nginx avec fallback SPA (`try_files $uri $uri/ /index.html`) et reverse proxy `/api/biens` & `/api/auth` vers `http://biens:8081`. |

---

## 2. Correction apportée dans `frontend/nginx.conf`

Lors de l'audit du fichier `frontend/nginx.conf`, une anomalie bloquante de routage reverse-proxy a été identifiée et corrigée :
- **Avant** :
  ```nginx
  location /api/biens/ {
      set $upstream_biens ${BIENS_SERVICE_URL};
      rewrite ^/api/biens/(.*)$ /$1 break;
      proxy_pass $upstream_biens;
  }
  ```
  1. Les requêtes sans slash final (`GET /api/biens`, `POST /api/biens`, `GET /api/biens?ville=Rabat`) ne correspondaient pas à `location /api/biens/` et tombaient dans le fallback SPA (`index.html`).
  2. Le `rewrite` supprimait le préfixe `/api/biens/` alors que l'application Express monte son routeur sur `app.use("/api/biens", bienRouter)` et `app.use("/api/auth", authRouter)`.
- **Après correction** :
  ```nginx
  location /api/biens {
      set $upstream_biens ${BIENS_SERVICE_URL};
      proxy_pass $upstream_biens;
  }

  location /api/auth {
      set $upstream_biens ${BIENS_SERVICE_URL};
      proxy_pass $upstream_biens;
  }
  ```
  Toutes les requêtes `/api/biens`, `/api/biens/:id`, `/api/biens/:id/statut`, `/api/biens/:id/disponibilite` et `/api/auth/login` sont désormais transmises intégralement au microservice `biens:8081` avec l'en-tête `Authorization: Bearer <JWT>`.

---

## 3. Audit en lecture seule du fichier racine `docker-compose.yml`

Conformément à l'étape 5, le fichier racine `docker-compose.yml` a été vérifié en **lecture seule** (sans aucune modification) avec la commande :

```powershell
docker compose --env-file .env.example -f docker-compose.yml config --services
```

### Constat exact relevé :
```text
time="2026-11-19T01:14:15+01:00" level=warning msg="C:\\Users\\OUALI\\Downloads\\gestion-locative\\docker-compose.yml: the attribute `version` is obsolete, it will be ignored, please remove it to avoid potential confusion"
locataires-db
gestion-locataires
```
**Analyse** : Lors d'un merge précédent sur le dépôt partagé (`commit 14286ab`), le fichier racine `docker-compose.yml` a été remplacé par la configuration d'un collègue ne contenant que les deux services `locataires-db` et `gestion-locataires` (ainsi que l'attribut obsolète `version: '3.8'`). Conformément aux règles de périmètre strict (Section 0), **`docker-compose.yml` n'a pas été touché**, et toute l'orchestration du module Biens est assurée de façon autonome par **`docker-compose.biens.yml`**.

---

## 4. Résultats des vérifications Docker sur l'environnement local

1. **Versions des outils installés** :
   - `docker --version` : `Docker version 29.5.3, build d1c06ef`
   - `docker compose version` : `Docker Compose version v5.1.4`
2. **Validation de la configuration `docker-compose.biens.yml`** :
   ```powershell
   docker compose -f docker-compose.biens.yml config --services
   ```
   Résultat :
   ```text
   db
   biens
   db_test
   frontend
   ```
   La résolution complète (`docker compose -f docker-compose.biens.yml config`) s'exécute avec **code de sortie 0** (aucune erreur de syntaxe, variables d'environnement avec valeurs de repli fonctionnelles, volumes et healthchecks valides).
3. **Exécution du démon Docker Desktop** :
   Lors de l'appel à `docker compose -f docker-compose.biens.yml up -d --build`, le client Docker a retourné :
   ```text
   unable to get image 'gestion-locative/backend_gestion_biens:local': failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine; check if the path is correct and if the daemon is running: open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified.
   ```
   *Explication* : Le moteur Linux de Docker Desktop n'était pas démarré sur la machine hôte Windows au moment du test (l'environnement de développement utilisant l'instance PostgreSQL 16 native sur `localhost:5432`).

---

## 5. Commandes d'exploitation avec Docker Desktop actif

Dès que Docker Desktop est lancé sur la machine hôte :

```powershell
# 1. Démarrer la stack complète (PostgreSQL + DB Test + Backend Biens + Frontend Nginx)
docker compose -f docker-compose.biens.yml up -d --build

# 2. Vérifier l'état des conteneurs et les healthchecks (healthy)
docker compose -f docker-compose.biens.yml ps

# 3. Tester le healthcheck du microservice Biens
curl http://localhost:8081/health

# 4. Authentification et création d'un bien à travers le reverse proxy Nginx du frontend (port 3000)
curl -X POST http://localhost:3000/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"bailleur@gestion-locative.com","password":"admin123"}'

# 5. Vérifier le chargement du frontend et le fallback SPA
curl -I http://localhost:3000/
curl -I http://localhost:3000/biens

# 6. Arrêter et nettoyer les conteneurs et volumes
docker compose -f docker-compose.biens.yml down -v
```
