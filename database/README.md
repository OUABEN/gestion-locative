# Base de données — `db_gestion_locative`

Base **PostgreSQL 16** unique, partagée par les 4 microservices. Les identifiants sont des `UUID` générés par `gen_random_uuid()` (natif depuis PostgreSQL 13, aucune extension requise).

> Aucune `FOREIGN KEY` entre tables de modules différents : les liaisons (`bien_id`, `locataire_id`, `bail_id`, `echeance_id`, `paiement_id`) sont de simples colonnes `UUID NOT NULL` indexées. La cohérence est garantie par les microservices (appels REST).

## Rôle des fichiers (`init/`)

| Ordre | Fichier | Tables | Microservice |
|---|---|---|---|
| 1 | `01_users.sql` | `users` (rôles `BAILLEUR`, `GESTIONNAIRE`, `LOCATAIRE`) | Authentification JWT partagée |
| 2 | `02_biens.sql` | `biens` + index `ville`, `statut` | backend_gestion_biens (8081) |
| 3 | `03_locataires.sql` | `locataires` (`cin` unique) + index `nom`, `email` | backend_gestion_locataires (8082) |
| 4 | `04_baux.sql` | `baux` + index unique partiel : **un seul bail `ACTIF` par bien** | backend_gestion_locataires (8082) |
| 5 | `05_loyers_paiements.sql` | `echeances` (unique `bail_id, mois, annee`) et `paiements` | backend_gestion_loyers (8083) |

Les scripts n'utilisent que `CREATE TABLE IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS` : ils sont **ré-exécutables** sans erreur.

## Ordre d'exécution

```
01_users.sql → 02_biens.sql → 03_locataires.sql → 04_baux.sql → 05_loyers_paiements.sql
```

## Exécution dans pgAdmin

1. Créer la base : clic droit sur **Databases → Create → Database…**, nom : `db_gestion_locative`, puis **Save**.
2. Sélectionner la base `db_gestion_locative` dans l'arborescence.
3. Ouvrir **Tools → Query Tool**.
4. Cliquer sur **Open File** (icône dossier), choisir `init/01_users.sql`.
5. Cliquer sur **Execute** (▶ ou `F5`).
6. Répéter les étapes 4–5 pour chaque fichier, dans l'ordre (02 → 05).
7. Vérifier : **Schemas → public → Tables** (clic droit → Refresh).

## Alternative en ligne de commande (psql)

```bash
createdb -U postgres db_gestion_locative
for f in database/init/*.sql; do psql -U postgres -d db_gestion_locative -f "$f"; done
```

## Schéma Quittances : Flyway est la seule source de vérité

Les tables du service Quittances sont créées uniquement par les migrations
Flyway (backend/backend_gestion_quittances/src/main/resources/db/migration,
V1 à V9). Il n'y a plus de script correspondant dans database/init.

- Au démarrage, le service applique les migrations (baseline-on-migrate=true,
  baseline-version=0), car la base contient déjà les tables des autres modules.
- Hibernate reste en mode `validate` : il ne crée ni ne modifie le schéma.
- Valider les migrations sur une base de test neuve et isolee, sans supprimer les donnees existantes.
