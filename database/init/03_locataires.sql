-- =====================================================================
-- 03_locataires.sql
-- Base : db_gestion_locative (PostgreSQL 16)
-- Module : backend_gestion_locataires (port 8082)
-- =====================================================================

CREATE TABLE IF NOT EXISTS locataires (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nom         VARCHAR(100) NOT NULL,
    prenom      VARCHAR(100) NOT NULL,
    email       VARCHAR(255),
    telephone   VARCHAR(30),
    cin         VARCHAR(20)  NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_locataires_nom   ON locataires (nom);
CREATE INDEX IF NOT EXISTS idx_locataires_email ON locataires (email);

