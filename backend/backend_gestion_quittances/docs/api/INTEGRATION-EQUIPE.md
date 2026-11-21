# Contrats à partager avec l'équipe

Le contrat OpenAPI fourni décrit les 20 opérations REST déjà présentes dans Quittances. Il n'est pas une déclaration d'intégration réussie avec les autres modules.
Les identifiants externes sont des chaînes opaques, non vides et limitées à 100 caractères. Les UUID des documents Quittances sont distincts de ces identifiants.

## API consommées — conventions provisoires

Toutes ces routes utilisent GET et JSON, sans corps de requête et sans accès SQL aux bases des collègues.

| Propriétaire | Route provisoire | Paramètres |
|---|---|---|
| Ayoub / Paiements | /api/v1/paiements/affectations | bailId, debut, fin |
| Ahmed / Locataires | /api/v1/locataires/{id} | id |
| Mohamed / Biens | /api/v1/biens/{id} | id |
| Responsable à confirmer / Baux | /api/v1/baux/{id}/situation | id, debut, fin |

Ces chemins doivent être confirmés par les collègues ; les URLs et chemins sont configurables. Le responsable des baux reste à décider. Aucun cinquième microservice obligatoire n'est ajouté.
Les transports Java actuels fixent 2 secondes pour la connexion et 5 secondes pour la lecture. Les redirections ne sont pas suivies.
En SIMULATION : endpoints locaux avec données fictives, sans jeton. En INTEGRATION : jeton Bearer de service configurable, jamais publié dans les contrats ni dans Git. Faire valider les scopes et les identités par l'équipe.
Une réponse HTTP d'erreur, une réponse illisible ou incohérente bloque la préparation ; les API de création renvoient alors une erreur adaptée, notamment 503. Une réponse externe 404 n'est pas assimilée à une preuve de paiement inexistant ou soldé.

## Réponses minimales illustratives

Données fictives, versionContrat "1".

Locataire :
```json
{"versionContrat":"1","id":"LOC-DEMO-001","nom":"Demonstration","prenom":"Locataire Un","email":"locataire.un@example.test","emailVerifie":false}
```

Bien :
```json
{"versionContrat":"1","id":"BIEN-DEMO-001","adresse":"Adresse fictive, Agadir","bailleurId":"BAILLEUR-TEST"}
```

Situation du bail :
```json
{"versionContrat":"1","bailId":"BAIL-DEMO-001","locataireId":"LOC-DEMO-001","bienId":"BIEN-DEMO-001","bailleurId":"BAILLEUR-TEST","nomBailleur":"Bailleur fictif","debutBail":"2026-01-01","finBail":"2026-12-31","periodeDebut":"2026-10-01","periodeFin":"2026-10-31","loyerDu":"3000.00","chargesDues":"200.00","devise":"MAD"}
```

Paiements (deux versements, avec loyer et charges séparés) :
```json
{"versionContrat":"1","complet":true,"bailId":"BAIL-DEMO-001","periodeDebut":"2026-10-01","periodeFin":"2026-10-31","affectations":[{"affectationId":"AFF-001","paiementId":"PAY-001","bailId":"BAIL-DEMO-001","periodeDebut":"2026-10-01","periodeFin":"2026-10-31","nature":"LOYER","montant":"1500.00","devise":"MAD","statut":"CONFIRME"},{"affectationId":"AFF-002","paiementId":"PAY-002","bailId":"BAIL-DEMO-001","periodeDebut":"2026-10-01","periodeFin":"2026-10-31","nature":"LOYER","montant":"1500.00","devise":"MAD","statut":"CONFIRME"},{"affectationId":"AFF-003","paiementId":"PAY-002","bailId":"BAIL-DEMO-001","periodeDebut":"2026-10-01","periodeFin":"2026-10-31","nature":"CHARGES","montant":"200.00","devise":"MAD","statut":"CONFIRME"}]}
```

`complet=true` signifie que la liste est complète pour la période, pas que le loyer est intégralement réglé. Les affectations doivent être fournies par Paiements : Quittances ne répartit pas un versement sur plusieurs mois par supposition.
Les montants externes des exemples sont des chaînes décimales acceptées par les DTO actuels ; le format numérique ou textuel définitif reste à harmoniser avec l'équipe.

## Authentification du consommateur de Quittances

Envoi du JWT dans `Authorization: Bearer <jeton>`. Vérification RS256, issuer, audience et dates selon la configuration existante.
Le contrat actuel utilise `roles` contenant BAILLEUR ou LOCATAIRE, et respectivement `bailleur_id` ou `locataire_id`. L'identité d'audit utilise sub et iss.
Les opérations de consultation utilisent un rôle métier unique. La création a un contrôle spécifique dans son controller ; harmoniser ce point avec le contrat partagé au moment de l'intégration.
Aucun endpoint d'authentification supplémentaire n'est créé dans ce module. Ne pas transmettre les clés privées ou des jetons réels avec la documentation.

## Points encore ouverts

- Endpoints et jeux de données des trois véritables services : à confirmer et tester ensemble.
- Propriétaire des baux, règles de périodes, évolutions de montants : à confirmer.
- Issuer/audience/claims/scopes communs : à harmoniser.
- Emails réels : utiliser l'adresse vérifiée du service Locataires et un SMTP configuré. Aujourd'hui, Mailpit reçoit une adresse fictive générée côté Quittances.
- Notifications de paiements annulés/remboursés et réconciliation : contrat événementiel et idempotence à définir, endpoints non présents à ce stade.
- Docker Compose commun et démarrage complet du service conteneurisé : à valider. Les modes locaux actuels JWT/signature/Mailpit sont volontairement limités à 127.0.0.1 et ne constituent pas une configuration Docker de production.

## Fonctions non annoncées comme disponibles

Ne pas appeler /api/v1/quittances/statistiques, /public/v1/verification/{token}, /internal/v1/paiements/confirmes ou /api/v1/quittances/{id}/envoyer : ces routes ne sont pas présentes dans les sources inspectées.
L'email local utilise /envois-mailpit ; le PDF émis local utilise /emission-demo/pdf.
La signature de démonstration n'est pas une signature qualifiée et la preuve SHA-256 ne certifie pas à elle seule l'identité d'une personne.
