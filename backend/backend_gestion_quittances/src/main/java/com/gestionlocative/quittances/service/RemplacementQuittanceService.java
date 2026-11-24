package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.dto.RemplacementQuittanceResponse;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.security.IdentiteAudit;
import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
    private final IdentiteAudit identiteAudit;
    private final QuittanceRepository repository;

    public RemplacementQuittanceService(
            ConsultationQuittanceService consultationService,
            ConstructionBrouillonService constructionService,
            EnregistrementRemplacementService enregistrementService,
            IdentiteAudit identiteAudit,
            QuittanceRepository repository
    ) {
        this.consultationService = consultationService;
        this.constructionService = constructionService;
        this.enregistrementService = enregistrementService;
        this.identiteAudit = identiteAudit;
        this.repository = repository;
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

        if (!lecteur.peutEcrire()) {
            throw new AccessDeniedException(
                    "Le remplacement est réservé au bailleur ou au gestionnaire."
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

        IdentiteAudit.Acteur contexteAudit =
                identiteAudit.depuis(authentication);
        String acteur = contexteAudit.sujet();
        String emetteur = contexteAudit.emetteurIdentite();

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

        String bailleurId = lecteur.gestionnaire()
                ? repository.findById(precedentId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Document introuvable."))
                        .getBailleurId()
                : lecteur.identifiant();

        var preparation = constructionService.construire(
                bailleurId,
                precedent.bailId(),
                precedent.periodeDebut(),
                precedent.periodeFin()
        );

        UUID nouveauId = enregistrementService.enregistrer(
                precedentId,
                bailleurId,
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