# Cypress - Quittances (phase 5)

Les tests ouvrent le frontend React compile localement par Vite Preview sur
`127.0.0.1:13003`. Les requetes `/api/**` sont interceptees avec `cy.intercept` ;
aucun appel n'est adresse aux API ou bases de donnees reelles.

## Execution

Prerequis : Node.js, npm, dependances installees dans `frontend/node_modules`,
Cypress 16.3.0 installe dans ce dossier QA (`npm install` ici si necessaire).

Depuis la racine du depot :

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File '.\qualite-logicielle\quittances\e2e-cypress\run-cypress.ps1'
```

Le script construit React hors du depot, demarre un serveur temporaire sur 13003,
lance le seul fichier `quittances.cy.js`, lit le rapport JUnit de 7 cas, puis
arrete uniquement les processus qu'il a crees. Il ne modifie pas les bases,
les services Docker, ni le code des autres modules.

## Perimetre des sept scenarios

1. Redirection vers le login sans session.
2. Liste et indicateurs vides pour un bailleur.
3. Transmission des filtres de liste.
4. Rejet d'un role GESTIONNAIRE non reconnu.
5. Interdiction du formulaire au LOCATAIRE.
6. Rejet d'une periode inversee, sans POST.
7. Preparation d'un brouillon avec POST simule et navigation vers son detail.

Le JWT est synthetique et n'est pas signe. L'execution ne prouve pas une
integration complete avec le backend ; l'integration HTTP et la charge sont
evaluees separement avec Newman et JMeter dans l'environnement isole de QA.

Les rapports se trouvent dans `docs/qualite/quittances/cypress/`.
Ne conserver dans Git que le rapport JUnit final et les captures synthetiques
utiles, pas les diagnostics ou executions echouees.
