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

## Reproduction

La procédure complète et ses prérequis sont décrits dans
`qualite-logicielle/quittances/README.md`. Elle ne démarre pas Docker, n’utilise
pas de base existante et remplace uniquement le sous-dossier généré
`docs/qualite/quittances/jacoco/`.
