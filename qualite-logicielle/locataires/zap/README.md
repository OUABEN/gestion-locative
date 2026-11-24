# Audit de Sécurité Dynamique OWASP ZAP — Module Locataires & Baux

Ce dossier contient les outils d'audit dynamique de sécurité (DAST) basés sur l'image Docker officielle d'OWASP ZAP.

## Cibles d'évaluation
- **Backend API (Node.js/Express) :** `http://localhost:8082` (Spécification OpenAPI : `/api-docs.json`)
- **Frontend SPA (React 18) :** `http://localhost:3000`

## Lancement du Scan sous Windows (PowerShell)
```powershell
.\run-zap.ps1
```

## Lancement du Scan sous Linux / macOS (Bash)
```bash
./run-zap.sh
```

## Lancement via ZAP Automation Framework (zap.yaml)
```bash
docker run --rm -v "${PWD}/../../..:/zap/wrk/:rw" zaproxy/zap-stable zap.yaml -autorun /zap/wrk/qualite-logicielle/locataires/zap/zap.yaml
```

Les rapports finaux sont enregistrés dans `docs/qualite/locataires/zap/`.
