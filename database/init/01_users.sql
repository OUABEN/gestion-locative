-- =====================================================================
-- 01_users.sql
-- Base : db_gestion_locative (PostgreSQL 16)
-- Table des utilisateurs (authentification JWT partagée)
-- =====================================================================

CREATE TABLE IF NOT EXISTS users (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nom            VARCHAR(100) NOT NULL,
    prenom         VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL
                   CONSTRAINT chk_users_role CHECK (role IN ('BAILLEUR', 'GESTIONNAIRE', 'LOCATAIRE')),
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);

