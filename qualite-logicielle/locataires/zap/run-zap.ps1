# =====================================================================
# Script PowerShell d'exécution OWASP ZAP (Baseline Frontend + API Scan Backend)
# Module : Gestion des Locataires & Baux (Port 8082, Frontend Port 3000)
# Auteur : Ahmed LAARIF — Groupe 16 (Université Ibn Zohr Agadir - FSA)
# =====================================================================

$ErrorActionPreference = "Continue"
$rootDir = Resolve-Path "$PSScriptRoot/../../.."
$outputDir = "$rootDir/docs/qualite/locataires/zap"
$backendTarget = "http://host.docker.internal:8082"
$frontendTarget = "http://host.docker.internal:3000"
$openapiSpecUrl = "$backendTarget/api-docs.json"

# Création du dossier de sortie si nécessaire
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " AUDIT DYNAMIQUE DE SÉCURITÉ APPLICATIVE OWASP ZAP (DAST)" -ForegroundColor Cyan
Write-Host " Module : Gestion des Locataires & Baux (Ahmed LAARIF - Gr. 16)" -ForegroundColor Cyan
Write-Host " Backend : $backendTarget | Frontend : $frontendTarget" -ForegroundColor Cyan
Write-Host "=================================================================`n" -ForegroundColor Cyan

# ---------------------------------------------------------------------
# 1. Baseline Scan Frontend (Port 3000)
# ---------------------------------------------------------------------
Write-Host "[1/2] Lancement du Scan Baseline Frontend ($frontendTarget)..." -ForegroundColor Yellow
docker run --rm `
  -v "${outputDir}:/zap/wrk/:rw" `
  zaproxy/zap-stable zap-baseline.py `
  -t $frontendTarget `
  -r zap-frontend-locataires-report.html `
  -x zap-frontend-locataires-report.xml `
  -I

# ---------------------------------------------------------------------
# 2. API Scan Backend (Port 8082 via OpenAPI specification)
# ---------------------------------------------------------------------
Write-Host "`n[2/2] Lancement du Scan OpenAPI Backend ($openapiSpecUrl)..." -ForegroundColor Yellow
docker run --rm `
  -v "${rootDir}:/zap/wrk/:rw" `
  zaproxy/zap-stable zap-api-scan.py `
  -t $openapiSpecUrl `
  -f openapi `
  -r docs/qualite/locataires/zap/zap-backend-locataires-report.html `
  -x docs/qualite/locataires/zap/zap-backend-locataires-report.xml `
  -I

Write-Host "`n=================================================================" -ForegroundColor Green
Write-Host " AUDIT OWASP ZAP TERMINÉ AVEC SUCCÈS !" -ForegroundColor Green
Write-Host " Rapports générés dans : docs/qualite/locataires/zap/" -ForegroundColor Green
Write-Host "=================================================================" -ForegroundColor Green
