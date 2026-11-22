# Tests API Postman/Newman — Quittances

## Contenu et perimetre

- `Quittances-api.postman_collection.json` est une collection dediee, derivee
  des routes de `backend/backend_gestion_quittances/postman/DarLoc.postman_collection.json`.
- `Quittances-test.postman_environment.json` ne contient ni JWT, ni secret, ni
  identifiant de document. Les deux JWT requis sont des variables Postman de
  type secret laissées vides.
- Les neuf requetes sont exclusivement des `GET`. Aucune creation, emission,
  annulation, archive ou operation Mailpit n'est executee par cette collection.

Les scenarios configurent 19 assertions Postman (`pm.test`) :

| Cas | Assertions |
| --- | ---: |
| Etat public du service et structure JSON | 3 |
| Identite sans JWT : 401 | 1 |
| Liste sans JWT : 401 | 1 |
| Liste avec JWT mal forme : 401 | 1 |
| Identite avec JWT BAILLEUR : 200 et contrat JSON | 3 |
| Liste avec role GESTIONNAIRE : 403 | 1 |
| Consultation du document du bailleur : 200 et contrat JSON | 3 |
| Consultation d'un document d'un autre bailleur : 404 et ProblemDetail | 2 |
| Liste du bailleur : contrat JSON, inclusion du document propre et exclusion du tiers | 4 |

Les scenarios demandant un jeton valide ou des documents de test sont ignores
si leurs variables sont vides. Les valeurs attendues reposent sur les DTO et
les controles d'acces existants; les requetes ne modifient pas les donnees.

## Reproduction

Newman est une dependance locale. Depuis ce dossier, l'installation
reproductible est :

```powershell
npm.cmd install
```

Depuis la racine du depot, le runner provisionne un cluster PostgreSQL
temporaire, demarre l'application sur le port 18084, cree deux documents
synthetiques, signe deux JWT avec une cle aleatoire en memoire, execute Newman,
assainit les rapports et nettoie les ressources temporaires :

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File `
  ".\qualite-logicielle\quittances\postman\run-postman.ps1" `
  -JavaHome "C:\Program Files\Java\jdk-21.0.12.1" `
  -PostgresBin "C:\Program Files\PostgreSQL\18\bin"
```

Le runner exige que le port 18084 soit libre et refuse de s'y connecter s'il
est deja occupe. Il ne lance pas Docker et ne contacte pas les services
externes; les modes de leurs adaptateurs sont forces a `SIMULATION`.

Le rapport JSON est `docs/qualite/quittances/postman/newman-report.json`.
`newman-report.html` est une synthese tabulaire generee a partir des memes
resultats, sans corps HTTP ni jeton. Les rapports ne doivent contenir ni JWT,
ni secret, ni donnee personnelle. Ne jamais utiliser l'environnement Postman
existant qui cible `127.0.0.1:8084`; l'isolation de sa base n'est pas etablie.

## Resultats mesures le 22 novembre 2026

- Newman 6.2.3 local; Node.js 24.21.0; npm 11.19.0.
- Java 21.0.12.1, Maven 3.9.16 et PostgreSQL 18.6 pour le runner.
- 9 requetes executees, 0 echec HTTP d'assertion.
- 19 assertions configurees et executees : 19 reussies, 0 echouee, 0 ignoree.
- Statuts observes : 3 x 401, 4 x 200, 1 x 403 et 1 x 404.
- Les deux documents et les identites sont synthetiques. Le test de liste
  confirme que le BAILLEUR voit son document et pas celui du tiers.
- Le cluster, l'application, les fixtures, la cle JWT et les jetons temporaires
  ont ete arretes/supprimes apres l'execution. Seuls les rapports assainis sont
  conserves.
- Aucun fournisseur reel Biens, Locataires ou Paiements n'a ete contacte.
- Newman a emis l'avertissement Node non bloquant `DEP0176`; l'installation
  npm a aussi signale des dependances transitives depreciees.
