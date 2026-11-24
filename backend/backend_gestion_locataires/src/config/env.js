require('dotenv').config();

const portStr = process.env.PORT || '8082';
const PORT = parseInt(portStr, 10);

// JWT_SECRET : lu depuis l'environnement, jamais stocké en dur en production
const JWT_SECRET = process.env.JWT_SECRET || (process.env.NODE_ENV === 'test' ? 'test_jwt_secret_for_unit_tests_32_chars_long' : undefined);

if (!JWT_SECRET && process.env.NODE_ENV === 'production') {
  throw new Error("Variable d'environnement obligatoire manquante : JWT_SECRET");
}

// URLs des microservices partenaires
const BIENS_SERVICE_URL = process.env.BIENS_SERVICE_URL || 'http://localhost:8081';
const LOYERS_SERVICE_URL = process.env.LOYERS_SERVICE_URL || 'http://localhost:8083';
const QUITTANCES_SERVICE_URL = process.env.QUITTANCES_SERVICE_URL || 'http://localhost:8084';

// Configuration PostgreSQL standardisée
// En environnement Docker (DOCKER_ENV=true, CONTAINER=true ou NODE_ENV=production sans DB_HOST explicite), l'hôte par défaut est 'db'
const isDocker = process.env.DOCKER_ENV === 'true' || process.env.IS_DOCKER === 'true' || process.env.NODE_ENV === 'production';
const DB_HOST = process.env.DB_HOST || (isDocker ? 'db' : 'localhost');
const DB_PORT = process.env.DB_PORT ? parseInt(process.env.DB_PORT, 10) : 5432;
const DB_NAME = process.env.DB_NAME || process.env.POSTGRES_DB || 'db_gestion_locative';
const DB_USER = process.env.DB_USER || process.env.POSTGRES_USER || 'gl_user';
const DB_PASSWORD = process.env.DB_PASSWORD || process.env.POSTGRES_PASSWORD || '';
const DATABASE_URL = process.env.DATABASE_URL || (DB_HOST && DB_USER ? `postgresql://${DB_USER}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}` : undefined);

module.exports = {
  PORT: isNaN(PORT) ? 8082 : PORT,
  NODE_ENV: process.env.NODE_ENV || 'development',
  JWT_SECRET: JWT_SECRET || '',
  BIENS_SERVICE_URL,
  LOYERS_SERVICE_URL,
  QUITTANCES_SERVICE_URL,
  DB_HOST,
  DB_PORT,
  DB_NAME,
  DB_USER,
  DB_PASSWORD,
  DATABASE_URL,
};
