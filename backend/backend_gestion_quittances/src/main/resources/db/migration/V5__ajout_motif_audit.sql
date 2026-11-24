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