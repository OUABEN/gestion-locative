import { createApp } from "./app";
import { env } from "./config/env";
import { pool } from "./config/database";
import { logger } from "./utils/logger";

const app = createApp();

const server = app.listen(env.PORT, () => {
  logger.info(`Microservice backend_gestion_biens démarré sur le port ${env.PORT} (mode: ${env.NODE_ENV})`);
  logger.info(`Documentation Swagger disponible sur http://localhost:${env.PORT}/api-docs`);
});

const gracefulShutdown = (signal: string) => {
  logger.info(`Signal ${signal} reçu. Arrêt gracieux en cours...`);
  server.close(async () => {
    try {
      await pool.end();
      logger.info("Connexions PostgreSQL fermées.");
      process.exit(0);
    } catch (err) {
      logger.error("Erreur lors de la fermeture du pool PostgreSQL :", err);
      process.exit(1);
    }
  });
};

process.on("SIGTERM", () => gracefulShutdown("SIGTERM"));
process.on("SIGINT", () => gracefulShutdown("SIGINT"));

