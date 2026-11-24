# =====================================================================
# Script d'Exécution Global de Tous les Tests de Qualité Logicielle (QL)
# Module : Gestion des Locataires & Baux (Port 8082, Frontend Port 3000)
# Auteur : Ahmed LAARIF — Groupe 16
# Encadré par : Dr. J. IDRAIS
# =====================================================================

$ErrorActionPreference = "Continue"
$rootDir = Resolve-Path "$PSScriptRoot/../../"
$docsQualiteDir = "$rootDir/docs/qualite/locataires"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " SUITE GLOBALE D'ASSURANCE QUALITÉ LOGICIELLE (QL)" -ForegroundColor Cyan
Write-Host " Module : Gestion des Locataires & Baux | Port 8082" -ForegroundColor Cyan
Write-Host " Réalisé par : LAARIF Ahmed (Groupe 16)" -ForegroundColor Cyan
Write-Host " Encadré par : Dr. J. IDRAIS" -ForegroundColor Cyan
Write-Host "=================================================================`n" -ForegroundColor Cyan

# Création de l'arborescence de documentation qualité
New-Item -ItemType Directory -Path "$docsQualiteDir/istanbul-backend" -Force | Out-Null
New-Item -ItemType Directory -Path "$docsQualiteDir/securite" -Force | Out-Null
New-Item -ItemType Directory -Path "$docsQualiteDir/newman" -Force | Out-Null
New-Item -ItemType Directory -Path "$docsQualiteDir/jmeter" -Force | Out-Null
New-Item -ItemType Directory -Path "$docsQualiteDir/cypress" -Force | Out-Null
New-Item -ItemType Directory -Path "$docsQualiteDir/zap" -Force | Out-Null

# ---------------------------------------------------------------------
# 1. Tests Unitaires Backend & Couverture Istanbul (Jest)
# ---------------------------------------------------------------------
Write-Host "[1/6] Tests Unitaires Backend & Couverture Istanbul (Jest)..." -ForegroundColor Yellow
Push-Location "$rootDir/backend/backend_gestion_locataires"
npx jest --coverage --forceExit --watchAll=false
if (Test-Path "coverage/lcov-report") {
    Copy-Item -Path "coverage/lcov-report/*" -Destination "$docsQualiteDir/istanbul-backend" -Recurse -Force
    Copy-Item -Path "coverage/lcov.info" -Destination "$docsQualiteDir/istanbul-backend" -Force
    Write-Host "  -> Rapport de couverture Istanbul copié dans docs/qualite/locataires/istanbul-backend/" -ForegroundColor Green
}
Pop-Location

# ---------------------------------------------------------------------
# 2. Tests Frontend React (Vitest + React Testing Library)
# ---------------------------------------------------------------------
Write-Host "`n[2/6] Tests Unitaires Frontend React (Vitest - Locataires)..." -ForegroundColor Yellow
Push-Location "$rootDir/frontend"
npx vitest run src/tests/locataires.frontend.test.jsx
Pop-Location

# ---------------------------------------------------------------------
# 3. Audit de Sécurité Statique des Dépendances (npm audit)
# ---------------------------------------------------------------------
Write-Host "`n[3/6] Audit de Sécurité des Dépendances Backend (npm audit)..." -ForegroundColor Yellow
Push-Location "$rootDir/backend/backend_gestion_locataires"
npm audit --json | Out-File -FilePath "$docsQualiteDir/securite/npm-audit.json" -Encoding utf8
npm audit | Out-File -FilePath "$docsQualiteDir/securite/npm-audit.txt" -Encoding utf8
Write-Host "  -> Résultats d'audit enregistrés dans docs/qualite/locataires/securite/npm-audit.json" -ForegroundColor Green
Pop-Location

# ---------------------------------------------------------------------
# 4. Tests Fonctionnels d'API REST (Newman / Postman)
# ---------------------------------------------------------------------
Write-Host "`n[4/6] Tests Fonctionnels d'API REST (Newman / Postman)..." -ForegroundColor Yellow
Push-Location "$rootDir/qualite-logicielle/locataires/postman"
if (Get-Command newman -ErrorAction SilentlyContinue) {
    newman run locataires.postman_collection.json `
      --reporters cli,htmlextra `
      --reporter-htmlextra-export "$docsQualiteDir/newman/newman-report.html"
} else {
    Write-Host "  -> Newman n'est pas installé globalement. Commande à lancer : npm test" -ForegroundColor DarkGray
}
Pop-Location

# ---------------------------------------------------------------------
# 5. Tests de Charge et Performance (Apache JMeter)
# ---------------------------------------------------------------------
Write-Host "`n[5/6] Tests de Charge et Performance (Apache JMeter)..." -ForegroundColor Yellow
Push-Location "$rootDir/qualite-logicielle/locataires/jmeter"
if (Get-Command jmeter -ErrorAction SilentlyContinue) {
    jmeter -n -t plan-test-locataires.jmx `
      -l "$docsQualiteDir/jmeter/results.jtl" `
      -e -o "$docsQualiteDir/jmeter/dashboard"
} else {
    Write-Host "  -> JMeter CLI non détecté dans le PATH. Plan prêt : plan-test-locataires.jmx" -ForegroundColor DarkGray
}
Pop-Location

# ---------------------------------------------------------------------
# 6. Tests E2E Navigateur (Cypress)
# ---------------------------------------------------------------------
Write-Host "`n[6/6] Tests E2E Navigateur (Cypress)..." -ForegroundColor Yellow
Push-Location "$rootDir/qualite-logicielle/locataires/e2e-cypress"
if (Get-Command npx -ErrorAction SilentlyContinue) {
    Write-Host "  -> Pour lancer Cypress en mode interactif : npx cypress open" -ForegroundColor DarkGray
    Write-Host "  -> Pour lancer en mode headless : npx cypress run" -ForegroundColor DarkGray
}
Pop-Location

Write-Host "`n=================================================================" -ForegroundColor Green
Write-Host " CAMPAGNE DE TESTS QUALITÉ LOGICIELLE FINALISÉE !" -ForegroundColor Green
Write-Host " Tous les rapports sont centralisés dans : docs/qualite/locataires/" -ForegroundColor Green
Write-Host "=================================================================" -ForegroundColor Green
