package com.gestionlocative.quittances.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class PreparationQuittanceService {

    private final ConstructionBrouillonService constructionService;
    private final EnregistrementBrouillonService enregistrementService;

    public PreparationQuittanceService(
            ConstructionBrouillonService constructionService,
            EnregistrementBrouillonService enregistrementService
    ) {
        this.constructionService = constructionService;
        this.enregistrementService = enregistrementService;
    }

    @Transactional(propagation = Propagation.NEVER)
    public UUID preparer(
            String bailleurAuthentifieId,
            String bailId,
            LocalDate debut,
            LocalDate fin
    ) {
        var resultat = constructionService.construire(
                bailleurAuthentifieId,
                bailId,
                debut,
                fin
        );

        return enregistrementService.enregistrer(
                resultat.document(),
                resultat.affectations()
        );
    }
}