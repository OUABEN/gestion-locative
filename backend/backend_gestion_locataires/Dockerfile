# syntax=docker/dockerfile:1
# =====================================================================
# backend_gestion_locataires — Node.js 20 + TypeScript — port 8082
# =====================================================================

# ---------- Stage 1 : build (compilation TypeScript -> dist/) ----------
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- Stage 2 : runtime (dépendances de production uniquement) ----------
FROM node:20-alpine AS runtime
ENV NODE_ENV=production \
    PORT=8082
WORKDIR /app

COPY --chown=node:node package.json package-lock.json ./
RUN npm ci --omit=dev && npm cache clean --force

COPY --from=build --chown=node:node /app/dist ./dist

# Utilisateur non-root fourni par l'image officielle node
USER node

EXPOSE 8082
CMD ["node", "dist/server.js"]

