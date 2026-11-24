package com.gestionlocative.loyers.controller;

import com.gestionlocative.loyers.dto.CreatePaiementRequest;
import com.gestionlocative.loyers.dto.EcheanceResponse;
import com.gestionlocative.loyers.dto.PaiementResponse;
import com.gestionlocative.loyers.security.Roles;
import com.gestionlocative.loyers.service.AccesService;
import com.gestionlocative.loyers.service.EcheanceService;
import com.gestionlocative.loyers.service.PaiementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Un paiement enregistré est un paiement confirmé : ces endpoints sont ceux utilisés par le service Quittances.
 */
@RestController
@RequestMapping("/api/paiements")
@Tag(name = "Paiements")
public class PaiementController {

    private final PaiementService paiementService;
    private final EcheanceService echeanceService;
    private final AccesService acces;

    public PaiementController(PaiementService paiementService, EcheanceService echeanceService, AccesService acces) {
        this.paiementService = paiementService;
        this.echeanceService = echeanceService;
        this.acces = acces;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Enregistrer un paiement (total ou partiel) sur une échéance",
            description = "409 si l'échéance est déjà payée, si le montant dépasse le reste à payer ou si la date est dans le futur.")
    public PaiementResponse enregistrer(@Valid @RequestBody CreatePaiementRequest request) {
        return paiementService.enregistrer(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.TOUS)
    @Operation(summary = "Consulter un paiement confirmé")
    public PaiementResponse trouver(@PathVariable UUID id) {
        PaiementResponse p = paiementService.trouver(id);
        EcheanceResponse e = echeanceService.trouver(p.echeanceId());
        acces.verifierBail(e.bailId());
        return p;
    }

    @GetMapping
    @PreAuthorize(Roles.TOUS)
    @Operation(summary = "Historique des paiements d'une échéance (echeanceId) ou d'un bail (bailId)")
    public List<PaiementResponse> lister(@RequestParam(required = false) UUID echeanceId,
                                         @RequestParam(required = false) UUID bailId) {
        if (echeanceId != null) {
            EcheanceResponse e = echeanceService.trouver(echeanceId);
            acces.verifierBail(e.bailId());
            return paiementService.listerParEcheance(echeanceId);
        }
        if (bailId != null) {
            acces.verifierBail(bailId);
            return paiementService.historiqueParBail(bailId);
        }
        throw new IllegalArgumentException("Le paramètre echeanceId ou bailId est obligatoire");
    }
}
