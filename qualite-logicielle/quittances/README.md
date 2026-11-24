# Qualite logicielle - Quittances

## Dernier bilan du 24 novembre 2026

| Outil | Resultat |
|---|---|
| JUnit | 317/317 tests reussis |
| Vitest | 16/16 tests reussis |
| Total Java et frontend | 333/333 tests reussis |
| JaCoCo - Instructions | 82,28 % |
| JaCoCo - Lignes | 83,45 % |
| JaCoCo - Branches | 63,28 % |
| Istanbul - Instructions | 62,21 % |
| Istanbul - Lignes | 65,97 % |
| Istanbul - Branches | 54,10 % |
| Istanbul - Fonctions | 39,72 % |
| Checkstyle | 0 erreur, 1641 avertissements |
| SonarQube Quality Gate | Passed |
| SonarQube - Couverture du nouveau code | 81,1 % |
| SonarQube - Duplication du nouveau code | 0,94 % |
| SonarQube - Nouveaux problemes | 0 |
| SonarQube - Couverture globale | 74,1 % |
| SonarQube - Duplication globale | 1,7 % |
| SonarQube - Vulnerabilites globales | 1 |

## Rapports disponibles

- [JUnit](rapports/junit/RESULTATS.md)
- [JaCoCo](rapports/jacoco/index.html)
- [Checkstyle](rapports/checkstyle/checkstyle.html)
- [Vitest et Istanbul](rapports/istanbul/RESULTATS.md)
- [Couverture frontend HTML](rapports/istanbul/index.html)
- [SonarQube](rapports/sonarqube/RESULTATS.md)
- [Mesures SonarQube](rapports/sonarqube/mesures-sonarqube-finalisation.json)

## Etat de l'integration

Le nouveau contrat de recuperation de l'identite du bailleur
est implemente et valide par les tests automatises.

La validation reelle du gestionnaire reste bloquee par
la validation des UUID de demonstration dans le service Biens.

La validation positive du locataire rattache reste a effectuer.

Les anciennes campagnes Postman, JMeter, Cypress, Snyk et ZAP
sont conservees comme preuves historiques.

La CI de cette nouvelle version reste a verifier apres publication.

---

## Documentation et campagnes precedentes

# Qualité logicielle — Quittances

<!-- BILAN-20261124 -->

## Bilan du 24 novembre 2026

Branche : `integration-reelle-quittances`.

Pull Request : https://github.com/OUABEN/gestion-locative/pull/24

| Outil | Résultat |
|---|---|
| JUnit / Maven | 306 tests réussis sur 306 |
| JaCoCo - Instructions | 82,13 % |
| JaCoCo - Lignes | 83,32 % |
| JaCoCo - Branches | 62,89 % |
| Checkstyle | 0 erreur, 1 641 avertissements |
| SonarQube Quality Gate | Passed |
| SonarQube - Couverture du nouveau code | 80,6 % |
| SonarQube - Duplication du nouveau code | 0,97 % |
| GitHub Actions | 7 vérifications réussies sur 7 |

La dernière campagne locale utilise Java 21, Maven et une base
PostgreSQL 17 dédiée aux tests, exécutée sous Docker.

Commande Maven utilisée depuis la racine du dépôt :

```powershell
mvn -f ".\backend\backend_gestion_quittances\pom.xml" clean verify checkstyle:checkstyle
```

Cette commande suppose que la base de tests isolée, les variables
de connexion et les clés JWT locales sont correctement configurées.
Elle ne doit jamais être dirigée vers une base de production.

La procédure historique utilisant un cluster PostgreSQL temporaire
reste documentée ci-dessous. Sa compatibilité avec les dernières
modifications doit être vérifiée avant une nouvelle utilisation.

## Intégration continue

Le workflow `.github/workflows/ci.yml` utilise Java 21 pour
Quittances et conserve Java 17 pour les autres services concernés.

Pour Quittances, il crée une base PostgreSQL dédiée, active Flyway,
génère des clés RSA temporaires et exécute `mvn -B test`.

Les clés privées ne sont pas versionnées.

La CI a été validée sur le commit `d2154d5`.

## Consultation des rapports

- [JUnit](rapports/junit/RESULTATS.md)
- [JaCoCo](rapports/jacoco/index.html)
- [Checkstyle](rapports/checkstyle/checkstyle.html)
- [SonarQube](rapports/sonarqube/RESULTATS.md)
- [Bilan général](rapports/bilan/RAPPORT_PROVISOIRE.md)

Les rapports JaCoCo et Checkstyle actualisés ont été publiés
dans le commit `c097e76`.

Les campagnes Postman, JMeter, Cypress, Snyk et ZAP antérieures
restent des résultats historiques. Elles ne constituent pas
une nouvelle validation de la version du 24 novembre.

---

## Historique et documentation anterieure

Les resultats ci-dessous sont conserves comme historique.
Les mesures anciennes ne remplacent pas le bilan du 24 novembre 2026.

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

Les sorties sont `qualite-logicielle/quittances/rapports/checkstyle/checkstyle-result.xml` et
`qualite-logicielle/quittances/rapports/checkstyle/checkstyle.html`. Le runner remplace les
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
rapports HTML et XML dans `qualite-logicielle/quittances/rapports/jacoco/`; une nouvelle
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
