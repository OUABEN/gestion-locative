package com.gestionlocative.quittances.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class AnnulationTraceRepository {

    private final JdbcTemplate jdbcTemplate;

    public AnnulationTraceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enregistrer(
            UUID quittanceId,
            String acteur,
            String emetteurIdentite,
            Instant date,
            String statutAvant,
            String motif
    ) {
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
                    statut_apres,
                    motif
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                quittanceId,
                acteur,
                emetteurIdentite,
                "ANNULATION",
                "SUCCES",
                Timestamp.from(date),
                statutAvant,
                "ANNULE",
                motif
        );
    }
}