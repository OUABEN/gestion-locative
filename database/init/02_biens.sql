-- =====================================================================
-- 02_biens.sql
-- Base : db_gestion_locative (PostgreSQL 16)
-- Module : backend_gestion_biens (port 8081)
-- =====================================================================

CREATE TABLE IF NOT EXISTS biens (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    adresse       VARCHAR(255)  NOT NULL,
    ville         VARCHAR(100)  NOT NULL,
    type          VARCHAR(50)   NOT NULL,
    surface       NUMERIC(10,2) NOT NULL
                  CONSTRAINT chk_biens_surface CHECK (surface > 0),
    prix_mensuel  NUMERIC(12,2) NOT NULL
                  CONSTRAINT chk_biens_prix_mensuel CHECK (prix_mensuel >= 0),
    charges       NUMERIC(12,2) NOT NULL DEFAULT 0
                  CONSTRAINT chk_biens_charges CHECK (charges >= 0),
    statut        VARCHAR(10)   NOT NULL DEFAULT 'LIBRE'
                  CONSTRAINT chk_biens_statut CHECK (statut IN ('LIBRE', 'OCCUPE')),
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_biens_ville  ON biens (ville);
CREATE INDEX IF NOT EXISTS idx_biens_statut ON biens (statut);

