# Gestion Locative

Application de gestion locative : gestion des **biens**, des **locataires** et des **baux**, suivi des **loyers / paiements**, et émission des **quittances**.

## Architecture

- **4 microservices backend** en architecture **MVC**
- **1 frontend** React JS
- **1 base PostgreSQL 16 partagée** : `db_gestion_locative`
- Communication **REST / JSON** entre frontend et microservices
- **Authentification JWT partagée** (même secret de signature pour tous les services)

```
                 ┌──────────────────────────┐
                 │  frontend (React) :3000  │
                 └────────────┬─────────────┘
                     REST/JSON + JWT
   ┌──────────────┬───────────┴───┬───────────────────┐
   ▼              ▼               ▼                   ▼
 biens :8081  locataires :8082  loyers :8083   quittances :8084
 (Node/TS)    (Node/TS)         (Spring Boot)  (Spring Boot)
   └──────────────┴───────┬───────┴───────────────────┘
                          ▼
             PostgreSQL : db_gestion_locative
```

## Ports

| Service | Technologie | Port |
|---|---|---|
| backend_gestion_biens | Node.js + TypeScript | 8081 |
| backend_gestion_locataires | Node.js + TypeScript | 8082 |
| backend_gestion_loyers | Spring Boot | 8083 |
| backend_gestion_quittances | Spring Boot | 8084 |
| frontend | React JS | 3000 |
| PostgreSQL | PostgreSQL 16 | 5432 |

## Base de données

Scripts d'initialisation dans [`database/init/`](database/init/) — voir [`database/README.md`](database/README.md).

## Arborescence

```
gestion-locative/
├── backend/
│   ├── backend_gestion_biens/
│   │   ├── src/
│   │   │   ├── config/
│   │   │   ├── controllers/
│   │   │   ├── middlewares/
│   │   │   ├── models/
│   │   │   ├── repositories/
│   │   │   ├── routes/
│   │   │   ├── services/
│   │   │   └── utils/
│   │   └── tests/
│   │       ├── integration/
│   │       └── unit/
│   ├── backend_gestion_locataires/
│   │   ├── src/
│   │   │   ├── config/
│   │   │   ├── controllers/
│   │   │   ├── middlewares/
│   │   │   ├── models/
│   │   │   ├── repositories/
│   │   │   ├── routes/
│   │   │   ├── services/
│   │   │   └── utils/
│   │   └── tests/
│   │       ├── integration/
│   │       └── unit/
│   ├── backend_gestion_loyers/
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/com/gestionlocative/loyers/
│   │       │   │   ├── config/
│   │       │   │   ├── controller/
│   │       │   │   ├── dto/
│   │       │   │   ├── model/
│   │       │   │   ├── repository/
│   │       │   │   ├── security/
│   │       │   │   └── service/
│   │       │   └── resources/
│   │       └── test/java/
│   └── backend_gestion_quittances/
│       └── src/
│           ├── main/
│           │   ├── java/com/gestionlocative/quittances/
│           │   │   ├── config/
│           │   │   ├── controller/
│           │   │   ├── dto/
│           │   │   ├── model/
│           │   │   ├── repository/
│           │   │   ├── security/
│           │   │   └── service/
│           │   └── resources/
│           └── test/java/
├── frontend/
│   ├── public/
│   └── src/
│       ├── assets/
│       ├── components/
│       ├── context/
│       ├── hooks/
│       ├── pages/
│       │   ├── biens/
│       │   ├── locataires/
│       │   ├── loyers/
│       │   └── quittances/
│       ├── routes/
│       ├── services/
│       └── utils/
├── database/
│   ├── init/
│   │   ├── 01_users.sql
│   │   ├── 02_biens.sql
│   │   ├── 03_locataires.sql
│   │   ├── 04_baux.sql
│   │   ├── 05_loyers_paiements.sql
│   │   └── 06_quittances.sql
│   └── README.md
├── .github/
│   └── workflows/
├── .gitignore
└── README.md
```

