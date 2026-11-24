package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.dto.CreationQuittanceRequest;
import com.gestionlocative.quittances.dto.CreationQuittanceResponse;
import com.gestionlocative.quittances.security.LecteurQuittances;
import com.gestionlocative.quittances.service.PreparationQuittanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
public class QuittanceCreationController {

    private final PreparationQuittanceService preparationService;

    public QuittanceCreationController(
            PreparationQuittanceService preparationService
    ) {
        this.preparationService = preparationService;
    }

    @PostMapping("/api/v1/quittances")
    public ResponseEntity<CreationQuittanceResponse> creer(
            @RequestBody CreationQuittanceRequest demande,
            Authentication authentication
    ) {
        LecteurQuittances lecteur =
                LecteurQuittances.depuis(authentication);

        if (!lecteur.peutEcrire()) {
            throw new AccessDeniedException(
                    "Creation reservee au bailleur et au gestionnaire.");
        }

        verifierDemande(demande);

        UUID id;

        if (lecteur.gestionnaire()) {
            id = preparationService.preparerPourGestionnaire(
                    authentication,
                    demande.bailId(),
                    demande.periodeDebut(),
                    demande.periodeFin()
            );
        } else {
            id = preparationService.preparer(
                    lecteur.identifiant(),
                    demande.bailId(),
                    demande.periodeDebut(),
                    demande.periodeFin()
            );
        }

        CreationQuittanceResponse reponse =
                new CreationQuittanceResponse(
                        id,
                        "BROUILLON",
                        "NON_SIGNE"
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }


    private void verifierDemande(CreationQuittanceRequest demande) {
        if (demande == null
                || demande.bailId() == null
                || demande.bailId().isBlank()
                || demande.bailId().length() > 100
                || !demande.bailId().equals(demande.bailId().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L'identifiant du bail est obligatoire et limité à 100 caractères."
            );
        }

        if (demande.periodeDebut() == null
                || demande.periodeFin() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Les dates de début et de fin sont obligatoires."
            );
        }

        if (demande.periodeFin().isBefore(demande.periodeDebut())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fin de période doit être postérieure ou égale au début."
            );
        }
    }
}