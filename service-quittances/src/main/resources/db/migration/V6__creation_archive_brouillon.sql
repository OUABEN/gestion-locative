CREATE TABLE document_fichier (
                                  id UUID PRIMARY KEY,
                                  quittance_id UUID NOT NULL,
                                  version_document BIGINT NOT NULL,
                                  nature VARCHAR(30) NOT NULL,
                                  reference_stockage VARCHAR(36) NOT NULL,
                                  sha256 VARCHAR(64) NOT NULL,
                                  taille_octets BIGINT NOT NULL,
                                  cree_le TIMESTAMPTZ NOT NULL,

                                  CONSTRAINT fk_document_fichier_quittance
                                      FOREIGN KEY (quittance_id)
                                          REFERENCES quittance(id)
                                          ON DELETE RESTRICT,

                                  CONSTRAINT uq_document_fichier_reference
                                      UNIQUE (reference_stockage),

                                  CONSTRAINT uq_archive_brouillon_version
                                      UNIQUE (quittance_id, version_document, nature),

                                  CONSTRAINT ck_document_fichier_version
                                      CHECK (version_document >= 0),

                                  CONSTRAINT ck_document_fichier_nature
                                      CHECK (nature = 'BROUILLON'),

                                  CONSTRAINT ck_document_fichier_reference
                                      CHECK (
                                          reference_stockage ~
                                          '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
),

    CONSTRAINT ck_document_fichier_sha256
        CHECK (sha256 ~ '^[0-9a-f]{64}$'),

    CONSTRAINT ck_document_fichier_taille
        CHECK (taille_octets > 0)
);