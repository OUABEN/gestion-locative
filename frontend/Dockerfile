# syntax=docker/dockerfile:1
# =====================================================================
# frontend — React JS — servi par nginx (port 80 dans le conteneur)
# =====================================================================

# ---------- Stage 1 : build (bundle statique) ----------
FROM node:20-alpine AS build
WORKDIR /app

# URLs des API intégrées au bundle au moment du build.
# Par défaut : chemins relatifs, relayés par nginx vers les microservices.
ARG API_BIENS_URL=/api/biens
ARG API_LOCATAIRES_URL=/api/locataires
ARG API_LOYERS_URL=/api/loyers
ARG API_QUITTANCES_URL=/api/quittances

# Exposées à la fois pour Vite (VITE_*) et Create React App (REACT_APP_*)
ENV VITE_API_BIENS_URL=$API_BIENS_URL \
    VITE_API_LOCATAIRES_URL=$API_LOCATAIRES_URL \
    VITE_API_LOYERS_URL=$API_LOYERS_URL \
    VITE_API_QUITTANCES_URL=$API_QUITTANCES_URL \
    REACT_APP_API_BIENS_URL=$API_BIENS_URL \
    REACT_APP_API_LOCATAIRES_URL=$API_LOCATAIRES_URL \
    REACT_APP_API_LOYERS_URL=$API_LOYERS_URL \
    REACT_APP_API_QUITTANCES_URL=$API_QUITTANCES_URL

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Vite produit dist/, Create React App produit build/ : les deux sont gérés
RUN npm run build \
 && mkdir -p /app/out \
 && if [ -d dist ]; then cp -r dist/. /app/out/; \
    elif [ -d build ]; then cp -r build/. /app/out/; \
    else echo "Erreur : ni dist/ ni build/ apres npm run build" >&2; exit 1; fi

# ---------- Stage 2 : runtime nginx ----------
FROM nginx:alpine AS runtime

# Cibles du reverse proxy /api/* (surchargées par docker-compose)
ENV BIENS_SERVICE_URL=http://backend_gestion_biens:8081 \
    LOCATAIRES_SERVICE_URL=http://backend_gestion_locataires:8082 \
    LOYERS_SERVICE_URL=http://backend-gestion-loyers:8083 \
    QUITTANCES_SERVICE_URL=http://backend-gestion-quittances:8084

# Les fichiers de /etc/nginx/templates sont passés à envsubst au démarrage
# et écrits dans /etc/nginx/conf.d/ (mécanisme natif de l'image nginx).
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/out /usr/share/nginx/html

EXPOSE 80

