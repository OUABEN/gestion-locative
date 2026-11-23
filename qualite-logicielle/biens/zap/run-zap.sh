#!/usr/bin/env bash
# =====================================================================
# Script Bash d'exécution OWASP ZAP (Baseline + API Scan)
# Module : Gestion des Biens
# =====================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_DIR="${ROOT_DIR}/docs/qualite/biens/zap"
mkdir -p "${OUTPUT_DIR}"

echo "=========================================================="
echo " Lancement de l'audit dynamique OWASP ZAP (DAST)"
echo "=========================================================="

# 1. Baseline Scan Frontend
echo -e "\n[1/2] Scan Baseline de l'interface Frontend..."
docker run --rm \
  --network host \
  -v "${OUTPUT_DIR}:/zap/wrk/:rw" \
  zaproxy/zap-stable zap-baseline.py \
  -t http://localhost:3005 \
  -r zap-frontend-report.html \
  -x zap-frontend-report.xml \
  -I || true

# 2. API Scan Backend (via OpenAPI specification)
echo -e "\n[2/2] Scan API OpenAPI du Backend..."
docker run --rm \
  --network host \
  -v "${ROOT_DIR}:/zap/wrk/:rw" \
  zaproxy/zap-stable zap-api-scan.py \
  -t /zap/wrk/backend/backend_gestion_biens/src/docs/openapi.yaml \
  -f openapi \
  -r docs/qualite/biens/zap/zap-backend-report.html \
  -x docs/qualite/biens/zap/zap-backend-report.xml \
  -I || true

echo -e "\nAudit OWASP ZAP terminé. Rapports sauvegardés dans docs/qualite/biens/zap/"
