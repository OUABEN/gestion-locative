package com.gestionlocative.loyers.controller;

import com.gestionlocative.loyers.dto.CreatePaiementRequest;
import com.gestionlocative.loyers.dto.PaiementResponse;
import com.gestionlocative.loyers.service.PaiementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paiements")
@PreAuthorize("hasAnyRole('BAILLEUR','GESTIONNAIRE')")
public class PaiementController {

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaiementResponse enregistrer(@Valid @RequestBody CreatePaiementRequest request) {
        return paiementService.enregistrer(request);
    }

    @GetMapping("/{id}")
    public PaiementResponse trouver(@PathVariable UUID id) {
        return paiementService.trouver(id);
    }

    /** Historique : filtrer par échéance OU par bail. */
    @GetMapping
    public List<PaiementResponse> lister(@RequestParam(required = false) UUID echeanceId,
                                         @RequestParam(required = false) UUID bailId) {
        if (echeanceId != null) {
            return paiementService.listerParEcheance(echeanceId);
        }
        if (bailId != null) {
            return paiementService.historiqueParBail(bailId);
        }
        throw new IllegalArgumentException("Le paramètre echeanceId ou bailId est obligatoire");
    }
}
