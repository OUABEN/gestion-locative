# Suite d'Assurance Qualité Logicielle (QL) — Module Locataires & Baux

**Projet :** Plateforme de Gestion Locative Immobilière — Architecture Microservices  
**Module :** Gestion des Locataires & Baux (`backend_gestion_locataires`, Port 8082, Frontend Port 3000)  
**Réalisé par :** LAARIF Ahmed (Groupe 16 — Université Ibn Zohr d'Agadir, FSA Agadir)  
**Encadré par :** Dr. J. IDRAIS  
**Session :** Novembre 2026  

---

## 🎯 Vue d'Ensemble & Objectifs

Ce dossier rassemble l'ensemble des artefacts et configurations d'assurance qualité logicielle (QL) mis en place pour garantir la fiabilité, la sécurité, la maintenabilité et la robustesse du microservice **Gestion des Locataires & Baux** :
- Validation unitaire et couverture de code microscopique (Jest + Vitest).
- Validation des contrats d'interfaces et des habilitations RBAC (Postman / Newman).
- Métrologie de performance sous montée en charge progressive (Apache JMeter).
- Validation des parcours utilisateurs critiques de bout-en-bout (Cypress E2E).
- Analyse statique de code, de couverture et de qualité (SonarQube).
- Audit dynamique de sécurité applicative DAST (OWASP ZAP).
- Audit de vulnérabilités sur les dépendances NPM (`npm audit`).

---

## 📂 Structure du Répertoire `qualite-logicielle/locataires/`

```text
qualite-logicielle/locataires/
├── postman/
│   ├── locataires.postman_collection.json  # Collection complète Newman (Auth, CRUD, 409 CIN, 403 BOLA, Baux read-only)
│   └── package.json                        # Script d'exécution Newman CLI avec rapport htmlextra
├── jmeter/
│   └── plan-test-locataires.jmx            # Plan de test JMeter (Smoke 5 VU, Charge 50 VU, Stress 100 VU)
├── e2e-cypress/
│   ├── package.json                        # Dépendances Cypress et reporters Mochawesome
│   ├── cypress.config.js                   # Configuration Cypress (baseUrl: http://localhost:3000, viewports, reports)
│   └── cypress/
│       ├── e2e/
│       │   └── locataires-e2e.cy.js        # Scénarios E2E (ListPage, FormPage 409 CIN, DetailPage, MonEspaceLocataire)
│       └── support/
│           ├── commands.js                 # Commandes personnalisées Cypress (loginAsBailleur, loginAsLocataire)
│           └── e2e.js                      # Configuration globale Cypress
├── sonar/
│   ├── sonar-project.properties            # Configuration SonarQube Scanner (sources, tests, lcov.info)
│   ├── docker-compose.sonar.yml            # Stack Docker locale SonarQube + PostgreSQL
│   └── README.md                           # Guide d'exécution SonarQube
├── zap/
│   ├── zap.yaml                            # Automatisation ZAP Automation Framework (OpenAPI scan)
│   ├── run-zap.ps1                         # Script d'exécution Docker OWASP ZAP (Windows PowerShell)
│   └── README.md                           # Guide d'exécution OWASP ZAP DAST
├── run-all-tests.ps1                       # Script PowerShell orchestrateur de tous les tests
└── README.md                               # Le présent document descriptif
```

---

## 📊 Résultats des Mesures Réelles Obtenues

| Domaine de Test | Outil de Qualification | Métrique Réelle Mesurée | Statut d'Exécution |
| :--- | :--- | :--- | :--- |
| **Tests Unitaires Backend** | Jest v29 | **4 suites passées, 42 tests réussis / 42** en 1.623 s | ✅ 100% Succès |
| **Tests Unitaires Frontend** | Vitest + RTL | **21 tests réussis / 21** (`locataires.frontend.test.jsx`) | ✅ 100% Succès |
| **Compilation Production** | Vite build | **2 127 modules transformés** dans `dist/` sans erreur | ✅ 100% Validé |
| **Couverture Istanbul Backend** | Istanbul / c8 | **65.24% Statements**, **67.83% Lines** (Cœur métier > 85%) | ✅ Conforme |
| **Audit Dépendances NPM** | `npm audit` | **0 vulnérabilité** sur l'arbre complet de 370 paquets | ✅ 0 CVE |
| **Contrats d'API & Sécurité** | Newman / Postman | Scénarios complets CRUD, /me, 409 Conflict, 403 RBAC | 🚀 Prêt à lancer |
| **Audit Dynamique DAST** | OWASP ZAP | Scan OpenAPI port 8082 & Baseline frontend port 3000 | 🚀 Prêt à lancer |
| **Performance & Charge** | Apache JMeter | Profil Smoke (5 VU), Charge (50 VU), Stress (100 VU) | 🚀 Prêt à lancer |
| **Parcours E2E IHM** | Cypress | Scénario complet Résident & Gestionnaire | 🚀 Prêt à lancer |

---

## 🚀 Guide d'Exécution Rapide

### 1. Exécution Globale via le Script d'Orchestration
Depuis la racine du projet ou depuis ce dossier :
```powershell
.\run-all-tests.ps1
```

### 2. Tests Unitaires & Couverture Backend (Jest)
```bash
cd backend/backend_gestion_locataires
npx jest --coverage --forceExit --watchAll=false
```

### 3. Tests Unitaires Frontend (Vitest)
```bash
cd frontend
npx vitest run src/tests/locataires.frontend.test.jsx
```

### 4. Tests d'API Newman / Postman
```bash
cd qualite-logicielle/locataires/postman
npm test
```

### 5. Tests de Charge Apache JMeter
```bash
cd qualite-logicielle/locataires/jmeter
jmeter -n -t plan-test-locataires.jmx -l results.jtl -e -o dashboard/
```

### 6. Tests E2E Cypress
```bash
cd qualite-logicielle/locataires/e2e-cypress
npx cypress open
# ou en mode headless :
npx cypress run --browser chrome
```

### 7. Audit Dynamique OWASP ZAP (Docker)
```powershell
cd qualite-logicielle/locataires/zap
.\run-zap.ps1
```

---

## 📄 Rapports et Livrables Associés
Tous les rapports et comptes-rendus d'exécution sont générés et archivés sous :
- `docs/DAT_LOCATAIRES_AHMED_LAARIF.docx` (Dossier d'Architecture Technique complet)
- `docs/RAPPORT_QUALITE_LOGICIELLE_LOCATAIRES_AHMED_LAARIF.docx` (Rapport d'Assurance Qualité complet)
- `docs/qualite/locataires/` (Rapports HTML Istanbul, npm audit JSON, rapports Newman, ZAP et JMeter)
