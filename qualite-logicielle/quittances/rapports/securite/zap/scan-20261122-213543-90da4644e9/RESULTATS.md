# Phase 6 - OWASP ZAP Baseline (frontend statique isole)

- Date : 2026-11-22 22:12:48
- Commit local : 4e2246a
- Port de production 8084 : non utilise
- Reseau Docker : interne, aucune publication de port
- Cible : http://qa-quittances-ui/ (copie statique dans nginx de test)
- Methode : ZAP Baseline / exploration et analyse passive
- Code retour ZAP : 2 (0=OK, 1=FAIL, 2=avertissements, 3=erreur)
- Alertes par regle : eleve=0 ; moyen=2 ; faible=2 ; information=1

## Limites
Le scan ne teste ni le backend Spring Boot, ni les routes authentifiees, ni une base de donnees.
Les alertes de configuration HTTP peuvent concerner nginx de test et non les serveurs reels.
Verifier chaque alerte avant toute conclusion de securite.

## Rapports
- rapport-zap.html
- rapport-zap.json
- rapport-zap.md
