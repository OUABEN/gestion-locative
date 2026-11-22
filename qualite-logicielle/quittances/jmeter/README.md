# Tests de performance JMeter - Quittances

JMeter 5.6.3, Java 21, backend Quittances sur `127.0.0.1:18084` avec une base PostgreSQL temporaire suffixee `_test`.

## Perimetre et execution

- Deux paliers successifs : 5 et 10 utilisateurs virtuels, 25 secondes chacun.
- Lecture seule : `GET /api/v1/systeme/etat` (HTTP 200 attendu) et `GET /api/v1/quittances` sans JWT (HTTP 401 attendu).
- Temporisation de 1 seconde avant chaque requete. Aucun test de PDF, signature ni emission officielle.
- Execution du 22 novembre 2026 via un lanceur temporaire connecte au runner Newman isole ; backend et PostgreSQL temporaires arretes apres les mesures.
- Ne jamais utiliser le port Docker 8084 ni sa base sur 5434. Ne pas lancer le runner JMeter seul sans preuve que 18084 pointe vers une instance Java de test et sa base `_test`.

## Resultats verifies

Rapport de reference : `docs/qualite/quittances/jmeter/execution-20261122-162107/`.

- 330 requetes, 330 reussies, 0 echec (0 %).
- Moyenne 5,6 ms ; minimum 2 ms ; maximum 114 ms.
- P90 8 ms, P95 10 ms, P99 17 ms.
- Debit moyen environ 6,6 requetes/seconde ; duree environ 50 secondes.
- Premier palier : 110 requetes ; deuxieme palier : 220 requetes.

La preuve brute est `resultats.jtl`. Le tableau de bord HTML est conserve en archive `rapport-html.zip` : extraire l'archive dans un dossier, puis ouvrir `index.html`.
Le journal `jmeter.log` et les tentatives precedentes ne sont pas publies.

Limite : ces performances concernent des endpoints legers, non les PDF, paiements, consultations authentifiees ou l'integration reelle des autres modules.