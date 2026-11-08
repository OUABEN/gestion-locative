import { Pool, PoolConfig } from "pg";
import { env } from "./env";
import { logger } from "../utils/logger";

const poolConfig: PoolConfig = env.DATABASE_URL
  ? { connectionString: env.DATABASE_URL }
  : {
      host: env.DB_HOST,
      port: env.DB_PORT,
      database: env.DB_NAME,
      user: env.DB_USER,
      password: env.DB_PASSWORD,
    };

export const pool = new Pool({
  ...poolConfig,
  max: 10,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
});

pool.on("error", (err) => {
  logger.error("Erreur inattendue sur le client PostgreSQL inactif :", err);
});

