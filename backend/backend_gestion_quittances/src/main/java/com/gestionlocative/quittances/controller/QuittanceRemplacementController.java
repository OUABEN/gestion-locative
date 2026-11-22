package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.dto.RemplacementQuittanceRequest;
import com.gestionlocative.quittances.dto.RemplacementQuittanceResponse;
import com.gestionlocative.quittances.service.RemplacementQuittanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class QuittanceRemplacementController {

    private final RemplacementQuittanceService service;

    public QuittanceRemplacementController(
            RemplacementQuittanceService service
    ) {
        this.service = service;
    }

    @PostMapping("/api/v1/quittances/{id}/remplacer")
    public ResponseEntity<RemplacementQuittanceResponse> remplacer(
            Authentication authentication,
            @PathVariable("id") UUID id,
            @RequestBody RemplacementQuittanceRequest demande
    ) {
        var resultat = service.preparer(
                authentication,
                id,
                demande.motif(),
                demande.confirmation()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(resultat);
    }
}