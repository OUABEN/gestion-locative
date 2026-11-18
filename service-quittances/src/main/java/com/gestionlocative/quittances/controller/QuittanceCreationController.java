package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.dto.CreationQuittanceRequest;
import com.gestionlocative.quittances.dto.CreationQuittanceResponse;
import com.gestionlocative.quittances.service.PreparationQuittanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
        String bailleurId = identifierBailleur(authentication);
        verifierDemande(demande);

        UUID id = preparationService.preparer(
                bailleurId,
                demande.bailId(),
                demande.periodeDebut(),
                demande.periodeFin()
        );

        CreationQuittanceResponse reponse =
                new CreationQuittanceResponse(
                        id,
                        "BROUILLON",
                        "NON_SIGNE"
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    private String identifierBailleur(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt)
                || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Une identité JWT authentifiée est nécessaire."
            );
        }

        boolean estBailleur = authentication.getAuthorities().stream()
                .anyMatch(autorite ->
                        "ROLE_BAILLEUR".equals(autorite.getAuthority())
                );

        if (!estBailleur) {
            throw new AccessDeniedException(
                    "La création est réservée au bailleur."
            );
        }

        Object identifiant = jwt.getToken()
                .getClaims()
                .get("bailleur_id");

        if (!(identifiant instanceof String bailleurId)
                || bailleurId.isBlank()
                || bailleurId.length() > 100
                || !bailleurId.equals(bailleurId.trim())) {
            throw new AccessDeniedException(
                    "L'identifiant du bailleur est absent ou invalide."
            );
        }

        return bailleurId;
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