package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.dto.AnnulationQuittanceRequest;
import com.gestionlocative.quittances.dto.AnnulationQuittanceResponse;
import com.gestionlocative.quittances.service.AnnulationQuittanceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class QuittanceAnnulationController {

    private final AnnulationQuittanceService service;

    public QuittanceAnnulationController(
            AnnulationQuittanceService service
    ) {
        this.service = service;
    }

    @PostMapping("/api/v1/quittances/{id}/annuler")
    public AnnulationQuittanceResponse annuler(
            Authentication authentication,
            @PathVariable("id") UUID id,
            @RequestBody AnnulationQuittanceRequest demande
    ) {
        return service.annuler(
                authentication,
                id,
                demande.motif(),
                demande.confirmation()
        );
    }
}