require('dotenv').config();

const portStr = process.env.PORT || '8082';
const PORT = parseInt(portStr, 10);

const JWT_SECRET = process.env.JWT_SECRET || 'change_me_with_a_long_random_secret_of_at_least_32_chars';
const BIENS_SERVICE_URL = process.env.BIENS_SERVICE_URL || 'http://localhost:8081';
const LOYERS_SERVICE_URL = process.env.LOYERS_SERVICE_URL || 'http://localhost:8083';

module.exports = {
  PORT: isNaN(PORT) ? 8082 : PORT,
  NODE_ENV: process.env.NODE_ENV || 'development',
  JWT_SECRET,
  BIENS_SERVICE_URL,
  LOYERS_SERVICE_URL,
  DATABASE_URL: process.env.DATABASE_URL,
  DB_HOST: process.env.DB_HOST,
  DB_PORT: process.env.DB_PORT ? parseInt(process.env.DB_PORT, 10) : 5432,
  DB_NAME: process.env.DB_NAME,
  DB_USER: process.env.DB_USER,
  DB_PASSWORD: process.env.DB_PASSWORD,
};