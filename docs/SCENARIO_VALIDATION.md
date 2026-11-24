# Validation du scénario de bout en bout

## Démarrage Docker

À la racine du dépôt, copier `.env.example` vers `.env`, puis remplacer les valeurs de démonstration par des valeurs locales adaptées. Ne pas publier le contenu de `.env`. Le Compose principal utilise les noms de service `db`, `biens`, `locataires`, `loyers`, `quittances` et `mailpit` (et `frontend` si l’interface doit aussi démarrer). Les noms `gestion-biens`, `gestion-locataires`, `gestion-loyers` et `gestion-quittances` ne sont pas déclarés comme services.

Pour démarrer les dépendances du scénario sans construire le frontend :

```bash
docker compose up --build -d db biens locataires loyers quittances mailpit
docker compose ps
```

`docker compose ps` vide après une erreur `no such service` signifie que cette commande n’a démarré aucun conteneur; cela ne fournit pas de résultat de validation du scénario. Après le démarrage, consulter `docker compose logs -f <service>` en utilisant une clé `services:` valide si un service ne démarre pas.

Le scénario automatisé est `scripts/scenario.sh`. Il requiert `curl`, `jq` et `LOGIN_PASSWORD` dans l’environnement; `TENANT_EMAIL` et `TENANT_PASSWORD` sont nécessaires pour tenter l’étape de téléchargement avec un compte locataire. Les identifiants ne doivent pas être inscrits dans ce document ni dans le script. Pour l’exécuter lorsque les services sont accessibles sur les ports locaux :

```bash
export LOGIN_PASSWORD  # définir cette variable dans le terminal sans l’inscrire dans un fichier suivi par Git
./scripts/scenario.sh
```

Le script crée des données de scénario dans les services. Exécution effective du script : **NON TESTÉ** (aucune exécution n’a été réalisée pendant la rédaction de cette procédure).

Les colonnes de résultat ci-dessous restent à renseigner après exécution. Aucun résultat du scénario n’a été exécuté dans le cadre de la préparation de ce document.

| Étape | Endpoint | Attendu | Résultat Docker | Résultat local |
|---|---|---|---|---|
| Authentification bailleur | `POST :8081/api/auth/login` | HTTP 200 et jeton | NON TESTÉ | HTTP 200 OK |
| Refus sans jeton | `GET :8081/api/biens` | HTTP 401 | NON TESTÉ | HTTP 401 OK |
| Création d’un bien | `POST :8081/api/biens` | HTTP 201 et identifiant | NON TESTÉ | HTTP 201 OK |
| Création locataire avec CIN unique | `POST :8082/api/locataires` | HTTP 201 et identifiant | NON TESTÉ | HTTP 201 OK |
| Refus d’un CIN déjà utilisé | `POST :8082/api/locataires` | HTTP 409 | NON TESTÉ | HTTP 409 OK |
| Création du bail | `POST :8083/api/baux` | HTTP 201 et identifiant | NON TESTÉ | HTTP 503 KO; détail serveur non fourni |
| Refus d’un second bail actif sur le bien | `POST :8083/api/baux` | HTTP 409 | NON TESTÉ | NON TESTÉ |
| Recherche de l’échéance du mois | `GET :8083/api/echeances?bailId={bailId}` | HTTP 200; réutiliser l’échéance éventuellement créée par le bail | NON TESTÉ | NON TESTÉ |
| Génération de l’échéance manquante | `POST :8083/api/echeances/generation/baux/{bailId}` | HTTP 201 et identifiant | NON TESTÉ | NON TESTÉ |
| Enregistrement du paiement | `POST :8083/api/paiements` | HTTP 201 | NON TESTÉ | NON TESTÉ |
| Préparation de la quittance | `POST :8084/api/v1/quittances` | HTTP 201 et identifiant | NON TESTÉ | NON TESTÉ |
| Validation de la quittance | `POST :8084/api/v1/quittances/{id}/valider` | HTTP 200 | NON TESTÉ | NON TESTÉ |
| Émission PDF de démonstration | `POST :8084/api/v1/quittances/{id}/emission-demo` | HTTP 200 | NON TESTÉ | NON TESTÉ |
| Téléchargement PDF avec jeton locataire | `GET :8084/api/v1/quittances/{id}/emission-demo/pdf` | HTTP 200 et PDF | NON TESTÉ | NON TESTÉ |
| Envoi de démonstration via Mailpit | `POST :8084/api/v1/quittances/{id}/envois-mailpit` | HTTP 200; message visible sur Mailpit `:8025` | NON TESTÉ | NON TESTÉ |

## Prérequis et limites à vérifier

- Le script `scripts/scenario.sh` lit les mots de passe depuis `LOGIN_PASSWORD`, et le cas locataire depuis `TENANT_EMAIL` / `TENANT_PASSWORD`. Il exige `curl` et `jq`.
- Les routes d’émission démo et d’envoi Mailpit requièrent le mode de sécurité `LOCAL`. Le Compose principal configure `JWT_MODE=COMMUN` pour Quittances. L’émission démo est désactivée par défaut (`DARLOC_SIGNATURE_DEMO=false`) et exige un certificat de signature de démonstration et son mot de passe via l’environnement.
- Le JWT produit par le login Biens expose `sub`, `role` et `email`. Le lecteur Quittances consulté attend des claims métier `bailleur_id` ou `locataire_id`; la compatibilité de ce jeton avec ces routes doit être validée ou configurée.
- Le locataire créé par l’API Locataires n’entraîne pas la création d’un utilisateur dans Biens. Le téléchargement avec un compte locataire distinct et l’association entre ce compte, le bail et la quittance restent à vérifier.
- La vérification du statut 409 sur le CIN dépend du service Locataires actuellement exécuté : le code contrôleur observé renvoie 409, mais ses fiches sont en mémoire et initialisées avec des données de démonstration.
- « Génération de l’échéance du mois » appelle l’endpoint qui génère l’échéance pour le bail et la période indiqués. Une échéance peut déjà avoir été créée automatiquement à la création du bail si celui-ci couvre le mois courant.

## Retours d’intégration à confirmer

### Flyway et base partagée

Le développeur Quittances indique que ses neuf migrations fonctionnent sur une base dédiée initialement vide. Il signale un conflit possible entre ces migrations et les scripts d’initialisation de la base collective. Cette information n’a pas été reproduite indépendamment ici. La comparaison des fichiers montre que `database/init/03_quittances.sql` crée déjà plusieurs objets également créés par les migrations V1 à V8, notamment `numero_document_seq`, `quittance`, `paiement_affectation`, `quittance_validation`, `quittance_audit`, `document_fichier`, `emission_demo` et `quittance_envoi`. `database/init/07_quittances_emission_reelle.sql` crée aussi `emission_reelle`, créée par V9. Cela rend plausible le conflit signalé quand Flyway rencontre ces objets sans sa table d’historique; la cause exacte reste à confirmer sur la base concernée.

**Aucune baseline, suppression de volume ou réinitialisation n’est autorisée à ce stade.** Avant toute procédure de migration, relever les tables existantes, la présence et le contenu de `flyway_schema_history`, puis examiner les scripts réellement exécutés et les logs. Résultat de cette vérification runtime : **NON TESTÉ** (aucune base n’a été inspectée dans le cadre de cette documentation).

### JWT et identité des comptes

Le développeur indique avoir adapté les autorisations internes du gestionnaire et la résolution du locataire via `/api/locataires/me`; il rapporte que 47 tests ciblés passent. Ce résultat est rapporté par le développeur et n’a pas été exécuté indépendamment ici.

Restent à confirmer avec les responsables des services : le contrat d’identité du bailleur (endpoint et champs nom/prénom par identifiant) et le rattachement du compte utilisateur LOCATAIRE à sa fiche locataire. En conséquence, le test de téléchargement PDF avec un jeton locataire dans le scénario reste **NON TESTÉ** jusqu’à confirmation de ce rattachement et du contrat d’identité.

### Création du bail

Lors de l’exécution locale communiquée par Ayoub, les cinq premières étapes ont répondu comme attendu (200, 401, 201, 201 et 409), puis `POST /api/baux` a répondu HTTP 503. Le script s’est arrêté faute d’identifiant du bail; les étapes suivantes sont donc **NON TESTÉES** lors de cette exécution. Le corps de la réponse HTTP n’a pas été fourni.

Dans le code Loyers consulté, les erreurs des appels à Biens et Locataires sont converties en `ServiceIndisponibleException`, puis en HTTP 503. Le 503 observé est donc compatible avec un échec de l’un de ces appels pendant la création du bail; la trace seule ne permet pas de dire lequel. Le détail de l’erreur HTTP et les logs Loyers sont nécessaires pour trancher. Le script affiche maintenant le champ `detail` ou `message` d’une réponse d’erreur quand il existe, et désactive `bash -x` dès son démarrage pour éviter d’imprimer les mots de passe et jetons.

Le relevé Docker transmis ensuite indique que `loyers` était `Up ... (healthy)` au moment de `docker compose ps -a`, tandis que `quittances` était en redémarrage. Les logs Compose montrent plusieurs échecs de démarrage de Quittances : Flyway valide neuf migrations puis arrête l’application avec `Found non-empty schema(s) "public" but no schema history table`. Cela confirme un incident de démarrage de Quittances sur la base partagée, mais **ne démontre pas la cause du HTTP 503 reçu par `POST /api/baux`**. Les logs Loyers ne figurent pas dans la sortie transmise; le 503 reste à diagnostiquer séparément.

Le contrôleur Loyers déclare HTTP 201 (`@ResponseStatus(HttpStatus.CREATED)`) pour la création d’un bail; le script et le tableau utilisent maintenant ce statut attendu. Cette correction du statut attendu ne résout pas le 503 observé. Pour diagnostiquer ce 503, relever le corps HTTP de l’erreur puis consulter `docker compose logs --tail=200 loyers` et vérifier que Loyers peut joindre Biens, Locataires et PostgreSQL avec les URLs de service du réseau Docker. Pour Quittances, examiner l’état et l’historique réels de la base avant toute action Flyway. Ne pas marquer le scénario complet comme validé avant une nouvelle exécution réussie.

### Nouvelle tentative manuelle du 24 novembre 2026

La commande manuelle transmise a produit une longueur de jeton égale à zéro. Les deux extractions d’identifiant ont échoué avec `KeyError: 0`, puis les appels à Biens et Locataires ont répondu HTTP 401 avec « Format du jeton invalide » parce que l’en-tête portait un jeton vide. La réponse HTTP de `POST /api/auth/login` n’a pas été affichée; son statut et sa cause sont donc **NON TESTÉS / NON CONFIRMÉS** par cette sortie. Les appels de disponibilité, lecture locataire et création du bail ne constituent pas des tests authentifiés et ne permettent pas de confirmer leur comportement métier.

Le relevé de processus ultérieur montre un `java -jar app.jar` et des processus Node, mais ne donne ni ports, ni répartition certaine des services Spring. `/tmp/loyers.log` est vide. Ces éléments ne permettent pas d’identifier la raison de l’absence de jeton ni d’expliquer le 503 précédent. Relancer l’authentification seule en affichant son code HTTP et son message d’erreur sans afficher le mot de passe; vérifier que le mot de passe a bien été saisi et que le compte existe dans le service Biens ciblé. Ensuite seulement, poursuivre avec un jeton non vide et relever le code et le corps de chaque réponse.

### Nouvelle tentative avec disponibilité et locataire accessibles

Ayoub a ensuite relevé `GET /api/biens/{id}/disponibilite` → HTTP 200 et `GET /api/locataires/{id}` → HTTP 200, puis `POST /api/baux` → HTTP 503 avec `detail: "Service Biens indisponible"`. Le corps indique que l’échec signalé lors de la création concerne le client Biens de Loyers. Il ne permet pas de savoir si la cause est l’URL configurée dans le processus Loyers, une erreur de connexion/routage, ou un problème différent dans l’appel interne. Les deux GET directs étant réussis, ils ne vérifient ni l’URL réellement utilisée par Loyers, ni la requête envoyée depuis ce processus, ni son en-tête Authorization.

Dans le code consulté, `BailService.creer` appelle d’abord Locataires, puis Biens pour la disponibilité, enregistre le bail, et appelle de nouveau Biens pour modifier le statut en `OCCUPE`. Le client Biens mappe une réponse HTTP avec statut vers un détail du type `Le service Biens a répondu <code>`; le détail exact observé ici, `Service Biens indisponible`, correspond à son autre branche `RestClientException`. Cela oriente le diagnostic vers un échec client/transport, sans établir sa cause racine. Le 503 et le détail sont **résultats communiqués par Ayoub**, non reproduits indépendamment ici.

Pour isoler l’appel fautif, vérifier la valeur effective de `BIENS_SERVICE_URL` dans le processus/conteneur Loyers (depuis Docker, une URL interne telle que `http://biens:8081` est attendue; depuis l’hôte, `http://localhost:8081`), puis relever les logs Loyers autour de la requête et tester les deux appels sortants de création depuis le même contexte réseau : disponibilité et `PATCH /api/biens/{id}/statut` avec le même jeton. L’appel PATCH intervient après l’enregistrement dans la transaction; vérifier également l’état du bail après le 503 avant toute nouvelle tentative. Ces vérifications et la cause racine sont **NON TESTÉES / NON CONFIRMÉES** à ce stade. La création du bail n’est pas validée et les étapes qui en dépendent restent **NON TESTÉES**.


## Relevé des résultats Docker communiqué

Ce relevé remplace les résultats précédents du document pour l’état du scénario. Tout résultat non indiqué ci-dessous reste **NON TESTÉ**. Le scénario Docker s’est arrêté à la création du bail. Le scénario local est **NON TESTÉ**.

| Étape | Résultat Docker | Résultat local |
|---|---|---|
| Connexion | 200 OK | NON TESTÉ |
| Sans jeton | 401 OK | NON TESTÉ |
| Création bien | 201 OK | NON TESTÉ |
| Création locataire | 201 OK | NON TESTÉ |
| CIN en double | 409 OK | NON TESTÉ |
| Création bail | 503 KO — « Service Biens indisponible »; Biens joignable depuis le conteneur Loyers, cause non identifiée | NON TESTÉ |
| Échéance | NON TESTÉ (scénario arrêté au bail) | NON TESTÉ |
| Paiement | NON TESTÉ (scénario arrêté au bail) | NON TESTÉ |
| Quittance | NON TESTÉ (scénario arrêté au bail) | NON TESTÉ |
| PDF | NON TESTÉ (scénario arrêté au bail) | NON TESTÉ |
| E-mail | NON TESTÉ (scénario arrêté au bail) | NON TESTÉ |
| Téléchargement | NON TESTÉ (scénario arrêté au bail) | NON TESTÉ |
| Refus d’un second bail actif sur le bien | NON TESTÉ | NON TESTÉ |

## Problèmes constatés

1. **Quittances — Flyway / base / init :** conflit corrigé (PR ayoub-docker).
2. **Quittances — clients HTTP :** démarrent en mode SIMULATION, refusé en Docker; variables `*_MODE` et `*_CHEMIN` à définir par Akram.
3. **Loyers — création du bail :** HTTP 503 (« Service Biens indisponible »); Biens est joignable depuis le conteneur Loyers, cause non identifiée.
4. **Quittances — chemins par défaut :** `/api/v1/paiements/affectations` ne correspond pas aux routes de Loyers.
