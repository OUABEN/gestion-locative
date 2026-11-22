package com.gestionlocative.quittances.repository;

import com.gestionlocative.quittances.dto.EvenementHistoriqueResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class HistoriqueQuittanceRepository {

    private final JdbcTemplate jdbcTemplate;

    public HistoriqueQuittanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long compter(UUID quittanceId) {
        Long nombre = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM quittance_audit
                WHERE quittance_id = ?
                """,
                Long.class,
                quittanceId
        );

        return nombre.longValue();
    }

    public List<EvenementHistoriqueResponse> consulter(
            UUID quittanceId,
            int taille,
            long decalage
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    operation,
                    resultat,
                    horodatage,
                    statut_avant,
                    statut_apres,
                    motif
                FROM quittance_audit
                WHERE quittance_id = ?
                ORDER BY horodatage ASC, id ASC
                LIMIT ? OFFSET ?
                """,
                (resultat, numeroLigne) ->
                        new EvenementHistoriqueResponse(
                                resultat.getObject("id", UUID.class),
                                resultat.getString("operation"),
                                resultat.getString("resultat"),
                                resultat.getTimestamp("horodatage").toInstant(),
                                resultat.getString("statut_avant"),
                                resultat.getString("statut_apres"),
                                resultat.getString("motif")
                        ),
                quittanceId,
                taille,
                decalage
        );
    }
}