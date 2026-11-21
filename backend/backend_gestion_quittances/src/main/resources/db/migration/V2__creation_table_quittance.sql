CREATE TABLE quittance (
                           id UUID PRIMARY KEY,

                           numero VARCHAR(40),

                           bail_id VARCHAR(100) NOT NULL,
                           locataire_id VARCHAR(100) NOT NULL,
                           bien_id VARCHAR(100) NOT NULL,
                           bailleur_id VARCHAR(100) NOT NULL,

                           periode_debut DATE NOT NULL,
                           periode_fin DATE NOT NULL,

                           loyer_du NUMERIC(12,2) NOT NULL,
                           charges_dues NUMERIC(12,2) NOT NULL,
                           loyer_paye NUMERIC(12,2) NOT NULL,
                           charges_payees NUMERIC(12,2) NOT NULL,
                           total_paye NUMERIC(12,2) NOT NULL,

                           devise VARCHAR(3) NOT NULL DEFAULT 'MAD',

                           type_document VARCHAR(30) NOT NULL,
                           statut VARCHAR(30) NOT NULL DEFAULT 'BROUILLON',
                           statut_signature VARCHAR(30) NOT NULL DEFAULT 'NON_SIGNE',

                           cree_le TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           emis_le TIMESTAMP WITH TIME ZONE,

                           pdf_reference VARCHAR(255),
                           pdf_sha256 VARCHAR(64),

                           motif_annulation VARCHAR(2000),

                           document_precedent_id UUID,
                           motif_remplacement VARCHAR(2000),

                           donnees_figees JSONB NOT NULL,

                           version BIGINT NOT NULL DEFAULT 0,

                           CONSTRAINT uq_quittance_numero
                               UNIQUE (numero),

                           CONSTRAINT fk_quittance_document_precedent
                               FOREIGN KEY (document_precedent_id)
                                   REFERENCES quittance (id)
                                   ON DELETE RESTRICT,

                           CONSTRAINT ck_quittance_identifiants
                               CHECK (
                                   LENGTH(BTRIM(bail_id)) > 0
                                       AND LENGTH(BTRIM(locataire_id)) > 0
                                       AND LENGTH(BTRIM(bien_id)) > 0
                                       AND LENGTH(BTRIM(bailleur_id)) > 0
                                   ),

                           CONSTRAINT ck_quittance_periode
                               CHECK (periode_fin >= periode_debut),

                           CONSTRAINT ck_quittance_montants
                               CHECK (
                                   loyer_du >= 0
                                       AND charges_dues >= 0
                                       AND loyer_paye >= 0
                                       AND charges_payees >= 0
                                       AND total_paye > 0
                                   ),

                           CONSTRAINT ck_quittance_total
                               CHECK (total_paye = loyer_paye + charges_payees),

                           CONSTRAINT ck_quittance_plafonds
                               CHECK (
                                   loyer_paye <= loyer_du
                                       AND charges_payees <= charges_dues
                                   ),

                           CONSTRAINT ck_quittance_devise
                               CHECK (devise = 'MAD'),

                           CONSTRAINT ck_quittance_type
                               CHECK (
                                   type_document IN (
                                                     'QUITTANCE_INTEGRALE',
                                                     'RECU_PARTIEL'
                                       )
                                   ),

                           CONSTRAINT ck_quittance_type_montants
                               CHECK (
                                   (
                                       type_document = 'QUITTANCE_INTEGRALE'
                                           AND loyer_paye = loyer_du
                                           AND charges_payees = charges_dues
                                       )
                                       OR
                                   (
                                       type_document = 'RECU_PARTIEL'
                                           AND total_paye < loyer_du + charges_dues
                                       )
                                   ),

                           CONSTRAINT ck_quittance_statut
                               CHECK (
                                   statut IN (
                                              'BROUILLON',
                                              'EN_ATTENTE_VALIDATION',
                                              'EN_ATTENTE_SIGNATURE',
                                              'EMIS',
                                              'A_REEXAMINER',
                                              'ANNULE',
                                              'REMPLACE'
                                       )
                                   ),

                           CONSTRAINT ck_quittance_signature
                               CHECK (
                                   statut_signature IN (
                                                        'NON_SIGNE',
                                                        'EN_ATTENTE',
                                                        'SIGNE_VERIFIE',
                                                        'A_REEXAMINER'
                                       )
                                   ),

                           CONSTRAINT ck_quittance_numero_non_vide
                               CHECK (
                                   numero IS NULL
                                       OR LENGTH(BTRIM(numero)) > 0
                                   ),

                           CONSTRAINT ck_quittance_emission_coherente
                               CHECK (
                                   (
                                       emis_le IS NULL
                                           AND numero IS NULL
                                       )
                                       OR
                                   (
                                       emis_le IS NOT NULL
                                           AND numero IS NOT NULL
                                           AND pdf_reference IS NOT NULL
                                           AND pdf_sha256 IS NOT NULL
                                       )
                                   ),

                           CONSTRAINT ck_quittance_emis
                               CHECK (
                                   statut <> 'EMIS'
                                       OR (
                                       emis_le IS NOT NULL
                                           AND statut_signature = 'SIGNE_VERIFIE'
                                       )
                                   ),

                           CONSTRAINT ck_quittance_date_emission
                               CHECK (
                                   emis_le IS NULL
                                       OR emis_le >= cree_le
                                   ),

                           CONSTRAINT ck_quittance_pdf_reference
                               CHECK (
                                   pdf_reference IS NULL
                                       OR LENGTH(BTRIM(pdf_reference)) > 0
                                   ),

                           CONSTRAINT ck_quittance_pdf_sha256
                               CHECK (
                                   pdf_sha256 IS NULL
                                       OR pdf_sha256 ~ '^[0-9a-f]{64}$'
),

    CONSTRAINT ck_quittance_annulation
        CHECK (
            statut <> 'ANNULE'
            OR (
                motif_annulation IS NOT NULL
                AND LENGTH(BTRIM(motif_annulation)) > 0
            )
        ),

    CONSTRAINT ck_quittance_remplacement
        CHECK (
            (
                document_precedent_id IS NULL
                AND motif_remplacement IS NULL
            )
            OR
            (
                document_precedent_id IS NOT NULL
                AND motif_remplacement IS NOT NULL
                AND LENGTH(BTRIM(motif_remplacement)) > 0
            )
        ),

    CONSTRAINT ck_quittance_pas_auto_remplacement
        CHECK (
            document_precedent_id IS NULL
            OR document_precedent_id <> id
        ),

    CONSTRAINT ck_quittance_snapshot
        CHECK (
            JSONB_TYPEOF(donnees_figees) = 'object'
            AND donnees_figees <> '{}'::JSONB
        ),

    CONSTRAINT ck_quittance_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX uq_quittance_integrale_active
    ON quittance (bail_id, periode_debut, periode_fin)
    WHERE type_document = 'QUITTANCE_INTEGRALE'
      AND statut IN (
          'BROUILLON',
          'EN_ATTENTE_VALIDATION',
          'EN_ATTENTE_SIGNATURE',
          'EMIS',
          'A_REEXAMINER'
      );

CREATE UNIQUE INDEX uq_quittance_remplacement_actif
    ON quittance (document_precedent_id)
    WHERE document_precedent_id IS NOT NULL
      AND statut NOT IN ('ANNULE', 'REMPLACE');

CREATE INDEX idx_quittance_bailleur_creation
    ON quittance (bailleur_id, cree_le DESC, id);

CREATE INDEX idx_quittance_locataire_creation
    ON quittance (locataire_id, cree_le DESC, id);

CREATE INDEX idx_quittance_bien_periode
    ON quittance (bien_id, periode_debut, periode_fin);

CREATE INDEX idx_quittance_bail_periode
    ON quittance (bail_id, periode_debut, periode_fin);

CREATE INDEX idx_quittance_statut_creation
    ON quittance (statut, cree_le DESC, id);