package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.dto.RemplacementQuittanceResponse;
import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class RemplacementQuittanceService {

    private final ConsultationQuittanceService consultationService;
    private final ConstructionBrouillonService constructionService;
    private final EnregistrementRemplacementService enregistrementService;

    public RemplacementQuittanceService(
            ConsultationQuittanceService consultationService,
            ConstructionBrouillonService constructionService,
            EnregistrementRemplacementService enregistrementService
    ) {
        this.consultationService = consultationService;
        this.constructionService = constructionService;
        this.enregistrementService = enregistrementService;
    }

    @Transactional(propagation = Propagation.NEVER)
    public RemplacementQuittanceResponse preparer(
            Authentication authentication,
            UUID precedentId,
            String motif,
            Boolean confirmation
    ) {
        LecteurQuittances lecteur =
                LecteurQuittances.depuis(authentication);

        if (!lecteur.bailleur()) {
            throw new AccessDeniedException(
                    "Le remplacement est réservé au bailleur."
            );
        }

        if (!Boolean.TRUE.equals(confirmation)
                || motif == null
                || motif.isBlank()
                || motif.strip().length() > 2000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Une confirmation et un motif de 1 à 2000 caractères "
                            + "sont obligatoires."
            );
        }

        JwtAuthenticationToken jwt =
                (JwtAuthenticationToken) authentication;

        String acteur = jwt.getToken().getSubject();
        String emetteur = jwt.getToken().getClaimAsString("iss");

        if (acteur == null
                || acteur.isBlank()
                || acteur.length() > 200
                || emetteur == null
                || emetteur.isBlank()
                || emetteur.length() > 500) {
            throw new AccessDeniedException(
                    "L'identité ne permet pas une traçabilité suffisante."
            );
        }

        var precedent = consultationService.consulter(
                authentication,
                precedentId
        );

        if (!"ANNULE".equals(precedent.statut())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Annulez d'abord le document avec un motif."
            );
        }

        var preparation = constructionService.construire(
                lecteur.identifiant(),
                precedent.bailId(),
                precedent.periodeDebut(),
                precedent.periodeFin()
        );

        UUID nouveauId = enregistrementService.enregistrer(
                precedentId,
                lecteur.identifiant(),
                acteur,
                emetteur,
                motif.strip(),
                preparation
        );

        return new RemplacementQuittanceResponse(
                nouveauId,
                precedentId,
                "BROUILLON",
                "NON_SIGNE"
        );
    }
}