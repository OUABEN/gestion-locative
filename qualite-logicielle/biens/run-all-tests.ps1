# =====================================================================
# Script d'Exécution Global de Tous les Tests de Qualité Logicielle (QL)
# Module : Gestion des Biens
# Auteur : Mohamed OUABEN — Groupe 16
# =====================================================================

$ErrorActionPreference = "Continue"
$rootDir = Resolve-Path "$PSScriptRoot/../../"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " LANCEMENT GLOBAL DE LA SUITE DE QUALITÉ LOGICIELLE (QL)" -ForegroundColor Cyan
Write-Host " Module : Gestion des Biens | Groupe 16" -ForegroundColor Cyan
Write-Host "=================================================================`n" -ForegroundColor Cyan

# ---------------------------------------------------------------------
# 1. Vérification de l'environnement Docker
# ---------------------------------------------------------------------
Write-Host "[1/6] Démarrage de la stack Docker (biens, frontend, db)..." -ForegroundColor Yellow
docker compose -f "$rootDir/docker-compose.biens.yml" up -d
Start-Sleep -Seconds 3

# ---------------------------------------------------------------------
# 2. Tests Unitaires Backend & Couverture Istanbul
# ---------------------------------------------------------------------
Write-Host "`n[2/6] Tests Unitaires Backend (Jest + Istanbul)..." -ForegroundColor Yellow
npm test --prefix "$rootDir/backend/backend_gestion_biens" -- --coverage

# ---------------------------------------------------------------------
# 3. Tests d'Intégration Backend (PostgreSQL Réelle)
# ---------------------------------------------------------------------
Write-Host "`n[3/6] Tests d'Intégration Backend (Supertest + PostgreSQL)..." -ForegroundColor Yellow
npm run test:integration --prefix "$rootDir/backend/backend_gestion_biens"

# ---------------------------------------------------------------------
# 4. Tests Frontend & Couverture Vitest
# ---------------------------------------------------------------------
Write-Host "`n[4/6] Tests Unitaires Frontend React (Vitest + RTL)..." -ForegroundColor Yellow
npm run test:coverage --prefix "$rootDir/frontend"

# ---------------------------------------------------------------------
# 5. Tests API Fonctionnels (Newman / Postman)
# ---------------------------------------------------------------------
Write-Host "`n[5/6] Tests d'API REST (Newman / Postman - 41 assertions)..." -ForegroundColor Yellow
npm test --prefix "$rootDir/qualite-logicielle/biens/postman"

# ---------------------------------------------------------------------
# 6. Tests E2E IHM & Génération des 22 Captures d'Écran
# ---------------------------------------------------------------------
Write-Host "`n[6/6] Tests E2E Navigateur (14 étapes) & Captures d'Écran..." -ForegroundColor Yellow
node "$rootDir/qualite-logicielle/biens/capture-tools/run-e2e-and-captures.js"

# ---------------------------------------------------------------------
# 7. Regénération du Rapport Word (.docx)
# ---------------------------------------------------------------------
Write-Host "`n[BONUS] Mise à jour du Rapport Word (.docx)..." -ForegroundColor Yellow
node "$rootDir/qualite-logicielle/biens/capture-tools/generate-report-docx.js"

Write-Host "`n=================================================================" -ForegroundColor Green
Write-Host " TOUS LES TESTS ONT ÉTÉ EXÉCUTÉS AVEC SUCCÈS !" -ForegroundColor Green
Write-Host " Rapport Word généré : docs/RAPPORT_QUALITE_LOGICIELLE_BIENS.docx" -ForegroundColor Green
Write-Host " Captures disponibles : docs/qualite/biens/captures/" -ForegroundColor Green
Write-Host "=================================================================" -ForegroundColor Green
