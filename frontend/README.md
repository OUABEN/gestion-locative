# Frontend — Gestion des Biens Immobiliers (`frontend_gestion_biens`)

Application web React 18 + Vite pour le module **Gestion des Biens** du système d'information de gestion locative.

---

## 1. Stack Technique

- **Framework** : React 18 + Vite 6
- **Routage** : `react-router-dom` v6 (avec `ProtectedRoute` et `RoleGuard`)
- **Appels HTTP** : `axios` avec intercepteur d'authentification et gestion automatique de l'expiration du token (déconnexion sur 401)
- **Formulaires & Validation** : `react-hook-form` + `zod` (règles strictement synchronisées avec le contrat OpenAPI du backend)
- **Graphiques** : `recharts` (Donut répartition par statut, Barres par ville, Barres horizontales par type de bien)
- **Icônes & Polices** : `lucide-react`, `@fontsource/playfair-display`, `@fontsource/inter` (polices 100% locales, aucune ressource CDN externe)
- **Design System** : Palette de couleurs imposée (**Beige #E6D3B3**, **Marron #6B4426**, **Vert #2E7D4F**, et rouge pour les erreurs), contrastes WCAG AA, CSS Modules.

---

## 2. Variables d'Environnement

Le fichier `.env.example` contient les variables configurables :

```env
# URL du microservice Biens (relayé par le proxy Vite en dev sur /api/biens)
VITE_API_BIENS_URL=/api/biens

# URL du microservice d'authentification (si disponible)
VITE_AUTH_URL=http://localhost:8080

# Mode d'authentification ('token' pour coller un JWT généré, ou 'api' pour login classique)
VITE_AUTH_MODE=token
```

---

## 3. Lancement en Local

### Prérequis
- Node.js $\ge$ 20.x
- Backend `backend_gestion_biens` démarré sur le port `8081`

### Commandes
```powershell
# 1. Se déplacer dans le dossier
cd frontend/frontend_gestion_biens

# 2. Installer les dépendances
npm install

# 3. Lancer les vérifications de qualité
npm run lint
npm run build

# 4. Lancer le serveur de développement Vite (port 3000)
npm run dev
```

L'application s'ouvre sur : **[http://localhost:3000](http://localhost:3000)**.

---

## 4. Authentification & Rôles

### Génération d'un token de test
Dans le dossier `backend/backend_gestion_biens` :
```powershell
# Générer un token BAILLEUR (accès complet écriture / suppression)
npm run token BAILLEUR

# Générer un token GESTIONNAIRE (accès complet)
npm run token GESTIONNAIRE

# Générer un token LOCATAIRE (lecture seule, aucun bouton d'action)
npm run token LOCATAIRE
```

Copiez le token affiché et collez-le directement dans l'écran de connexion en mode **"Mode Token JWT"**.

### Comportement selon le rôle :
- **BAILLEUR / GESTIONNAIRE** : Accès total au tableau de bord, ajout de bien, modification, changement atomique de statut (LIBRE $\leftrightarrow$ OCCUPÉ), suppression.
- **LOCATAIRE** : Consultation du tableau de bord et du catalogue des biens. Les boutons "Ajouter", "Modifier" et "Supprimer" sont automatiquement masqués. En cas d'accès direct par URL à `/biens/nouveau`, un écran `RoleGuard` bloque l'action et informe l'utilisateur.

---

## 5. Configuration Docker & Nginx

Le dossier contient un `Dockerfile` multi-stage et une configuration `nginx.conf` assurant le fallback SPA et le proxy des requêtes vers le backend.

### Bloc à intégrer dans `docker-compose.yml` (à la racine) :

```yaml
  frontend_gestion_biens:
    build:
      context: ./frontend/frontend_gestion_biens
    image: gestion-locative/frontend_gestion_biens:local
    restart: unless-stopped
    ports:
      - "3000:80"
    depends_on:
      - backend_gestion_biens
    networks:
      - gl_network
```

