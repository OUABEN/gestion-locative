# Qualité logicielle — Quittances

Cette procédure exécute les tests JUnit du backend Quittances sous Java 21
contre un cluster PostgreSQL temporaire et génère un rapport JaCoCo. Elle ne
démarre pas Docker et ne réutilise aucune base existante.

## Prérequis

- JDK 21
- Maven 3
- PostgreSQL installé, avec `initdb.exe`, `pg_ctl.exe` et `createdb.exe`

Le plugin JaCoCo est déclaré dans le `pom.xml`; Maven le télécharge si nécessaire.
Aucun scanner SonarQube ou outil Checkstyle n’est requis pour cette phase.

## Exécution reproductible

Depuis la racine du dépôt, dans PowerShell :

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File `
  ".\qualite-logicielle\quittances\run-tests.ps1" `
  -JavaHome "C:\Program Files\Java\jdk-21.0.12.1" `
  -PostgresBin "C:\Program Files\PostgreSQL\18\bin"
```

`-ExecutionPolicy Bypass` s’applique uniquement au processus PowerShell lancé par
cette commande; la stratégie système ou utilisateur n’est pas modifiée.

Le script choisit un port local libre, crée une base au nom suffixé `_test`,
force la configuration datasource Spring vers cette base sur `127.0.0.1` et
l’utilise uniquement pour `mvn clean verify`. Il ignore les éventuels overrides
datasource hérités du processus, puis restaure les variables après l’exécution.
Le test HTTP exige une variable de mot de passe non vide : le script lui fournit
un UUID éphémère, non affiché ni sauvegardé. PostgreSQL autorise uniquement les
connexions locales par confiance; cette valeur n’est pas utilisée pour
l’authentification. Le script arrête et supprime le cluster qu’il a créé; si
PostgreSQL ne peut pas être arrêté, il conserve ses données pour éviter de
supprimer le dossier d’un serveur encore actif.

Les rapports Surefire/JUnit restent dans
`backend/backend_gestion_quittances/target/surefire-reports/`. JaCoCo écrit les
rapports HTML et XML dans `docs/qualite/quittances/jacoco/`; une nouvelle
exécution remplace uniquement ce dossier généré.

## Périmètre des tests

Les tests comprennent des tests unitaires de calcul, sécurité, clients HTTP,
contrôleurs et stockage, ainsi que des tests Spring intégrés. Les tests qui
démarrent le contexte Spring ou l’application HTTP utilisent la base temporaire.
`ParcoursQuittanceHttpIntegrationTest` exerce les endpoints Quittances sur une
base isolée et utilise des réponses de fournisseurs simulées dans le processus
de test; il ne valide pas l’intégration réelle aux autres modules.

Ne pas exécuter le script global de `qualite-logicielle/biens` pour cette phase :
il démarre sa propre stack Docker.
