# Tests JUnit â€” Quittances

- Campagne complete realisee apres les correctifs des records et du SQL : **180 tests reussis, 0 echec, 0 erreur, 0 ignore**.
- Apres l'ajustement de `SuiviAjoutsTest.java` : **3 tests cibles reussis**.
- La campagne complete reste a rejouer apres ce dernier ajustement pour figer une preuve globale definitive.
- Sources JUnit : `backend/backend_gestion_quittances/src/test/java/`.
- Rapports Surefire locaux : `backend/backend_gestion_quittances/target/surefire-reports/`.
- Les tests necessitant une base utilisent le runner `qualite-logicielle/quittances/run-tests.ps1` et un PostgreSQL temporaire isole.

Les XML Surefire bruts ne sont pas versionnes automatiquement car ils peuvent contenir des chemins et parametres propres au poste. Avant publication, verifier et assainir tout rapport exporte.