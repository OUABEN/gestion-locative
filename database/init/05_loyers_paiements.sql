-- =====================================================================
-- 05_loyers_paiements.sql
-- Base : db_gestion_locative (PostgreSQL 16)
-- Module : backend_gestion_loyers (port 8083)
-- Liaisons (bail_id, echeance_id) : colonnes UUID simples, SANS FOREIGN KEY.
-- =====================================================================

CREATE TABLE IF NOT EXISTS echeances (
    id       UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    bail_id  UUID          NOT NULL,
    mois     INT           NOT NULL
             CONSTRAINT chk_echeances_mois CHECK (mois BETWEEN 1 AND 12),
    annee    INT           NOT NULL
             CONSTRAINT chk_echeances_annee CHECK (annee >= 2000),
    montant  NUMERIC(12,2) NOT NULL
             CONSTRAINT chk_echeances_montant CHECK (montant >= 0),
    statut   VARCHAR(15)   NOT NULL DEFAULT 'A_PAYER'
             CONSTRAINT chk_echeances_statut CHECK (statut IN ('A_PAYER', 'PAYEE', 'EN_RETARD', 'IMPAYEE')),
    CONSTRAINT uq_echeances_bail_mois_annee UNIQUE (bail_id, mois, annee)
);

CREATE INDEX IF NOT EXISTS idx_echeances_bail_id ON echeances (bail_id);
CREATE INDEX IF NOT EXISTS idx_echeances_statut  ON echeances (statut);

CREATE TABLE IF NOT EXISTS paiements (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    echeance_id    UUID          NOT NULL,
    montant        NUMERIC(12,2) NOT NULL
                   CONSTRAINT chk_paiements_montant CHECK (montant > 0),
    date_paiement  DATE          NOT NULL DEFAULT CURRENT_DATE,
    mode_paiement  VARCHAR(30)   NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_paiements_echeance_id   ON paiements (echeance_id);
CREATE INDEX IF NOT EXISTS idx_paiements_date_paiement ON paiements (date_paiement);

