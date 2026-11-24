# Analyse SonarQube — Module Gestion des Biens

Ce dossier contient la configuration complète pour l'audit de qualité logicielle, dette technique et couverture sous SonarQube.

## 1. Démarrage du serveur SonarQube local

Pour démarrer SonarQube Community Edition (port 9000) :
```bash
docker compose -f docker-compose.sonar.yml up -d
```
*Note système :* SonarQube requiert au minimum 4 Go de mémoire RAM allouée et une valeur `vm.max_map_count >= 262144` pour le moteur Elasticsearch sous Linux/WSL.

## 2. Exécution du scanner

Une fois l'instance démarrée sur `http://localhost:9000` et un jeton d'authentification généré (`SONAR_TOKEN`) :

### Option A : SonarScanner CLI
```bash
sonar-scanner \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=$SONAR_TOKEN
```

### Option B : Image Docker officielle
```bash
docker run --rm \
  --network host \
  -v "$(pwd)/../../:/usr/src" \
  -w /usr/src/qualite-logicielle/biens/sonar \
  sonarsource/sonar-scanner-cli \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=$SONAR_TOKEN
```

## 3. Métriques intégrées
- Couverture Istanbul Backend : `docs/qualite/biens/istanbul-backend-integration/lcov.info`
- Couverture Istanbul Frontend : `docs/qualite/biens/istanbul-frontend/lcov.info`
- Résultats JUnit : `docs/rapports/biens/*.xml`
