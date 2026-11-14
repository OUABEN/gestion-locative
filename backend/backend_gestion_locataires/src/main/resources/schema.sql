-- Schéma initial pour la table des locataires
CREATE TABLE IF NOT EXISTS locataires (
    id UUID PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    cin VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(150),
    telephone VARCHAR(30),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_locataire_cin ON locataires(cin);
