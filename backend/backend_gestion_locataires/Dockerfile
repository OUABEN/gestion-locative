FROM node:20-alpine

WORKDIR /app

COPY package*.json ./

RUN npm install --omit=dev

COPY . .

EXPOSE 8082

HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=10s \
  CMD wget -qO- http://localhost:8082/health || exit 1

USER node

CMD ["node", "server.js"]

