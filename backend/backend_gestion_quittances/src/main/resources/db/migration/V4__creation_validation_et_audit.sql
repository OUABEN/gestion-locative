CREATE TABLE quittance_validation (
                                      id UUID PRIMARY KEY,
                                      quittance_id UUID NOT NULL,
                                      acteur VARCHAR(200) NOT NULL,
                                      emetteur_identite VARCHAR(500) NOT NULL,
                                      valide_le TIMESTAMPTZ NOT NULL,
                                      version_document_avant BIGINT NOT NULL,
                                      statut_avant VARCHAR(30) NOT NULL,

                                      CONSTRAINT fk_validation_quittance
                                          FOREIGN KEY (quittance_id)
                                              REFERENCES quittance(id)
                                              ON DELETE RESTRICT,

                                      CONSTRAINT uq_validation_quittance
                                          UNIQUE (quittance_id),

                                      CONSTRAINT ck_validation_acteur
                                          CHECK (length(trim(acteur)) > 0),

                                      CONSTRAINT ck_validation_emetteur
                                          CHECK (length(trim(emetteur_identite)) > 0),

                                      CONSTRAINT ck_validation_version
                                          CHECK (version_document_avant >= 0),

                                      CONSTRAINT ck_validation_statut_avant
                                          CHECK (
                                              statut_avant IN (
                                                               'BROUILLON',
                                                               'EN_ATTENTE_VALIDATION'
                                                  )
                                              )
);

CREATE TABLE quittance_audit (
                                 id UUID PRIMARY KEY,
                                 quittance_id UUID NOT NULL,
                                 acteur VARCHAR(200) NOT NULL,
                                 emetteur_identite VARCHAR(500) NOT NULL,
                                 operation VARCHAR(40) NOT NULL,
                                 resultat VARCHAR(20) NOT NULL,
                                 horodatage TIMESTAMPTZ NOT NULL,
                                 statut_avant VARCHAR(30),
                                 statut_apres VARCHAR(30),

                                 CONSTRAINT fk_audit_quittance
                                     FOREIGN KEY (quittance_id)
                                         REFERENCES quittance(id)
                                         ON DELETE RESTRICT,

                                 CONSTRAINT ck_audit_acteur
                                     CHECK (length(trim(acteur)) > 0),

                                 CONSTRAINT ck_audit_emetteur
                                     CHECK (length(trim(emetteur_identite)) > 0),

                                 CONSTRAINT ck_audit_operation
                                     CHECK (length(trim(operation)) > 0),

                                 CONSTRAINT ck_audit_resultat
                                     CHECK (resultat IN ('SUCCES', 'ECHEC'))
);

CREATE INDEX idx_audit_quittance_horodatage
    ON quittance_audit(quittance_id, horodatage, id);