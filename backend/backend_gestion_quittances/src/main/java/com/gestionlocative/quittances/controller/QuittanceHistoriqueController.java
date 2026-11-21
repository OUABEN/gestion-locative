package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.dto.PageHistoriqueResponse;
import com.gestionlocative.quittances.service.HistoriqueQuittanceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class QuittanceHistoriqueController {

    private final HistoriqueQuittanceService service;

    public QuittanceHistoriqueController(
            HistoriqueQuittanceService service
    ) {
        this.service = service;
    }

    @GetMapping("/api/v1/quittances/{id}/historique")
    public PageHistoriqueResponse consulter(
            Authentication authentication,
            @PathVariable("id") UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "taille", defaultValue = "20") int taille
    ) {
        return service.consulter(
                authentication,
                id,
                page,
                taille
        );
    }
}