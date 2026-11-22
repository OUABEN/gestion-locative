# Audit Dynamique de Sécurité — OWASP ZAP (DAST)

Ce dossier fournit l'outillage complet pour auditer dynamiquement l'API et l'interface du module Gestion des Biens.

## Structure
- `run-zap.ps1` : Script d'automatisation pour PowerShell sous Windows (Docker).
- `run-zap.sh` : Script d'automatisation pour Linux/macOS ou CI.
- `zap-config.xml` : Règles d'exclusion et seuils d'alertes configurés.

## Cibles du Scan
1. **Frontend React** : `http://localhost:3005` (Scan passif & baseline des en-têtes HTTP de sécurité, Content-Security-Policy, X-Content-Type-Options, Anti-Clickjacking).
2. **Backend API** : Spécification OpenAPI `backend/backend_gestion_biens/src/docs/openapi.yaml` analysée via `zap-api-scan.py` pour détecter les injections, faiblesses d'authentification et fuites de données.

## Génération des Rapports
Les rapports sont émis dans `docs/qualite/biens/zap/` :
- `zap-frontend-report.html` & `.xml`
- `zap-backend-report.html` & `.xml`
