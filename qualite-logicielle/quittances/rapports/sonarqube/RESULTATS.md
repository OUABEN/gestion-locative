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