-- Prérequis: database/init/03_quittances.sql (table quittance créée).
-- Chargé une seule fois par PostgreSQL lors de la première initialisation du volume.
CREATE TABLE IF NOT EXISTS emission_reelle (
    quittance_id UUID PRIMARY KEY REFERENCES quittance(id) ON DELETE RESTRICT,
    numero VARCHAR(40) NOT NULL UNIQUE,
    pdf BYTEA NOT NULL CHECK (octet_length(pdf) BETWEEN 1 AND 10485760),
    signature BYTEA NOT NULL CHECK (octet_length(signature) BETWEEN 256 AND 1024),
    certificat BYTEA NOT NULL CHECK (octet_length(certificat) BETWEEN 1 AND 32768),
    sha256 VARCHAR(64) NOT NULL CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    cree_le TIMESTAMPTZ NOT NULL
);
COMMENT ON TABLE emission_reelle IS 'Signature RSA détachée avec certificat configure ; valeur juridique non garantie par la seule vérification cryptographique.';
