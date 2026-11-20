package com.gestionlocative.quittances.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class ValidationTraceRepository {

    private final JdbcTemplate jdbcTemplate;

    public ValidationTraceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enregistrer(
            UUID quittanceId,
            String acteur,
            String emetteurIdentite,
            Instant date,
            long versionAvant,
            String statutAvant
    ) {
        jdbcTemplate.update("""
                INSERT INTO quittance_validation (
                    id,
                    quittance_id,
                    acteur,
                    emetteur_identite,
                    valide_le,
                    version_document_avant,
                    statut_avant
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                quittanceId,
                acteur,
                emetteurIdentite,
                Timestamp.from(date),
                versionAvant,
                statutAvant
        );

        jdbcTemplate.update("""
                INSERT INTO quittance_audit (
                    id,
                    quittance_id,
                    acteur,
                    emetteur_identite,
                    operation,
                    resultat,
                    horodatage,
                    statut_avant,
                    statut_apres
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                quittanceId,
                acteur,
                emetteurIdentite,
                "VALIDATION",
                "SUCCES",
                Timestamp.from(date),
                statutAvant,
                "EN_ATTENTE_SIGNATURE"
        );
    }
}