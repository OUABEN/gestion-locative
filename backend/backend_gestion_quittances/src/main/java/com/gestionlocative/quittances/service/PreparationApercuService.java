package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.dto.DonneesApercu;
import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

@Service
public class PreparationApercuService {

    private static final Set<String> STATUTS_AUTORISES = Set.of(
            "BROUILLON",
            "EN_ATTENTE_VALIDATION",
            "EN_ATTENTE_SIGNATURE"
    );

    private final ConsultationQuittanceService consultationService;
    private final ConsultationAffectationService affectationService;

    public PreparationApercuService(
            ConsultationQuittanceService consultationService,
            ConsultationAffectationService affectationService
    ) {
        this.consultationService = consultationService;
        this.affectationService = affectationService;
    }

    @Transactional(readOnly = true)
    public DonneesApercu charger(
            Authentication authentication,
            UUID id
    ) {
        LecteurQuittances lecteur =
                LecteurQuittances.depuis(authentication);

        if (!lecteur.peutEcrire()) {
            throw new AccessDeniedException(
                    "La prévisualisation est réservée au bailleur ou au gestionnaire."
            );
        }

        var document = consultationService.consulter(authentication, id);

        if (document.emisLe() != null
                || !STATUTS_AUTORISES.contains(document.statut())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ce document ne peut plus être prévisualisé "
                            + "comme un brouillon."
            );
        }

        var affectations = affectationService.consulter(authentication, id);

        return new DonneesApercu(document, affectations);
    }
}