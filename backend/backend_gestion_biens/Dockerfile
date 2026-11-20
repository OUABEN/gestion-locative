# syntax=docker/dockerfile:1
# =====================================================================
# backend_gestion_biens — Node.js 20 + TypeScript — port 8081
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
    PORT=8081
WORKDIR /app

COPY --chown=node:node package.json package-lock.json ./
RUN npm ci --omit=dev && npm cache clean --force

COPY --from=build --chown=node:node /app/dist ./dist

USER node

EXPOSE 8081

HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=10s \
  CMD wget -qO- http://localhost:8081/health || exit 1

CMD ["node", "dist/server.js"]
