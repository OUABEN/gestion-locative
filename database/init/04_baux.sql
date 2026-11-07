-- =====================================================================
-- 04_baux.sql
-- Base : db_gestion_locative (PostgreSQL 16)
-- Module : backend_gestion_locataires (port 8082)
-- Liaisons inter-modules (bien_id, locataire_id) : colonnes UUID simples,
-- SANS FOREIGN KEY (cohérence gérée par les microservices via REST).
-- =====================================================================

CREATE TABLE IF NOT EXISTS baux (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    bien_id         UUID          NOT NULL,
    locataire_id    UUID          NOT NULL,
    date_debut      DATE          NOT NULL,
    date_fin        DATE          NULL,
    montant_loyer   NUMERIC(12,2) NOT NULL
                    CONSTRAINT chk_baux_montant_loyer CHECK (montant_loyer >= 0),
    depot_garantie  NUMERIC(12,2) NOT NULL DEFAULT 0
                    CONSTRAINT chk_baux_depot_garantie CHECK (depot_garantie >= 0),
    statut          VARCHAR(10)   NOT NULL DEFAULT 'ACTIF'
                    CONSTRAINT chk_baux_statut CHECK (statut IN ('ACTIF', 'TERMINE')),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_baux_dates CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE INDEX IF NOT EXISTS idx_baux_bien_id      ON baux (bien_id);
CREATE INDEX IF NOT EXISTS idx_baux_locataire_id ON baux (locataire_id);
CREATE INDEX IF NOT EXISTS idx_baux_statut       ON baux (statut);

-- Un seul bail ACTIF par bien
CREATE UNIQUE INDEX IF NOT EXISTS uq_baux_bien_actif ON baux (bien_id) WHERE statut = 'ACTIF';

