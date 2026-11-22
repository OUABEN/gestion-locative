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

## Checkstyle

Lancer depuis la racine du dépôt avec Java 21 :

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File `
  ".\qualite-logicielle\quittances\run-checkstyle.ps1" `
  -JavaHome "C:\Program Files\Java\jdk-21.0.12.1"
```

Le contrôle examine les sources Java de production et de test, exclut les
ressources non Java et les sources générées, et n’exécute pas la suite Maven.
`UnusedImports` et `RedundantImport` sont de gravité `error`; les autres règles
sont des avertissements de style. Les violations, y compris celles de gravité
`error`, ne font pas échouer la commande (`failOnViolation=false`,
`failsOnError=false`); une erreur Maven d’exécution reste fatale au runner. Le
contrôle n’est pas lié au cycle `test`/`verify`, donc les violations observées
ne bloquent pas les tests existants.

Les règles sont définies dans `checkstyle.xml` : tabulations, lignes de plus de
120 caractères (hors packages/imports/URL), imports, noms Java, accolades,
espacement, lignes vides, déclarations multiples, ordre des modificateurs et
retours à la ligne des opérateurs. Il n’y a pas de règle de documentation
Javadoc ni de suppression de violation.

Les sorties sont `docs/qualite/quittances/checkstyle/checkstyle-result.xml` et
`docs/qualite/quittances/checkstyle/checkstyle.html`. Le runner remplace les
chemins absolus de l’ordinateur par des chemins relatifs au dépôt dans le XML;
les violations et leur position ne sont pas modifiées.

## SonarQube

`sonar-project.properties` cible exclusivement le backend Java Quittances et
déclare les rapports JaCoCo XML et Surefire. La référence Biens décrit un projet
TypeScript/React et un Compose dédié; ni sa configuration ni ses scripts ne
sont utilisés ici.

SonarQube n’est lancé que si un serveur autorisé est accessible et si le CLI
`sonar-scanner` est installé. Depuis la racine, après compilation et génération
des rapports JUnit/Surefire requis (relancer au besoin la procédure JUnit de
cette page, qui crée son propre cluster temporaire) :

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:SONAR_HOST_URL = "http://127.0.0.1:9000"
# Définir SONAR_TOKEN dans l’environnement sans l’écrire dans un fichier.
sonar-scanner -Dproject.settings=qualite-logicielle/quittances/sonar-project.properties
```

Ne pas démarrer Docker ou installer un serveur pour cette phase sans
autorisation. Sans serveur et scanner disponibles, la configuration seule ne
constitue pas une analyse ni un résultat Sonar.

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
