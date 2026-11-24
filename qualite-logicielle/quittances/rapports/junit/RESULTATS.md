# Tests JUnit — Quittances

<!-- BILAN-20261124 -->

## Derniere campagne du 24 novembre 2026

| Indicateur | Résultat |
|---|---:|
| Tests exécutés | 317 |
| Tests réussis | 317 |
| Échecs | 0 |
| Erreurs | 0 |
| Tests ignorés | 0 |
| Fichiers sources de test Java | 47 |
| Résultat Maven | BUILD SUCCESS |

Environnement : Java 21, Maven et PostgreSQL 17 sous Docker,
avec une base dédiée nommée `quittances_automatisation_test`.

Commande exécutée :

```powershell
mvn -f ".\backend\backend_gestion_quittances\pom.xml" clean verify checkstyle:checkstyle
```

Les tests couvrent notamment les calculs, les règles métier,
la sécurité JWT, les autorisations, les contrôleurs,
les clients HTTP, la génération PDF, la signature,
la consultation et la gestion des quittances.

Les tests d'intégration Spring utilisent une base isolée.
Les fournisseurs simulés ne constituent pas une preuve
d'interopérabilité avec les services réels du groupe.


### Validation du nouveau contrat Biens

- Endpoint consomme : GET /api/users/{id}.
- 11 nouveaux cas de test automatises.
- Tests du client collectif : 32/32 reussis.
- Campagne Java complete : 317/317 reussis.
- Aucun echec, aucune erreur, aucun test ignore.
- Relais du JWT du gestionnaire verifie.
- Identite du proprietaire et coherence du contrat controlees.
- Reponses invalides et erreurs HTTP externes testees.

La validation reelle du parcours gestionnaire reste bloquee
par le rejet des UUID de demonstration cote Biens (HTTP 400).

### Couverture JaCoCo actualisee

| Indicateur | Couverts | Total | Couverture |
|---|---:|---:|---:|
| Instructions | 12136 | 14750 | 82,28 % |
| Branches | 1170 | 1849 | 63,28 % |
| Lignes | 2461 | 2949 | 83,45 % |
| Complexite | 736 | 1437 | 51,22 % |
| Methodes | 420 | 506 | 83,00 % |
| Classes | 148 | 155 | 95,48 % |

### Checkstyle

- Fichiers analyses : 163.
- Erreurs : 0.
- Avertissements : 1641.

### Frontend Quittances

- Vitest : 16/16 tests reussis.
- Istanbul : 65,97 % de couverture des lignes.
- Total Java et frontend : 333/333 tests reussis
  sur les deux campagnes.

## GitHub Actions

Le job Backend quittances a été validé après adaptation
du workflow à Java 21, Flyway et aux clés RSA de test.

Les sept vérifications de la CI collective ont réussi
sur le commit `d2154d5`.

Sources des tests :
`backend/backend_gestion_quittances/src/test/java/`.

Rapports Surefire locaux :
`backend/backend_gestion_quittances/target/surefire-reports/`.

Les rapports Surefire bruts ne sont pas publiés automatiquement,
afin d'éviter la diffusion de paramètres propres au poste.

---

## Historique et documentation anterieure

Les resultats ci-dessous sont conserves comme historique.
Les mesures anciennes ne remplacent pas le bilan du 24 novembre 2026.

# Tests JUnit â€” Quittances

- Campagne complete realisee apres les correctifs des records et du SQL : **180 tests reussis, 0 echec, 0 erreur, 0 ignore**.
- Apres l'ajustement de `SuiviAjoutsTest.java` : **3 tests cibles reussis**.
- La campagne complete reste a rejouer apres ce dernier ajustement pour figer une preuve globale definitive.
- Sources JUnit : `backend/backend_gestion_quittances/src/test/java/`.
- Rapports Surefire locaux : `backend/backend_gestion_quittances/target/surefire-reports/`.
- Les tests necessitant une base utilisent le runner `qualite-logicielle/quittances/run-tests.ps1` et un PostgreSQL temporaire isole.

Les XML Surefire bruts ne sont pas versionnes automatiquement car ils peuvent contenir des chemins et parametres propres au poste. Avant publication, verifier et assainir tout rapport exporte.