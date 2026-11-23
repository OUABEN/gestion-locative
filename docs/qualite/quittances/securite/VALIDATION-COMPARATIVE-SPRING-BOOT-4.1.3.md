# Validation comparative - Spring Boot 4.1.2 et 4.1.3

**Module :** Quittances
**Date :** 23 novembre 2026
**Branche :** akram-jwt-commun-quittances
**Commit de reference :** 25535e7

## Objectif et methode

Verifier la compatibilite du module Quittances avec Spring Boot 4.1.3, sans modifier le projet ni utiliser les services reels. Comparaison avec Spring Boot 4.1.2 sur copies temporaires, en plusieurs executions JUnit distinctes.

Tests executes en conteneurs Docker sans port hote publie. Les classes qui necessitent PostgreSQL utilisent une base ephemere et un reseau Docker interne. Une cle publique JWT de test a ete creee sans utiliser les cles de production.

## Resultats JUnit (rapports XML verifies)

| Groupe | Spring Boot 4.1.2 | Spring Boot 4.1.3 |
|---|---:|---:|
| 4 classes unitaires | 23/23 | 23/23 |
| 5 classes unitaires supplementaires | 38/38 | 38/38 |
| 8 classes sans contexte DB | 50/50 | 50/50 |
| 4 classes Spring et PostgreSQL | 26/26 | 26/26 |
| 8 classes JPA / JDBC | 38/38 | 38/38 |
| Parcours HTTP complet | 3/3 | 3/3 |
| **Total** | **178/178** | **178/178** |

**Bilan :** 0 echec, 0 erreur, 0 ignore sur les deux versions. Les 30 classes de test comportant des tests JUnit ont ete validees. `SuiviScenarios.java` ne contient pas d'annotation de test.

## Comparaison securite (scan Snyk effectue precedemment)

| Version | Alertes Snyk Maven | Critiques | Elevees | Moyennes | Faibles |
|---|---:|---:|---:|---:|---:|
| 4.1.2 | 18 | 5 | 4 | 8 | 1 |
| 4.1.3 (POM temporaire) | 3 | 0 | 1 | 2 | 0 |

Les alertes restantes concernent `jaxb-core` (1 elevee) et `logback-classic` (2 moyennes). Le changement de version du parent Spring Boot est le seul changement teste dans le POM candidat.

## Sources locales des preuves

Rapports JUnit XML et journaux Maven conserves dans les repertoires temporaires suivants (sous `%TEMP%`) :
- `quittances-junit-isole-c781a2dc5abd` - 4 classes unitaires
- `quittances-junit-isole-83a14f63a002` - 5 classes unitaires supplementaires
- `quittances-junit-restants-sans-db-9b25b20f7199` - 8 classes sans contexte DB
- `quittances-postgres-isole-258945925d8b` - 4 classes Spring et PostgreSQL
- `quittances-8classes-postgres-isole-2a938b1609b8` - 8 classes JPA / JDBC
- `quittances-3http-postgres-isole-51c791a897b9` - Parcours HTTP complet

Rapports Snyk initiaux dans `docs/qualite/quittances/securite/snyk/`. Le scan candidat est conserve sous `%TEMP%/quittances-boot413-7176eba91b/snyk-boot413.json`.

## Limites et etat des changements

- Validation par groupes de classes, et non par une execution globale unique.
- Aucun environnement de production ou de preproduction teste.
- Les trois alertes Snyk restantes necessitent un examen ulterieur.
- Le vrai `pom.xml` du depot reste sur Spring Boot 4.1.2 au moment de cette synthese.
- Aucun commit ni push effectue durant cette validation ; `docs/integration/` preserve.

## Suite proposee

Apres relecture des preuves et du diff, envisager la mise a jour ciblee du parent Maven vers 4.1.3, puis renouveler les controles de compilation, de securite et d'integration sur un environnement isole.
