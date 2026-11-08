# Microservice Gestion des Biens (`backend_gestion_biens`)

Microservice Node.js 20 + TypeScript + Express + PostgreSQL (via `pg`) en architecture MVC pour la gestion du parc de biens immobiliers.

- **Port par défaut** : `8081`
- **Authentification** : JWT Bearer partagé
- **Base de données** : PostgreSQL 16 (`db_gestion_locative`, table `biens`)
- **Documentation API** : Swagger UI sur `http://localhost:8081/api-docs`

---

## 1. Variables d'environnement

Les variables requises peuvent être fournies via les variables système ou le fichier `.env` à la racine :

| Variable | Description | Exemple |
|---|---|---|
| `PORT` | Port d'écoute du serveur | `8081` |
| `NODE_ENV` | Environnement (`development`, `production`) | `development` |
| `DATABASE_URL` | URL de connexion PostgreSQL complète | `postgresql://gl_user:password@localhost:5433/db_gestion_locative` |
| `DB_HOST` | Hôte PostgreSQL (si `DATABASE_URL` non défini) | `localhost` ou `db` |
| `DB_PORT` | Port PostgreSQL (si `DATABASE_URL` non défini) | `5433` (local) ou `5432` (Docker) |
| `DB_NAME` | Nom de la base de données | `db_gestion_locative` |
| `DB_USER` | Utilisateur PostgreSQL | `gl_user` |
| `DB_PASSWORD` | Mot de passe PostgreSQL | `change_me_db_password` |
| `JWT_SECRET` | Secret partagé de signature des JWT | *(≥ 32 caractères)* |

---

## 2. Installation et Lancement local

### Prérequis
- Node.js 20+
- Base PostgreSQL active (par exemple lancée avec `docker compose up -d db`)

### Commandes
```bash
# Dans le dossier backend/backend_gestion_biens
npm install

# Linter
npm run lint

# Compiler TypeScript vers dist/
npm run build

# Démarrer en mode développement
npm run dev

# Démarrer en production (après build)
npm start
```

---

## 3. Génération d'un token JWT de test

Un script utilitaire permet de générer des tokens JWT valides :

```bash
# Token BAILLEUR (par défaut)
npx tsx scripts/gen-token.ts BAILLEUR

# Token GESTIONNAIRE
npx tsx scripts/gen-token.ts GESTIONNAIRE

# Token LOCATAIRE
npx tsx scripts/gen-token.ts LOCATAIRE
```

---

## 4. Lancement avec Docker

Le service dispose d'un Dockerfile multi-stage (build -> runtime non-root sur `node:20-alpine`).

```bash
# Depuis la racine du projet gestion-locative :
docker compose up -d db
docker compose up --build backend_gestion_biens
```

---

## 5. Exemples d'appels `curl`

Exportez votre jeton pour simplifier les requêtes :
```bash
export TOKEN="<votre_token_jwt>"
```

### Healthcheck (sans authentification)
```bash
curl -X GET http://localhost:8081/health
```

### Créer un bien (BAILLEUR ou GESTIONNAIRE)
```bash
curl -X POST http://localhost:8081/api/biens \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "adresse": "15 Avenue des Champs-Élysées",
    "ville": "Paris",
    "type": "T3",
    "surface": 72.5,
    "prixMensuel": 1450.00,
    "charges": 120.00
  }'
```

### Lister les biens avec filtres (tout utilisateur authentifié)
```bash
# Tous les biens
curl -X GET http://localhost:8081/api/biens \
  -H "Authorization: Bearer $TOKEN"

# Filtrer par ville, type et statut
curl -X GET "http://localhost:8081/api/biens?ville=Paris&statut=LIBRE&type=T3" \
  -H "Authorization: Bearer $TOKEN"
```

### Consulter un bien par ID
```bash
curl -X GET http://localhost:8081/api/biens/<ID_DU_BIEN> \
  -H "Authorization: Bearer $TOKEN"
```

### Vérifier la disponibilité (utilisé par Loyers/Baux)
```bash
curl -X GET http://localhost:8081/api/biens/<ID_DU_BIEN>/disponibilite \
  -H "Authorization: Bearer $TOKEN"
```

### Modifier le statut de façon atomique (BAILLEUR ou GESTIONNAIRE)
```bash
curl -X PATCH http://localhost:8081/api/biens/<ID_DU_BIEN>/statut \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "statut": "OCCUPE" }'
```
*Note : Un 2ème appel identique renvoie un code 409 (conflit).*

### Modifier entièrement un bien (BAILLEUR ou GESTIONNAIRE)
```bash
curl -X PUT http://localhost:8081/api/biens/<ID_DU_BIEN> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "adresse": "15 Avenue des Champs-Élysées - Rénové",
    "ville": "Paris",
    "type": "T3",
    "surface": 75.0,
    "prixMensuel": 1500.00,
    "charges": 130.00,
    "statut": "LIBRE"
  }'
```

### Supprimer un bien (BAILLEUR ou GESTIONNAIRE)
```bash
# Réussit si le statut est LIBRE (204 No Content)
# Échoue avec 409 si le statut est OCCUPE
curl -X DELETE http://localhost:8081/api/biens/<ID_DU_BIEN> \
  -H "Authorization: Bearer $TOKEN"
```

