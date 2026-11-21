CREATE TABLE paiement_affectation (
                                      id UUID PRIMARY KEY,

                                      quittance_id UUID NOT NULL,

                                      affectation_externe_id VARCHAR(100) NOT NULL,
                                      paiement_externe_id VARCHAR(100) NOT NULL,

                                      periode_debut DATE NOT NULL,
                                      periode_fin DATE NOT NULL,

                                      nature VARCHAR(20) NOT NULL,
                                      montant NUMERIC(12,2) NOT NULL,
                                      devise VARCHAR(3) NOT NULL,
                                      statut_source VARCHAR(20) NOT NULL,

                                      CONSTRAINT fk_affectation_quittance
                                          FOREIGN KEY (quittance_id)
                                              REFERENCES quittance (id)
                                              ON DELETE RESTRICT,

                                      CONSTRAINT uq_affectation_document
                                          UNIQUE (quittance_id, affectation_externe_id),

                                      CONSTRAINT ck_affectation_identifiants
                                          CHECK (
                                              LENGTH(BTRIM(affectation_externe_id)) > 0
                                                  AND LENGTH(BTRIM(paiement_externe_id)) > 0
                                              ),

                                      CONSTRAINT ck_affectation_periode
                                          CHECK (periode_fin >= periode_debut),

                                      CONSTRAINT ck_affectation_nature
                                          CHECK (nature IN ('LOYER', 'CHARGES')),

                                      CONSTRAINT ck_affectation_montant
                                          CHECK (montant > 0),

                                      CONSTRAINT ck_affectation_devise
                                          CHECK (devise = 'MAD'),

                                      CONSTRAINT ck_affectation_statut
                                          CHECK (statut_source = 'CONFIRME')
);

CREATE INDEX idx_affectation_paiement_externe
    ON paiement_affectation (paiement_externe_id);

CREATE INDEX idx_affectation_identifiant_externe
    ON paiement_affectation (affectation_externe_id);