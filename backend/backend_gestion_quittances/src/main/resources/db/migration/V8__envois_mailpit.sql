CREATE TABLE quittance_envoi (
    id UUID PRIMARY KEY,
    quittance_id UUID NOT NULL REFERENCES quittance(id),
    requete_id UUID NOT NULL,
    destinataire VARCHAR(254) NOT NULL,
    mode VARCHAR(20) NOT NULL CHECK (mode='MAILPIT'),
    statut VARCHAR(30) NOT NULL CHECK (statut IN ('EN_ATTENTE_ENVOI','ENVOYE','ECHEC_ENVOI','INDETERMINE')),
    cree_le TIMESTAMPTZ NOT NULL,
    termine_le TIMESTAMPTZ,
    erreur_code VARCHAR(60),
    CONSTRAINT uq_envoi_requete UNIQUE(quittance_id,requete_id)
);
CREATE UNIQUE INDEX uq_envoi_document_actif ON quittance_envoi(quittance_id)
    WHERE statut <> 'ECHEC_ENVOI';
CREATE INDEX idx_envoi_document_date ON quittance_envoi(quittance_id,cree_le DESC);
COMMENT ON TABLE quittance_envoi IS 'Tentatives Mailpit locales. ENVOYE signifie accepte par SMTP, pas remis a une personne.';
