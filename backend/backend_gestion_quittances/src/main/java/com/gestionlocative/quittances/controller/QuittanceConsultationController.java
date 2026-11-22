package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.dto.AffectationQuittanceResponse;
import com.gestionlocative.quittances.dto.PageQuittancesResponse;
import com.gestionlocative.quittances.dto.QuittanceConsultationResponse;
import com.gestionlocative.quittances.service.ConsultationAffectationService;
import com.gestionlocative.quittances.service.ConsultationQuittanceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class QuittanceConsultationController {

    private final ConsultationQuittanceService consultationService;
    private final ConsultationAffectationService affectationService;

    public QuittanceConsultationController(
            ConsultationQuittanceService consultationService,
            ConsultationAffectationService affectationService
    ) {
        this.consultationService = consultationService;
        this.affectationService = affectationService;
    }

    @GetMapping("/api/v1/quittances")
    public PageQuittancesResponse lister(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "taille", defaultValue = "20") int taille,
            @RequestParam(name = "tri", defaultValue = "creeLe") String tri,
            @RequestParam(name = "sens", defaultValue = "desc") String sens,
            @RequestParam(name = "bailId", required = false) String bailId,
            @RequestParam(name = "numero", required = false) String numero,
            @RequestParam(name = "statut", required = false) String statut,
            @RequestParam(name = "debut", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate debut,
            @RequestParam(name = "fin", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fin
    ) {
        return consultationService.lister(
                authentication,
                page,
                taille,
                tri,
                sens, bailId, numero, statut, debut, fin
        );
    }

    @GetMapping("/api/v1/quittances/{id}")
    public QuittanceConsultationResponse consulter(
            Authentication authentication,
            @PathVariable("id") UUID id
    ) {
        return consultationService.consulter(authentication, id);
    }

    @GetMapping("/api/v1/quittances/{id}/affectations")
    public List<AffectationQuittanceResponse> consulterAffectations(
            Authentication authentication,
            @PathVariable("id") UUID id
    ) {
        return affectationService.consulter(authentication, id);
    }
}