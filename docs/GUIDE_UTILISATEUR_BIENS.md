# Guide Utilisateur — Module Gestion des Biens (`LOCATIVE.`)

**Projet** : Plateforme Globale de Gestion Locative (Master d'Excellence Web Services — Sujet n° 16, Groupe 16)  
**Module** : Module 01 — Gestion des Biens Immobiliers (`backend_gestion_biens` + `frontend`)  
**Charte graphique** : Midnight Black (`#0B0B0C`) · Pure White (`#FFFFFF`) · Lime Green (`#C6F432`)  
**Devise** : Dirham marocain (`DH`, format `fr-MA`)

---

## 1. Présentation générale et Rôles utilisateurs

Le module **Gestion des Biens** permet de piloter l'ensemble du patrimoine immobilier de la plateforme **Gestion Locative** : suivi analytique en temps réel, consultation du catalogue, création et mise à jour des logements, chargement de photos stockées en base PostgreSQL et gestion atomique des statuts d'occupation (`LIBRE` / `OCCUPE`).

### Droits d'accès selon le rôle JWT (`RBAC`)

| Fonctionnalité / Écran | `BAILLEUR` (Propriétaire) | `GESTIONNAIRE` (Agence) | `LOCATAIRE` (Client) |
|---|:---:|:---:|:---:|
| Connexion & Inscription (`/login`) | Oui | Oui | Oui |
| Tableau de bord analytique & Graphiques (`/`) | Oui | Oui | Oui |
| Catalogue des biens : recherche, filtres, tri (`/biens`) | Oui | Oui | Oui (Lecture seule) |
| Fiche détaillée & Disponibilité (`/biens/:id`) | Oui | Oui | Oui (Lecture seule) |
| Ajouter un bien + Photo (`/biens/nouveau` & onglet Ajout) | **Oui** | **Oui** | Non (`RoleGuard` + `403`) |
| Modifier un bien (`/biens/:id/modifier`) | **Oui** | **Oui** | Non (`RoleGuard` + `403`) |
| Changer le statut `LIBRE` ↔ `OCCUPE` (`PATCH /statut`) | **Oui** | **Oui** | Non (Bouton masqué + `403`) |
| Supprimer un bien `LIBRE` (`DELETE /api/biens/:id`) | **Oui** | **Oui** | Non (Bouton masqué + `403`) |
| Supprimer un bien `OCCUPE` | **Refusé (`409 Conflict`)** | **Refusé (`409 Conflict`)** | Non (`403 Forbidden`) |

---

## 2. Authentification : Connexion & Inscription (`/login`)

À l'ouverture de l'application (`http://localhost:3000/login`), deux modes sont proposés dans la carte d'authentification :

### 2.1. Onglet « Connexion »
1. Saisissez votre **Adresse e-mail** (ex. `bailleur@gestion-locative.com`) et votre **Mot de passe**.
2. Cliquez une seule fois sur le bouton vert lime **« Se connecter »** : la session JWT s'ouvre immédiatement et vous redirige vers le **Tableau de bord** avec les données chargées.
3. **Accès direct en 1 clic (Comptes de démonstration)** : en bas du formulaire, trois boutons rapides (**Bailleur**, **Gestionnaire**, **Locataire**) permettent d'ouvrir instantanément une session correspondant au rôle souhaité.

### 2.2. Onglet « Inscription »
1. Cliquez sur l'onglet **« Inscription »**.
2. Renseignez votre **Prénom**, **Nom**, **Adresse e-mail**, **Mot de passe** (minimum 4 caractères) et choisissez votre **Profil & Permissions** (`BAILLEUR`, `GESTIONNAIRE` ou `LOCATAIRE`).
3. Cliquez sur **« Créer mon compte et accéder au Dashboard »** : le compte est enregistré dans la table PostgreSQL `users` et la session s'ouvre immédiatement sur le tableau de bord.

---

## 3. Navigation Principale et Panneau Latéral (`LOCATIVE.`)

Le panneau latéral gauche (`Sidebar`) en **Midnight Black (`#0B0B0C`)** regroupe toute la navigation de la plateforme :
- **Tableau de bord** (`/`)
- **Biens** (`/biens`) et sous-menu **Ajouter un bien** (`/biens/nouveau`, visible pour `BAILLEUR` et `GESTIONNAIRE`)
- **Locataires** (`/locataires` — synthèse inter-service port `8082`)
- **Loyers** (`/loyers` — synthèse inter-service port `8083`)
- **Quittances** (`/quittances` — synthèse inter-service port `8084`)

### Ouvrir / Masquer le menu latéral
- Cliquez sur le bouton **« Masquer menu »** en haut à gauche de la barre supérieure (`Topbar`), ou sur l'icône de fermeture dans l'en-tête du menu latéral, ou sur le bouton **« Masquer »** en bas du panneau pour passer en plein écran.
- Lorsque le panneau est fermé, le bouton de la barre supérieure devient **Lime Green (`Ouvrir menu`)** et une pilule flottante **`LOCATIVE. >`** apparaît en bas à gauche pour rouvrir le menu en un clic.

---

## 4. Tableau de bord (`/`)

Le tableau de bord offre une vision analytique complète et intègre une **barre de 3 choix rapides** dans le bandeau Héro :
1. **Tableau de bord** (vue analytique par défaut)
2. **Afficher les biens** (catalogue interactif intégré avec recherche et filtres par statut)
3. **Ajouter un bien** (création rapide avec aperçu carte en direct)

### 4.1. Indicateurs clés de performance (5 Cartes KPI)
- **Total des biens** : nombre total de logements enregistrés.
- **Disponibles** : nombre de biens au statut `LIBRE` prêts à être loués + barre de progression.
- **Biens Loués** : nombre de biens au statut `OCCUPE` (baux actifs).
- **Revenu réalisé** : somme des loyers mensuels en `DH` des biens `OCCUPE`, comparée au potentiel total du parc.
- **Charges** : cumul mensuel des provisions pour charges en `DH`.
- *(La jauge du **Taux d'occupation** en `%` est affichée à droite du bandeau Héro et au centre du graphique Donut).*

### 4.2. Graphiques interactifs (Recharts)
- **Revenus locatifs par ville** (courbe d'aire sur fond Midnight Black) : compare le revenu réalisé (`#C6F432`) au revenu potentiel par ville.
- **Occupation du parc** (Donut chart) : répartition `LIBRE` vs `OCCUPE` avec le taux d'occupation au centre.
- **Typologie des biens** (Barres horizontales) : nombre de biens par catégorie (*Appartement*, *Villa*, *Studio*, *Bureau*, *Local Commercial*).

### 4.3. Biens à la une & Derniers biens enregistrés
- **Biens à la une** : affiche les 3 biens au loyer le plus élevé sous forme de cartes illustrées.
- **Derniers biens enregistrés** : liste les 5 derniers logements avec miniature photo, ville, surface, loyer en `DH`, badge de statut et date.

---

## 5. Catalogue des Biens (`/biens`)

Accessible via le menu **Biens** ou le bouton **« Afficher les biens »** de la barre supérieure :
- **Recherche textuelle (avec debounce 300 ms)** : filtre en direct sur l'adresse, la ville ou le type.
- **Filtres combinables** :
  - *Ville* (liste dynamique des villes présentes)
  - *Statut* (`Tous les statuts`, `Libre`, `Occupé`)
  - *Type* (`Appartement`, `Villa`, `Studio`, `Bureau`, `Local Commercial`, etc.)
- **Tri** : *Plus récents*, *Loyer croissant*, *Loyer décroissant*, *Surface décroissante*.
- **Bascule de vue** :
  - **Vue en grille (Cartes)** : photo du bien avec zoom au survol, badge de type, badge de statut (`LIBRE` en vert lime avec point pulsant, `OCCUPE` en noir), prix mensuel en `DH`, surface, charges et boutons d'action.
  - **Vue en tableau** : lignes compactes avec miniature photo, adresse cliquable, ville, type, surface, loyer, charges, statut et actions.
- **Pagination** : 8 biens par page avec navigation *Précédent / Suivant*.

---

## 6. Ajouter ou Modifier un Bien (`/biens/nouveau` et `/biens/:id/modifier`)

Réservé aux rôles **`BAILLEUR`** et **`GESTIONNAIRE`** :

### Champs et règles de validation (React Hook Form + Zod + Backend)
- **Adresse complète*** : chaîne non vide obligatoire (ex. `18 Boulevard d'Anfa, Étage 4`).
- **Ville*** : chaîne non vide obligatoire (ex. `Casablanca`, `Rabat`, `Marrakech`).
- **Typologie du bien*** : chaîne non vide obligatoire (ex. `Appartement`, `Villa`, `Studio`, `Bureau`).
- **Surface habitable (m²)*** : nombre **strictement supérieur à 0** (`surface > 0`, ex. `85.5`).
- **Loyer mensuel (DH)*** : nombre **supérieur ou égal à 0** (`prixMensuel >= 0`, ex. `6500`).
- **Charges mensuelles (DH)** : nombre **supérieur ou égal à 0** (`charges >= 0`, `0` par défaut).
- **Statut d'occupation** *(en mode modification)* : `LIBRE` ou `OCCUPE`.
- **Barre Photo du bien (Stockée en base de données PostgreSQL)** :
  - Vous pouvez **coller un lien URL d'image** (`https://...`) dans la barre dédiée, **OU**
  - Cliquer sur le bouton **« Importer image »** pour choisir un fichier image (`JPG`, `PNG`, `WEBP`) depuis votre ordinateur.
  - Un **aperçu miniature en direct** s'affiche immédiatement sous la barre. Lors de la validation, l'image est stockée dans la colonne `image_url` de la table PostgreSQL `biens`. Si aucune image personnalisée n'est fournie, une photo haute définition correspondant au type du bien est utilisée automatiquement.

---

## 7. Fiche Détaillée, Changement de Statut et Suppression (`/biens/:id`)

En cliquant sur **« Consulter »** ou sur l'adresse d'un bien :
1. **Bandeau de couverture photo** : affiche la photo du bien en grand format, son type, son badge de statut, son adresse, sa ville et son loyer mensuel hors charges.
2. **Caractéristiques financières & techniques** : Loyer mensuel net, Charges locatives, Surface habitable (`m²`), et **Total mensuel charges comprises** (`Loyer + Charges`).
3. **Informations d'enregistrement** : Identifiant unique `UUID`, date de création et date de dernière mise à jour.
4. **Disponibilité opérationnelle (`GET /api/biens/:id/disponibilite`)** :
   - Encadré vert lime **« Logement disponible »** si `statut === 'LIBRE'`.
   - Encadré noir **« Logement occupé »** si `statut === 'OCCUPE'`.
5. **Changement d'état atomique (`PATCH /api/biens/:id/statut`)** *(Bailleur / Gestionnaire)* :
   - Cliquez sur **« Passer au statut OCCUPÉ »** (ou **« Passer au statut LIBRE »**) puis confirmez dans la boîte modale.
6. **Suppression protégée (`DELETE /api/biens/:id`)** *(Bailleur / Gestionnaire)* :
   - Cliquez sur **« Supprimer »** : une modale de confirmation s'ouvre.
   - **Règle métier critique** : si le bien est actuellement **`OCCUPE`**, un avertissement rouge s'affiche dans la modale et le serveur bloque la suppression avec le code **`HTTP 409 Conflict`** (*« Impossible de supprimer un bien occupé »*). Pour supprimer un bien, il faut d'abord que son bail soit terminé et son statut repassé à **`LIBRE`**.

---

## 8. Messages d'erreur courants et Solutions

| Code / Message affiché | Cause | Solution |
|---|---|---|
| **400 — Données invalides** (*La surface doit être strictement supérieure à 0*, etc.) | Un champ obligatoire est vide ou une valeur numérique viole les règles (`surface <= 0`, `prixMensuel < 0`, `charges < 0`). | Corrigez le champ signalé en rouge sous l'input puis validez à nouveau. |
| **401 — Mot de passe incorrect / Aucun compte trouvé** | E-mail ou mot de passe erroné lors de la connexion. | Vérifiez vos identifiants, utilisez un bouton d'accès rapide (*Bailleur*, *Gestionnaire*, *Locataire*) ou créez un compte via l'onglet *Inscription*. |
| **403 — Accès réservé (`RoleGuard`)** | Vous êtes connecté avec le rôle `LOCATAIRE` (lecture seule) et tentez d'accéder à `/biens/nouveau` ou `/biens/:id/modifier`. | Déconnectez-vous en haut à droite et connectez-vous avec un profil `BAILLEUR` ou `GESTIONNAIRE`. |
| **404 — Logement introuvable** | L'UUID demandé dans l'URL n'existe pas ou le bien vient d'être supprimé. | Cliquez sur **« Retour au catalogue »** pour revenir à la liste des biens actifs. |
| **409 — Impossible de supprimer un bien occupé** | Tentative de suppression d'un logement dont le statut est `OCCUPE`. | Ouvrez la fiche du bien, basculez d'abord son statut vers `LIBRE`, puis procédez à la suppression. |
| **409 — Conflit : le bien est déjà dans ce statut** | Deux modifications simultanées ont tenté d'appliquer le même statut (`LIBRE` ou `OCCUPE`). | Rafraîchissez la fiche du bien : le statut souhaité est déjà appliqué. |

---

## 9. Foire Aux Questions (FAQ)

- **Q1 : Pourquoi un bien nouvellement créé est-il toujours au statut `LIBRE` ?**  
  C'est une règle métier du cahier des charges : tout logement ajouté au patrimoine démarre à l'état `LIBRE` (et `charges = 0 DH` si non renseignées) jusqu'à la signature d'un bail actif.
- **Q2 : Comment ajouter ma propre photo pour un bien immobilier ?**  
  Sur la page *Ajouter un bien* (`/biens/nouveau`), sur la page *Modifier* (`/biens/:id/modifier`) ou dans l'onglet *Ajouter un bien* du Dashboard, utilisez la barre **« Photo du bien »** : collez une URL d'image ou cliquez sur **« Importer image »** pour sélectionner un fichier de votre ordinateur.
- **Q3 :Pourquoi les boutons « Ajouter un bien », « Modifier » et « Supprimer » n'apparaissent-ils pas ?**  
  Vérifiez le badge de rôle affiché à côté de votre e-mail en haut à droite : si vous êtes connecté en tant que **`LOCATAIRE`**, l'interface est automatiquement configurée en mode consultation (lecture seule).
- **Q4 : Comment masquer le menu latéral gauche pour voir les graphiques en plein écran ?**  
  Cliquez sur le bouton **« Masquer menu »** en haut à gauche de la barre supérieure (ou sur **« Masquer »** en bas du menu latéral).

---

## 10. Liste précise des captures d'écran à réaliser (`docs/captures/biens/`)

> *Note technique : Playwright n'étant pas installé dans l'environnement d'exécution local, aucune image factice n'a été générée conformément aux consignes QA. Voici la liste exacte des captures à enregistrer dans `docs/captures/biens/` depuis `http://localhost:3000` :*

1. `docs/captures/biens/01_login_connexion.png` — Écran `/login` (onglet **Connexion** avec les boutons d'accès direct *Bailleur*, *Gestionnaire*, *Locataire*).
2. `docs/captures/biens/02_login_inscription.png` — Écran `/login` (onglet **Inscription** avec choix du rôle).
3. `docs/captures/biens/03_dashboard_hero_kpis.png` — Écran `/` (**Tableau de bord** : bandeau Héro Midnight & Lime, jauge d'occupation, barre des 3 choix et les 5 cartes KPI).
4. `docs/captures/biens/04_dashboard_charts.png` — Écran `/` (Graphiques Recharts : *Revenus locatifs par ville*, *Donut Occupation du parc*, *Typologie des biens*).
5. `docs/captures/biens/05_sidebar_ouverte_et_fermee.png` — Vue du panneau latéral `LOCATIVE.` ouvert avec les 5 modules (*Tableau de bord, Biens, Locataires, Loyers, Quittances*) puis replié avec la pilule flottante `LOCATIVE. >`.
6. `docs/captures/biens/06_catalogue_grille_cartes.png` — Écran `/biens` (Catalogue en **vue grille** avec photos des biens, filtres Ville/Statut/Type et recherche).
7. `docs/captures/biens/07_catalogue_vue_tableau.png` — Écran `/biens` (Catalogue en **vue tableau** avec miniatures photos et actions).
8. `docs/captures/biens/08_ajout_bien_barre_image.png` — Écran `/biens/nouveau` (Formulaire de création avec la **barre d'import de photo** et l'aperçu miniature en direct).
9. `docs/captures/biens/09_detail_bien_et_statut.png` — Écran `/biens/:id` (Fiche détaillée avec grande photo de couverture, encadré de disponibilité opérationnelle et modale de changement de statut `LIBRE` ↔ `OCCUPE`).
10. `docs/captures/biens/10_suppression_refusee_409.png` — Écran `/biens/:id` (Modale de suppression sur un bien `OCCUPE` affichant l'alerte de blocage `409 Conflict`).
11. `docs/captures/biens/11_vue_locataire_lecture_seule.png` — Écran `/biens` connecté avec le rôle `LOCATAIRE` (boutons d'ajout, de modification et de suppression masqués).
