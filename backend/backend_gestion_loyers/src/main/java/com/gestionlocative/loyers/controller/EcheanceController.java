package com.gestionlocative.loyers.controller;

import com.gestionlocative.loyers.dto.EcheanceResponse;
import com.gestionlocative.loyers.dto.GenererEcheancesRequest;
import com.gestionlocative.loyers.dto.GenerationResultat;
import com.gestionlocative.loyers.model.StatutEcheance;
import com.gestionlocative.loyers.service.EcheanceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/echeances")
@PreAuthorize("hasAnyRole('BAILLEUR','GESTIONNAIRE')")
public class EcheanceController {

    private final EcheanceService echeanceService;

    public EcheanceController(EcheanceService echeanceService) {
        this.echeanceService = echeanceService;
    }

    @GetMapping
    public List<EcheanceResponse> rechercher(@RequestParam(required = false) UUID bailId,
                                             @RequestParam(required = false) StatutEcheance statut) {
        return echeanceService.rechercher(bailId, statut);
    }

    @GetMapping("/{id}")
    public EcheanceResponse trouver(@PathVariable UUID id) {
        return echeanceService.trouver(id);
    }

    @PostMapping("/generation")
    public GenerationResultat genererPourMois(@Valid @RequestBody GenererEcheancesRequest request) {
        return echeanceService.genererPourMois(request.mois(), request.annee());
    }

    @PostMapping("/generation/baux/{bailId}")
    @ResponseStatus(HttpStatus.CREATED)
    public EcheanceResponse genererPourBail(@PathVariable UUID bailId,
                                            @Valid @RequestBody GenererEcheancesRequest request) {
        return echeanceService.genererPourBail(bailId, request.mois(), request.annee());
    }

    @PostMapping("/mise-a-jour-statuts")
    public Map<String, Integer> mettreAJourStatuts() {
        return Map.of("modifiees", echeanceService.mettreAJourStatuts());
    }
}
