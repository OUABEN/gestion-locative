# =====================================================================
# Script PowerShell d'exécution OWASP ZAP (Baseline + API Scan)
# Module : Gestion des Biens
# =====================================================================

$outputDir = Resolve-Path "../../docs/qualite/biens/zap"
$backendTarget = "http://host.docker.internal:8081"
$frontendTarget = "http://host.docker.internal:3005"
$openapiSpec = "/zap/wrk/backend/backend_gestion_biens/src/docs/openapi.yaml"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Lancement de l'audit dynamique OWASP ZAP (DAST)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Baseline Scan Frontend
Write-Host "`n[1/2] Scan Baseline de l'interface Frontend ($frontendTarget)..." -ForegroundColor Yellow
docker run --rm -v "${outputDir}:/zap/wrk/:rw" zaproxy/zap-stable zap-baseline.py `
  -t $frontendTarget `
  -r zap-frontend-report.html `
  -x zap-frontend-report.xml `
  -I

# 2. API Scan Backend (via OpenAPI specification)
Write-Host "`n[2/2] Scan API OpenAPI du Backend ($backendTarget)..." -ForegroundColor Yellow
docker run --rm -v "${PSScriptRoot}/../../:/zap/wrk/:rw" zaproxy/zap-stable zap-api-scan.py `
  -t $openapiSpec `
  -f openapi `
  -r docs/qualite/biens/zap/zap-backend-report.html `
  -x docs/qualite/biens/zap/zap-backend-report.xml `
  -I

Write-Host "`nAudit OWASP ZAP terminé. Rapports générés dans docs/qualite/biens/zap/" -ForegroundColor Green
