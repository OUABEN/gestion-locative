package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.dto.EtatServiceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/systeme")
public class EtatServiceController {

    @GetMapping("/etat")
    public EtatServiceResponse consulterEtat() {
        return new EtatServiceResponse(
                "service-quittances",
                "OPERATIONNEL"
        );
    }
}