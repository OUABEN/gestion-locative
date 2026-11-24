# SonarQube - Quittances

## Derniere analyse du 24 novembre 2026

| Indicateur | Resultat |
|---|---:|
| Quality Gate | Passed (OK) |
| Couverture du nouveau code | 81,1 % |
| Duplication du nouveau code | 0,94 % |
| Nouveaux problemes | 0 |
| Problemes acceptes | 0 |
| Security Hotspots du nouveau code | 0 |
| Nouvelles lignes a couvrir | 904 |
| Couverture globale | 74,1 % |
| Couverture globale des lignes | 80,7 % |
| Couverture globale des branches | 63,3 % |
| Duplication globale | 1,7 % |
| Lignes de code | 7450 |
| Bugs | 0 |
| Code smells | 0 |
| Vulnerabilites | 1 |
| Note de securite | D |

L'analyse porte exclusivement sur le backend Java Quittances.

Elle utilise les rapports de la campagne des 317 tests Java,
tous reussis sans echec, erreur ni test ignore.

Les trois conditions du Quality Gate sont respectees.
Le code de retour Maven est 0.
La duree de la commande est de 22,600 secondes.

## Limites

Une vulnerabilite reste ouverte dans le code global.
Son identite exacte reste a confirmer dans SonarQube.
Le Quality Gate ne prouve pas l'absence de vulnerabilites.

SonarQube Community dispose d'une analyse de securite limitee.
Le risque residuel JAXB signale par Snyk reste distinct.

Preuve machine : `mesures-sonarqube-finalisation.json`.

---

## Campagnes precedentes

# SonarQube — Quittances

<!-- BILAN-20261124 -->

## Analyse du 24 novembre 2026

| Mesure | Résultat |
|---|---|
| Quality Gate | Passed |
| Couverture du nouveau code | 80,6 % |
| Duplications du nouveau code | 0,97 % |
| Nouveaux problèmes | 0 |
| Problèmes acceptés affichés | 0 |
| Security Hotspots affichés sur le nouveau code | 0 |
| Nouvelles lignes à couvrir | 887 |

L'analyse porte exclusivement sur le backend Java Quittances.

Elle utilise les classes Java compilées, les rapports JUnit
et le rapport XML JaCoCo de la campagne des 306 tests.

Le Quality Gate est validé selon les conditions configurées
dans SonarQube.

Les mesures portant sur le nouveau code ne sont pas directement
comparables aux anciennes mesures globales du projet.

## Limites de l'analyse

Des avertissements relatifs aux informations Git blame et
au rapprochement de deux fichiers sources avec JaCoCo ont
été signalés pendant l'analyse.

L'absence de nouveaux problèmes ne garantit pas l'absence
de vulnérabilités dans l'ensemble du projet.

L'ancien avertissement CSRF et le risque résiduel JAXB
restent documentés dans l'historique. Leur résolution
n'est pas déduite du seul passage du Quality Gate.

Le fichier `mesures-sonarqube.json` antérieur est conservé
comme preuve de la campagne précédente.

---

## Historique et documentation anterieure

Les resultats ci-dessous sont conserves comme historique.
Les mesures anciennes ne remplacent pas le bilan du 24 novembre 2026.

# SonarQube â€” Quittances

Analyse finale communiquee le 23 novembre 2026, a l'issue des correctifs locaux.

| Mesure | Resultat |
|---|---|
| Quality Gate | Passed (conditions configurees) |
| Coverage | 66,3 % |
| Duplicated lines density | 9,6 % |
| Lines of code | 6 734 |
| Reliability | A, 0 issue |
| Maintainability | A, 0 issue |
| Security | D, 1 issue HIGH ouverte |

**Risque ouvert :** avertissement SonarQube de configuration CSRF sur
`SecurityConfiguration.java` (exclusion `/api/**`). L'API utilise JWT Bearer
et des sessions STATELESS ; la justification exige de confirmer l'absence de
mecanisme d'authentification par cookie automatiquement transmis.
L'alerte n'est pas declaree corrigee ni automatiquement acceptee.

L'alerte Snyk residuelle JAXB est distincte de l'alerte SonarQube.
Les mesures machine sont dans `mesures-sonarqube.json`.