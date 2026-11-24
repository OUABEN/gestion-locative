# SonarQube — Module Gestion des Locataires & Baux

Ce dossier contient la configuration pour exécuter l'analyse de qualité et de dette technique du module Locataires & Baux (port 8082).

## Prérequis
- Docker et Docker Compose installés.
- Tests unitaires et couverture exécutés au préalable (`npm test -- --coverage` dans `backend/backend_gestion_locataires`).

## Lancement de SonarQube en local
```bash
docker compose -f docker-compose.sonar.yml up -d
```
Accès à l'interface web : `http://localhost:9000` (identifiants par défaut : `admin` / `admin`).

## Exécution du Scanner SonarQube
```bash
docker run --rm \
  --network host \
  -v "${PWD}/../../:/usr/src" \
  -w /usr/src/qualite-logicielle/locataires/sonar \
  sonarsource/sonar-scanner-cli
```
