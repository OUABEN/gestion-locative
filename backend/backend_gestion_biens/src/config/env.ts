import dotenv from "dotenv";

dotenv.config();

export interface EnvConfig {
  PORT: number;
  NODE_ENV: string;
  DATABASE_URL?: string;
  DB_HOST?: string;
  DB_PORT?: number;
  DB_NAME?: string;
  DB_USER?: string;
  DB_PASSWORD?: string;
  JWT_SECRET: string;
}

const getEnvOrThrow = (key: string): string => {
  const value = process.env[key];
  if (!value || value.trim() === "") {
    throw new Error(`Variable d'environnement obligatoire manquante : ${key}`);
  }
  return value.trim();
};

const portStr = process.env.PORT || "8081";
const PORT = parseInt(portStr, 10);
if (isNaN(PORT) || PORT <= 0) {
  throw new Error(`Valeur de PORT invalide : ${process.env.PORT}`);
}

const JWT_SECRET = getEnvOrThrow("JWT_SECRET");

// Vérification de la connexion DB (soit DATABASE_URL, soit DB_HOST + DB_USER + DB_PASSWORD + DB_NAME)
const DATABASE_URL = process.env.DATABASE_URL?.trim();
const DB_HOST = process.env.DB_HOST?.trim();
const DB_PORT = process.env.DB_PORT ? parseInt(process.env.DB_PORT, 10) : 5432;
const DB_NAME = process.env.DB_NAME?.trim();
const DB_USER = process.env.DB_USER?.trim();
const DB_PASSWORD = process.env.DB_PASSWORD?.trim();

if (!DATABASE_URL && (!DB_HOST || !DB_USER || !DB_PASSWORD || !DB_NAME)) {
  throw new Error(
    "Configuration de base de données incomplète : DATABASE_URL ou (DB_HOST, DB_USER, DB_PASSWORD, DB_NAME) est requis."
  );
}

export const env: EnvConfig = {
  PORT,
  NODE_ENV: process.env.NODE_ENV || "development",
  DATABASE_URL,
  DB_HOST,
  DB_PORT,
  DB_NAME,
  DB_USER,
  DB_PASSWORD,
  JWT_SECRET,
};

