package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.dto.EtatServiceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EtatServiceController {

    @GetMapping({"/api/v1/systeme/etat", "/health"})
    public EtatServiceResponse consulterEtat() {
        return new EtatServiceResponse(
                "service-quittances",
                "OPERATIONNEL"
        );
    }
}