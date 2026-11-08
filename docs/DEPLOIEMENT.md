# Déploiement — Gestion Locative

Ce guide décrit l'exécution locale avec **Docker Compose** et le pipeline **CI/CD GitHub Actions**.

---

## 1. Prérequis

| Outil | Version minimale | Vérification |
|---|---|---|
| Docker Engine / Docker Desktop | 24+ | `docker --version` |
| Docker Compose (plugin v2) | 2.20+ | `docker compose version` |
| Git | 2.x | `git --version` |
| Espace disque libre | **≥ 5 Go** (images Node, Maven, JRE, nginx, PostgreSQL) | `docker system df` |

Pour développer hors Docker : Node.js 20, Java 21 (Temurin) et Maven 3.9.

---

## 2. Configuration : fichier `.env`

Toutes les valeurs sensibles sont lues depuis `.env` (jamais commité, présent dans `.gitignore`).

```bash
# Linux / macOS / Git Bash
cp .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

Puis éditer `.env` et **changer au minimum** `POSTGRES_PASSWORD` et `JWT_SECRET` :

```bash
openssl rand -base64 48   # génère un JWT_SECRET robuste
```

> `docker compose` refuse de démarrer si `POSTGRES_USER`, `POSTGRES_PASSWORD` ou `JWT_SECRET` sont absents (syntaxe `${VAR:?message}`).
> Évitez les caractères `@ : / ? #` dans `POSTGRES_PASSWORD` : ils casseraient `DATABASE_URL`.

---

## 3. Commandes

| Action | Commande |
|---|---|
| Construire et démarrer toute la stack | `docker compose up --build` |
| Idem, en arrière-plan | `docker compose up --build -d` |
| Démarrer uniquement la base | `docker compose up -d db` |
| Suivre les logs (tous services) | `docker compose logs -f` |
| Suivre les logs d'un service | `docker compose logs -f backend_gestion_biens` |
| État des conteneurs | `docker compose ps` |
| Arrêter (données conservées) | `docker compose down` |
| Arrêter **et supprimer les données** (volume `pgdata`) | `docker compose down -v` |
| Vérifier la configuration résolue | `docker compose config` |
| Se connecter à la base (depuis l'hôte) | `psql -h localhost -p 5433 -U gl_user -d db_gestion_locative` |
| Se connecter à la base (dans le conteneur) | `docker compose exec db psql -U gl_user -d db_gestion_locative` |

---

## 4. Services, ports et noms Docker

Les conteneurs communiquent **par nom de service** sur le réseau `gl_network` — jamais via `localhost`.

| Service Docker | Technologie | Port hôte → conteneur | URL interne (réseau `gl_network`) |
|---|---|---|---|
| `db` | PostgreSQL 16 (alpine) | **5433** → 5432 | `db:5432` |
| `backend_gestion_biens` | Node.js 20 + TypeScript | 8081 → 8081 | `http://backend_gestion_biens:8081` |
| `backend_gestion_locataires` | Node.js 20 + TypeScript | 8082 → 8082 | `http://backend_gestion_locataires:8082` |
| `backend_gestion_loyers` | Spring Boot (Java 21) | 8083 → 8083 | `http://backend-gestion-loyers:8083` (alias) |
| `backend_gestion_quittances` | Spring Boot (Java 21) | 8084 → 8084 | `http://backend-gestion-quittances:8084` (alias) |
| `frontend` | React + nginx | **3000** → 80 | `http://frontend:80` |

> **Pourquoi des alias avec tirets pour loyers / quittances ?** Tomcat (embarqué dans Spring Boot) peut rejeter un en-tête `Host` contenant `_`. Les services Spring sont donc joignables via `backend-gestion-loyers` et `backend-gestion-quittances` (le nom avec `_` reste aussi résolu).

Le port PostgreSQL hôte est **5433** pour éviter le conflit avec une instance locale sur 5432 (modifiable via `POSTGRES_HOST_PORT`).

### Reverse proxy du frontend

nginx sert l'application React (fallback SPA vers `index.html`) et relaie les appels API :

| URL navigateur | Cible interne |
|---|---|
| `http://localhost:3000/api/biens/...` | `backend_gestion_biens:8081/...` |
| `http://localhost:3000/api/locataires/...` | `backend_gestion_locataires:8082/...` |
| `http://localhost:3000/api/loyers/...` | `backend-gestion-loyers:8083/...` |
| `http://localhost:3000/api/quittances/...` | `backend-gestion-quittances:8084/...` |

Le préfixe `/api/<service>` est retiré avant transmission. L'en-tête `Authorization` (JWT) est transmis tel quel.

---

## 5. Variables d'environnement

### Fichier `.env` (racine)

| Variable | Exemple | Rôle |
|---|---|---|
| `POSTGRES_USER` | `gl_user` | Utilisateur PostgreSQL (**obligatoire**) |
| `POSTGRES_PASSWORD` | `change_me_db_password` | Mot de passe PostgreSQL (**obligatoire, secret**) |
| `POSTGRES_DB` | `db_gestion_locative` | Nom de la base |
| `POSTGRES_HOST_PORT` | `5433` | Port PostgreSQL exposé sur l'hôte |
| `JWT_SECRET` | *(chaîne aléatoire ≥ 32 car.)* | Secret JWT partagé (**obligatoire, secret**) |
| `JWT_EXPIRES_IN` | `1h` | Durée de validité des jetons |
| `NODE_ENV` | `production` | Environnement Node.js |
| `SPRING_PROFILES_ACTIVE` | `prod` | Profil Spring Boot |
| `FRONTEND_API_BIENS_URL` | `/api/biens` | URL API biens intégrée au build React |
| `FRONTEND_API_LOCATAIRES_URL` | `/api/locataires` | URL API locataires intégrée au build React |
| `FRONTEND_API_LOYERS_URL` | `/api/loyers` | URL API loyers intégrée au build React |
| `FRONTEND_API_QUITTANCES_URL` | `/api/quittances` | URL API quittances intégrée au build React |

### Injectées dans les conteneurs backend (par `docker-compose.yml`)

| Variable | Services | Valeur |
|---|---|---|
| `PORT` | tous | 8081 / 8082 / 8083 / 8084 |
| `SERVER_PORT` | Spring Boot | 8083 / 8084 |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | tous | `db`, `5432`, valeurs du `.env` |
| `DATABASE_URL` | tous | `postgresql://USER:PASSWORD@db:5432/db_gestion_locative` |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Spring Boot | `jdbc:postgresql://db:5432/db_gestion_locative` |
| `JWT_SECRET`, `JWT_EXPIRES_IN` | tous | valeurs du `.env` |
| `BIENS_SERVICE_URL` | tous (+ frontend) | `http://backend_gestion_biens:8081` |
| `LOCATAIRES_SERVICE_URL` | tous (+ frontend) | `http://backend_gestion_locataires:8082` |
| `LOYERS_SERVICE_URL` | tous (+ frontend) | `http://backend-gestion-loyers:8083` |
| `QUITTANCES_SERVICE_URL` | tous (+ frontend) | `http://backend-gestion-quittances:8084` |

Côté React, les URLs d'API sont exposées au build sous `VITE_API_*_URL` **et** `REACT_APP_API_*_URL` (compatible Vite et Create React App).

---

## 6. Pipeline CI/CD

```
 push / pull_request (main, develop)
              │
              ▼
 ┌─────────────────────────── CI (ci.yml) ───────────────────────────┐
 │  changes : dorny/paths-filter → quels dossiers ont changé ?       │
 │     │                                                             │
 │     ├─► node-services   (biens, locataires)                       │
 │     │     npm ci → lint → build → test → artifacts coverage/junit │
 │     ├─► spring-services (loyers, quittances)                      │
 │     │     setup-java 21 → mvn verify → artifacts surefire         │
 │     ├─► frontend        npm ci → build → test                     │
 │     ├─► database        postgres:16 → psql 01..06.sql (x2)        │
 │     │                   → vérifie les 7 tables                    │
 │     └─► compose         docker compose config                     │
 │                                                                   │
 │  Étapes ignorées (job vert) si package.json / pom.xml absent      │
 └───────────────────────────────┬───────────────────────────────────┘
                                 │ workflow_run : completed
                                 │ conclusion == success
                                 │ branche main + événement push
                                 ▼
 ┌─────────────────────────── CD (cd.yml) ───────────────────────────┐
 │  Matrice des 5 services                                           │
 │   si Dockerfile ET (package.json OU pom.xml) existent :           │
 │     buildx → login ghcr.io (GITHUB_TOKEN) → build-push            │
 │     tags : ghcr.io/<owner>/<service>:latest                       │
 │            ghcr.io/<owner>/<service>:<sha du commit>              │
 │     cache : type=gha (par service)                                │
 └───────────────────────────────────────────────────────────────────┘
```

Images publiées :

- `ghcr.io/<owner>/backend_gestion_biens`
- `ghcr.io/<owner>/backend_gestion_locataires`
- `ghcr.io/<owner>/backend_gestion_loyers`
- `ghcr.io/<owner>/backend_gestion_quittances`
- `ghcr.io/<owner>/frontend`

(`<owner>` = propriétaire du dépôt GitHub, converti en minuscules.)

---

## 7. Secrets GitHub

| Secret | Nécessaire ? | Usage |
|---|---|---|
| `GITHUB_TOKEN` | Fourni automatiquement | Push vers ghcr.io (`permissions: packages: write`) |

Aucun secret supplémentaire n'est requis pour la CI/CD actuelle. Points à vérifier dans **Settings → Actions → General → Workflow permissions** : le workflow déclare lui-même `packages: write`, mais une organisation peut restreindre ce droit.

Secrets à prévoir si le pipeline évolue :

| Secret | Quand |
|---|---|
| `JWT_SECRET_TEST` | Tests d'intégration utilisant un JWT en CI |
| `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY` | Déploiement automatique sur un serveur (SSH + `docker compose pull`) |
| `PROD_POSTGRES_PASSWORD`, `PROD_JWT_SECRET` | Génération du `.env` de production sur le serveur |

Après la première publication, les paquets ghcr.io sont **privés** par défaut : les rendre publics ou donner l'accès depuis l'onglet *Packages* du profil/organisation si nécessaire.

---

## 8. Dépannage

### Disque plein (`no space left on device`, `There is not enough space on the disk`)

Les builds Docker (Maven, node_modules) consomment beaucoup d'espace.

```bash
docker system df            # voir ce qui occupe l'espace
docker builder prune        # cache de build inutilisé
docker image prune          # images non utilisées par un conteneur
docker container prune      # conteneurs arrêtés
```

> ⚠️ `docker volume prune` et `docker system prune --volumes` suppriment **définitivement** les données des volumes inutilisés (dont une éventuelle base `pgdata` arrêtée).

Sous Docker Desktop (Windows/macOS), l'espace disque virtuel se règle dans *Settings → Resources*.

### Conflit de port (`port is already allocated`, `bind: address already in use`)

Un autre programme utilise déjà le port (souvent 5433, 3000 ou 808x).

```powershell
# Windows : trouver le processus qui écoute sur le port
Get-NetTCPConnection -LocalPort 5433 -State Listen | Select-Object OwningProcess
```

```bash
# Linux / macOS
lsof -i :5433
```

Solutions : arrêter le programme concerné, ou changer le port hôte (`POSTGRES_HOST_PORT=5434` dans `.env` pour PostgreSQL ; pour les autres services, modifier la partie gauche du mapping `ports` dans `docker-compose.yml`).

### Base non initialisée (tables absentes, scripts SQL ignorés)

Les scripts de `database/init/` ne sont exécutés par l'image PostgreSQL **que lors de la première création du volume** `pgdata`. Si le volume existe déjà (même vide ou issu d'une ancienne version), ils sont ignorés : le log affiche `PostgreSQL Database directory appears to contain a database; Skipping initialization`.

Option 1 — réinitialiser complètement (⚠️ supprime les données) :

```bash
docker compose down -v
docker compose up -d db
docker compose logs -f db    # vérifier "running /docker-entrypoint-initdb.d/01_users.sql" ...
```

Option 2 — rejouer les scripts sans perdre les données (ils sont idempotents grâce à `IF NOT EXISTS`) :

```bash
for f in database/init/0*.sql; do
  docker compose exec -T db psql -v ON_ERROR_STOP=1 -U gl_user -d db_gestion_locative < "$f"
done
```

```powershell
Get-ChildItem database/init/0*.sql | ForEach-Object {
  Get-Content $_ -Raw | docker compose exec -T db psql -v ON_ERROR_STOP=1 -U gl_user -d db_gestion_locative
}
```

### Un backend redémarre en boucle

`docker compose logs -f <service>` — causes fréquentes : code du service pas encore écrit (pas de `dist/server.js` ou de jar), variable manquante, base pas encore prête (le `depends_on: condition: service_healthy` attend le healthcheck `pg_isready`).

### Erreur 502 sur `http://localhost:3000/api/...`

nginx ne joint pas le backend : vérifier que le service tourne (`docker compose ps`) et écoute bien sur le port attendu (`PORT` / `SERVER_PORT`).

