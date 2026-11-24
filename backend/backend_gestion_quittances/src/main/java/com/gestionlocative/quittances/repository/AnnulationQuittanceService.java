package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.dto.AnnulationQuittanceResponse;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.StatutQuittance;
import com.gestionlocative.quittances.repository.AnnulationTraceRepository;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.security.IdentiteAudit;
import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class AnnulationQuittanceService {

    private final QuittanceRepository repository;
    private final AnnulationTraceRepository traceRepository;
    private final IdentiteAudit identiteAudit;

    public AnnulationQuittanceService(
            QuittanceRepository repository,
            AnnulationTraceRepository traceRepository,
            IdentiteAudit identiteAudit
    ) {
        this.repository = repository;
        this.traceRepository = traceRepository;
        this.identiteAudit = identiteAudit;
    }

    @Transactional(timeout = 10)
    public AnnulationQuittanceResponse annuler(
            Authentication authentication,
            UUID id,
            String motif,
            Boolean confirmation
    ) {
        LecteurQuittances lecteur =
                LecteurQuittances.depuis(authentication);

        if (!lecteur.peutEcrire()) {
            throw new AccessDeniedException(
                    "L'annulation est réservée au bailleur ou au gestionnaire."
            );
        }

        if (!Boolean.TRUE.equals(confirmation)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La confirmation explicite est obligatoire."
            );
        }

        if (motif == null
                || motif.isBlank()
                || motif.strip().length() > 2000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Un motif de 1 à 2000 caractères est obligatoire."
            );
        }

        IdentiteAudit.Acteur contexteAudit =
                identiteAudit.depuis(authentication);
        String acteur = contexteAudit.sujet();
        String emetteur = contexteAudit.emetteurIdentite();

        Quittance document = (lecteur.gestionnaire()
                ? repository.trouverPourModificationGestionnaire(id)
                : repository.trouverPourModification(id, lecteur.identifiant())
        ).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document introuvable."
                )
        );

        if (document.getStatut() == StatutQuittance.ANNULE
                || document.getStatut() == StatutQuittance.REMPLACE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ce document est déjà annulé ou remplacé."
            );
        }

        String statutAvant = document.getStatut().name();
        String motifNormalise = motif.strip();
        Instant date = Instant.now();

        document.annuler(motifNormalise);
        repository.saveAndFlush(document);

        traceRepository.enregistrer(
                document.getId(),
                acteur,
                emetteur,
                date,
                statutAvant,
                motifNormalise
        );

        return new AnnulationQuittanceResponse(
                document.getId(),
                document.getStatut().name(),
                document.getStatutSignature().name(),
                motifNormalise,
                date
        );
    }
}