package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.dto.ValidationQuittanceRequest;
import com.gestionlocative.quittances.dto.ValidationQuittanceResponse;
import com.gestionlocative.quittances.service.ValidationQuittanceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class QuittanceValidationController {

    private final ValidationQuittanceService service;

    public QuittanceValidationController(
            ValidationQuittanceService service
    ) {
        this.service = service;
    }

    @PostMapping("/api/v1/quittances/{id}/valider")
    public ValidationQuittanceResponse valider(
            Authentication authentication,
            @PathVariable("id") UUID id,
            @RequestBody ValidationQuittanceRequest demande
    ) {
        return service.valider(
                authentication,
                id,
                demande.confirmation()
        );
    }
}