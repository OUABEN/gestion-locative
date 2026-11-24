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