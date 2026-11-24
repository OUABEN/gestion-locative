-- Source : V1__creation_sequence_documentaire.sql
CREATE SEQUENCE numero_document_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    CACHE 1
    NO CYCLE;

COMMENT ON SEQUENCE numero_document_seq IS
    'Compteur global pour la numerotation des documents du service Quittances.';

-- Source : V2__creation_table_quittance.sql
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

-- Source : V3__creation_paiement_affectation.sql
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

-- Source : V4__creation_validation_et_audit.sql
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

-- Source : V5__ajout_motif_audit.sql
ALTER TABLE quittance_audit
    ADD COLUMN motif VARCHAR(2000);

ALTER TABLE quittance_audit
    ADD CONSTRAINT ck_audit_motif_non_vide
        CHECK (
            motif IS NULL
                OR length(trim(motif)) > 0
            );

ALTER TABLE quittance_audit
    ADD CONSTRAINT ck_audit_annulation_motivee
        CHECK (
            operation <> 'ANNULATION'
                OR motif IS NOT NULL
            );

-- Source : V6__creation_archive_brouillon.sql
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

-- Source : V7__emission_demonstration.sql
CREATE TABLE emission_demo (
    quittance_id UUID PRIMARY KEY REFERENCES quittance(id),
    pdf BYTEA NOT NULL CHECK (octet_length(pdf) BETWEEN 1 AND 10485760),
    signature BYTEA NOT NULL CHECK (octet_length(signature) BETWEEN 256 AND 1024),
    certificat BYTEA NOT NULL CHECK (octet_length(certificat) BETWEEN 1 AND 32768),
    sha256 VARCHAR(64) NOT NULL CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    cree_le TIMESTAMPTZ NOT NULL,
    mode VARCHAR(20) NOT NULL DEFAULT 'DEMONSTRATION' CHECK (mode = 'DEMONSTRATION')
);
COMMENT ON TABLE emission_demo IS 'PDF immuable et preuve locale de test. Ne constitue pas une signature qualifiée.';

-- Source : V8__envois_mailpit.sql
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
