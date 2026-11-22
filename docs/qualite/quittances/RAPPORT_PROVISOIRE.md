# Synthèse provisoire — Qualité logicielle Quittances

## Périmètre et environnement

Mesure effectuée le 22 novembre 2026 sur `akram-jwt-commun-quittances`.
Le backend a été construit avec Maven 3.9.16 et JDK 21.0.12.1. Les tests
d’intégration ont utilisé PostgreSQL 18.6 dans un cluster temporaire local
initialisé par `run-tests.ps1`, sur une base nommée
`quittances_qualite_90d8a0ef_test`. Le serveur a été arrêté et le dossier de
données temporaire supprimé après l’exécution.

La commande exécutée est :

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File `
  ".\qualite-logicielle\quittances\run-tests.ps1" `
  -JavaHome "C:\Program Files\Java\jdk-21.0.12.1" `
  -PostgresBin "C:\Program Files\PostgreSQL\18\bin"
```

Le contournement de la stratégie d’exécution est limité au processus enfant
PowerShell; aucune stratégie système/utilisateur n’a été changée. La procédure
exécute `mvn clean verify`, qui inclut les phases de tests et de package.

## JUnit / Surefire

| Mesure | Résultat |
| --- | ---: |
| Fichiers de rapports Surefire | 30 |
| Tests exécutés | 178 |
| Réussis | 178 |
| Échecs | 0 |
| Erreurs | 0 |
| Ignorés | 0 |
| Résultat Maven | `BUILD SUCCESS` |

Le dépôt contient 30 fichiers de test Java, dont 12 classes annotées
`@SpringBootTest`. Le parcours `ParcoursQuittanceHttpIntegrationTest` a exécuté
3 tests HTTP sur la base temporaire; ses fournisseurs externes sont simulés,
donc ce test ne prouve pas l’interopérabilité réelle avec Biens ou Paiements.
Les rapports texte/XML Surefire sont conservés dans
`backend/backend_gestion_quittances/target/surefire-reports/`.

## JaCoCo

Le plugin Maven JaCoCo 0.8.14 a été ajouté au backend. Le rapport a été généré
pendant la phase `verify`; les pourcentages ci-dessous sont calculés depuis
`jacoco.xml` :

| Mesure | Couvert | Total | Couverture |
| --- | ---: | ---: | ---: |
| Instructions | 8 833 | 12 262 | 72,04 % |
| Branches | 731 | 1 328 | 55,05 % |
| Lignes | 1 875 | 2 496 | 75,12 % |

Les classes de production à examiner en priorité, selon la couverture de lignes,
sont notamment :

| Classe | Lignes couvertes |
| --- | ---: |
| `EmissionDemoPdfService` | 3/83 (3,61 %) |
| `QuittanceValidationExceptionHandler` | 1/16 (6,25 %) |
| `EnvoiMailpitTransactions` | 4/43 (9,30 %) |
| `EmissionDemoController` | 1/8 (12,50 %) |
| `EmissionDemoExceptionHandler` | 1/8 (12,50 %) |
| `EnvoiMailpitExceptionHandler` | 1/8 (12,50 %) |
| `QuittanceApercuExceptionHandler` | 3/22 (13,64 %) |
| `SuiviExceptionHandler` | 1/7 (14,29 %) |

La classe interne `EmissionDemoPdfService$MiseEnPage` a 0/90 lignes couvertes.
Ces chiffres décrivent l’état actuel; aucun test n’a été ajouté pour gonfler la
couverture.

Les rapports sont générés sous `docs/qualite/quittances/jacoco/` :

- `index.html` : rapport HTML consultable;
- `jacoco.xml` : données XML utilisées pour les métriques ci-dessus;
- `jacoco.csv` : export tabulaire.

Le rapport HTML complet contient 285 fichiers pour 3 141 680 octets (environ
3,14 Mo), incluant les pages source et ressources nécessaires à sa navigation.

## Outils et anomalies

- Disponibles et utilisés : JDK 21.0.12.1, Maven 3.9.16, PostgreSQL 18.6;
  JaCoCo 0.8.14 est récupéré comme plugin Maven.
- Le `java` par défaut du terminal reste OpenJDK 8.0.504; la procédure sélectionne
  explicitement Java 21 et vérifie la version Java de Maven avant de lancer les
  tests.
- Aucune commande Checkstyle ou SonarScanner n’est installée/configurée pour
  Quittances à cette phase.
- La sortie contient l’avertissement JDK/Byte Buddy sur le chargement dynamique
  d’un agent et des avertissements PostgreSQL SQLState `23505` sur les
  contraintes d’unicité; malgré cela, les 178 tests passent sans erreur.
- Aucun JWT complet, clé privée, chemin utilisateur Windows ou valeur littérale
  de secret/mot de passe n’a été trouvé dans les rapports générés. La seule
  adresse ressemblant à un courriel utilise le domaine réservé `.test`; aucune
  adresse personnelle n’a été trouvée. Les vues source HTML JaCoCo montrent
  des noms de propriétés de configuration, pas leurs valeurs.
- Aucun rapport Word ni capture d’écran n’a été produit.

## Phase 2 — Checkstyle et SonarQube

### Checkstyle

| Élément | Résultat |
| --- | --- |
| Maven Checkstyle Plugin | 3.6.0 |
| Checkstyle | 10.23.1 |
| JVM/Maven | JDK 21.0.12.1 / Maven 3.9.16 |
| Sources analysées | 68 fichiers Java : 47 production, 21 tests |
| Tests Maven exécutés par cette analyse | Aucun |
| Total de violations | 1 604 |
| Gravité `error` | 0 |
| Gravité `warning` | 1 604 |
| Résultat de la commande | `BUILD SUCCESS` |

Les 1 604 violations se répartissent en 773 sur les sources de production et
831 sur les sources de test. L’import inutilisé précédemment signalé dans
`ControlePrealableEmissionServiceTest.java` a été retiré; aucune violation de
gravité `error` ne subsiste.

Commande exécutée depuis la racine :

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File `
  ".\qualite-logicielle\quittances\run-checkstyle.ps1" `
  -JavaHome "C:\Program Files\Java\jdk-21.0.12.1"
```

Répartition par règle (même catégorie que dans le XML, suffixe `Check` retiré) :

| Règle | Violations |
| --- | ---: |
| `WhitespaceAfter` | 646 |
| `WhitespaceAround` | 395 |
| `EmptyLineSeparator` | 282 |
| `LineLength` | 125 |
| `NeedBraces` | 54 |
| `OneStatementPerLine` | 51 |
| `AvoidStarImport` | 40 |
| `MultipleVariableDeclarations` | 9 |
| `MethodName` | 2 |

Les violations restantes d’espacement, longueur de ligne, accolades et imports
génériques sont des écarts de style/maintenabilité; aucun fichier métier n’a
été retouché pour réduire le total. Les fichiers concernés et leurs positions
figurent dans les rapports; les plus chargés sont
`SuiviScenarios.java` (380), `EnvoiMailpitTransactions.java` (255),
`EnvoiMailpitServiceTest.java` (148) et `EmissionDemoServiceTest.java` (147).

Rapports conservés dans `docs/qualite/quittances/checkstyle/` :

- `checkstyle-result.xml` : 68 fichiers analysés et violations détaillées;
- `checkstyle.html` et ses ressources : rapport lisible.

Le dossier contient 39 fichiers pour 1 276 895 octets. Les chemins absolus du
poste sont normalisés en chemins relatifs dans le XML; cette normalisation ne
change pas les règles, gravités, positions ni messages. L’HTML et le XML ne
contiennent aucun chemin utilisateur. Le rapport Maven affiche un avertissement
non bloquant sur l’absence d’URL du projet parent.

La vérification n’est pas liée au cycle Maven `test`/`verify`, et
`failOnViolation=false`/`failsOnError=false` laissent observer les violations
sans bloquer les tests ou le build habituel. Les erreurs d’exécution Maven
restent fatales au runner.

### SonarQube

Aucune analyse SonarQube n’a été effectuée : `sonar-scanner` n’est pas installé,
aucune installation n’a été trouvée dans le répertoire standard examiné et le
port de référence `127.0.0.1:9000` n’est pas accessible. Aucun serveur n’a été
démarré et aucun conteneur n’a été touché.

La configuration reproductible est
`qualite-logicielle/quittances/sonar-project.properties`; elle limite l’analyse
au Java du backend Quittances et référence JaCoCo XML et Surefire. Le scanner
nécessitera un serveur autorisé, le CLI installé, les classes compilées et les
rapports JUnit disponibles. Un éventuel jeton est fourni uniquement via
l’environnement et n’est pas enregistré dans les fichiers. La configuration
Sonar de Biens analyse TypeScript/React et son Compose/script n’a pas été
réutilisé ni exécuté.

## Reproduction

La procédure complète et ses prérequis sont décrits dans
`qualite-logicielle/quittances/README.md`. Elle ne démarre pas Docker, n’utilise
pas de base existante et remplace uniquement le sous-dossier généré
`docs/qualite/quittances/jacoco/`.

## Phase 3 — Postman / Newman

### Collection et périmètre

La collection dédiée est
`qualite-logicielle/quittances/postman/Quittances-api.postman_collection.json`.
Elle reprend les routes utiles de la collection existante du backend et
contient neuf requêtes `GET` : état du service, identité, liste, document du
bailleur, accès sans JWT, JWT mal formé, rôle GESTIONNAIRE et document d’un
autre bailleur. Aucune écriture ni émission n’est effectuée.

L’environnement associé est
`qualite-logicielle/quittances/postman/Quittances-test.postman_environment.json`.
Il pointe vers `http://127.0.0.1:18084`; les JWT et UUID y sont vides. Le
runner `qualite-logicielle/quittances/postman/run-postman.ps1` crée un
environnement temporaire privé, qui n’est pas conservé.

### Exécution et résultats

| Mesure | Résultat |
| --- | --- |
| Outil | Newman 6.2.3 local; Node.js 24.21.0; npm 11.19.0 |
| Environnement Java/Maven/PostgreSQL | Java 21.0.12.1; Maven 3.9.16; PostgreSQL 18.6 |
| Base | Cluster temporaire local, base suffixée `_test` |
| Requêtes | 9 exécutées; 0 en échec |
| Assertions configurées | 19 |
| Assertions exécutées | 19 |
| Réussies / échouées / ignorées | 19 / 0 / 0 |
| Statuts HTTP observés | 3 x 401, 4 x 200, 1 x 403, 1 x 404 |
| Résultat package backend | `BUILD SUCCESS` avec `-DskipTests` |

Les requêtes ont vérifié l’état public, l’identité sans et avec JWT BAILLEUR,
les refus sans JWT et avec JWT mal formé, le refus du rôle GESTIONNAIRE, la
consultation du document du bailleur, le 404 d’un document appartenant à un
autre bailleur et l’isolation de la liste. Le test de liste a confirmé
l’inclusion du document propre et l’exclusion du document tiers.

Le runner a généré en mémoire une clé HS256 aléatoire et les JWT synthétiques.
Il a inséré deux quittances fictives aux identifiants aléatoires dans la base
temporaire. Les valeurs d’authentification ont été retirées des rapports avant
leur conservation. Aucun secret, JWT, nom réel ou donnée personnelle n’est
conservé. Les adaptateurs externes étaient configurés en mode
`SIMULATION`; aucun service réel du groupe n’a été contacté.

Les rapports authentiques sont
`docs/qualite/quittances/postman/newman-report.json` et
`docs/qualite/quittances/postman/newman-report.html`. Le second est une
synthèse tabulaire générée à partir du résultat Newman assaini; il n’inclut
pas de corps HTTP. Les commandes et le runner reproductible sont décrits dans
`qualite-logicielle/quittances/postman/README.md`.

Après l’exécution, l’application Java a été arrêtée, PostgreSQL arrêté et le
répertoire temporaire supprimé. Le port 18084 est libre. Les conteneurs et
bases permanentes n’ont pas été utilisés ni modifiés.

Un premier essai de provisionnement s’est arrêté avant Newman parce que les
fixtures étaient insérées avant l’exécution de Flyway. Le runner a été corrigé
pour attendre le démarrage de Spring Boot et ses migrations avant l’insertion;
le cluster de cet essai a été arrêté et supprimé. L’exécution finale ci-dessus
est celle enregistrée dans les rapports.

Newman a émis l’avertissement Node non bloquant `DEP0176`; l’installation npm
a également signalé des dépendances transitives dépréciées. Aucun de ces
avertissements n’a fait échouer les assertions.
