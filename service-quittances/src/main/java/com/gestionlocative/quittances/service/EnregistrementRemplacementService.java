package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.StatutQuittance;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
public class EnregistrementRemplacementService {

    private final QuittanceRepository repository;
    private final EnregistrementBrouillonService enregistrementService;
    private final JdbcTemplate jdbcTemplate;

    public EnregistrementRemplacementService(
            QuittanceRepository repository,
            EnregistrementBrouillonService enregistrementService,
            JdbcTemplate jdbcTemplate
    ) {
        this.repository = repository;
        this.enregistrementService = enregistrementService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(timeout = 10)
    public UUID enregistrer(
            UUID precedentId,
            String bailleurId,
            String acteur,
            String emetteur,
            String motif,
            ConstructionBrouillonService.Resultat preparation
    ) {
        Quittance precedent = repository.trouverPourModification(
                precedentId,
                bailleurId
        ).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document précédent introuvable."
                )
        );

        if (precedent.getStatut() != StatutQuittance.ANNULE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le document précédent doit être annulé."
            );
        }

        Quittance nouveau = preparation.document();

        if (!precedent.getBailId().equals(nouveau.getBailId())
                || !precedent.getPeriodeDebut()
                .equals(nouveau.getPeriodeDebut())
                || !precedent.getPeriodeFin()
                .equals(nouveau.getPeriodeFin())
                || !bailleurId.equals(nouveau.getBailleurId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le remplacement doit concerner le même bail, "
                            + "la même période et le même bailleur."
            );
        }

        nouveau.rattacherDocumentPrecedent(precedentId, motif);

        UUID nouveauId = enregistrementService.enregistrer(
                nouveau,
                preparation.affectations()
        );

        Instant maintenant = Instant.now();

        tracer(
                precedentId,
                acteur,
                emetteur,
                maintenant,
                "REMPLACEMENT_PREPARE",
                "ANNULE",
                "ANNULE",
                motif
        );

        tracer(
                nouveauId,
                acteur,
                emetteur,
                maintenant,
                "CREATION_REMPLACEMENT",
                null,
                "BROUILLON",
                motif
        );

        return nouveauId;
    }

    private void tracer(
            UUID documentId,
            String acteur,
            String emetteur,
            Instant date,
            String operation,
            String statutAvant,
            String statutApres,
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
                documentId,
                acteur,
                emetteur,
                operation,
                "SUCCES",
                Timestamp.from(date),
                statutAvant,
                statutApres,
                motif
        );
    }
}