-- =====================================================================
-- 06_quittances.sql
-- Base : db_gestion_locative (PostgreSQL 16)
-- Module : backend_gestion_quittances (port 8084)
-- Liaisons (bail_id, paiement_id) : colonnes UUID simples, SANS FOREIGN KEY.
-- =====================================================================

CREATE TABLE IF NOT EXISTS quittances (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    bail_id           UUID          NOT NULL,
    paiement_id       UUID          NOT NULL,
    numero_quittance  VARCHAR(50)   NOT NULL UNIQUE,
    mois              INT           NOT NULL
                      CONSTRAINT chk_quittances_mois CHECK (mois BETWEEN 1 AND 12),
    annee             INT           NOT NULL
                      CONSTRAINT chk_quittances_annee CHECK (annee >= 2000),
    montant_paye      NUMERIC(12,2) NOT NULL
                      CONSTRAINT chk_quittances_montant_paye CHECK (montant_paye >= 0),
    date_paiement     DATE          NOT NULL,
    date_emission     DATE          NOT NULL DEFAULT CURRENT_DATE,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_quittances_dates CHECK (date_emission >= date_paiement)
);

CREATE INDEX IF NOT EXISTS idx_quittances_bail_id     ON quittances (bail_id);
CREATE INDEX IF NOT EXISTS idx_quittances_paiement_id ON quittances (paiement_id);
CREATE INDEX IF NOT EXISTS idx_quittances_mois_annee  ON quittances (annee, mois);

