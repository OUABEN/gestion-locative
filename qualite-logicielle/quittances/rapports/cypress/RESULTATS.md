# Resultats Cypress - Quittances (phase 5)

Execution locale : 2026-11-22 18:25:47

- Outil : Cypress 16.3.0, navigateur Electron.
- Frontend : React compile, serveur Vite Preview isole sur 127.0.0.1:13003.
- API : reponses simulees avec cy.intercept ; aucune requete au backend reel.
- Resultat : **7 tests, 7 reussites, 0 echec, 0 ignore**.
- Preuve : TEST-cypress-99dc2d7daeef7fa4b8e6253f7b12275b.xml.
- Port 13003 : libere apres le test.
- Correction d'application : affichage des erreurs de validation du formulaire de quittance.

Portee : ces tests valident les parcours UI avec des reponses API simulees, non
une validation de bout en bout des services reels.