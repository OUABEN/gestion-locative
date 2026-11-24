package com.gestionlocative.loyers.controller;

import com.gestionlocative.loyers.dto.EcheanceResponse;
import com.gestionlocative.loyers.dto.GenererEcheancesRequest;
import com.gestionlocative.loyers.dto.GenerationResultat;
import com.gestionlocative.loyers.model.StatutEcheance;
import com.gestionlocative.loyers.security.Roles;
import com.gestionlocative.loyers.service.AccesService;
import com.gestionlocative.loyers.service.EcheanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/echeances")
@Tag(name = "Échéances")
public class EcheanceController {

    private final EcheanceService echeanceService;
    private final AccesService acces;

    public EcheanceController(EcheanceService echeanceService, AccesService acces) {
        this.echeanceService = echeanceService;
        this.acces = acces;
    }

    @GetMapping
    @PreAuthorize(Roles.TOUS)
    @Operation(summary = "Lister les échéances (filtres bailId et statut ; le locataire ne voit que les siennes)")
    public List<EcheanceResponse> rechercher(@RequestParam(required = false) UUID bailId,
                                             @RequestParam(required = false) StatutEcheance statut) {
        List<EcheanceResponse> liste = echeanceService.rechercher(bailId, statut);
        if (acces.estLocataire()) {
            Set<UUID> miens = acces.bauxAccessibles();
            liste = liste.stream().filter(e -> miens.contains(e.bailId())).toList();
        }
        return liste;
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.TOUS)
    @Operation(summary = "Consulter une échéance (montant, total versé, reste à payer, date limite, statut)")
    public EcheanceResponse trouver(@PathVariable UUID id) {
        EcheanceResponse e = echeanceService.trouver(id);
        acces.verifierBail(e.bailId());
        return e;
    }

    @PostMapping("/generation")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Générer les échéances d'un mois pour tous les baux actifs (sans doublon)")
    public GenerationResultat genererPourMois(@Valid @RequestBody GenererEcheancesRequest request) {
        return echeanceService.genererPourMois(request.mois(), request.annee());
    }

    @PostMapping("/generation/baux/{bailId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Générer l'échéance d'un seul bail pour un mois")
    public EcheanceResponse genererPourBail(@PathVariable UUID bailId,
                                            @Valid @RequestBody GenererEcheancesRequest request) {
        return echeanceService.genererPourBail(bailId, request.mois(), request.annee());
    }

    @PostMapping("/mise-a-jour-statuts")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Recalculer les statuts (A_PAYER, EN_RETARD, IMPAYEE) à la date du jour")
    public Map<String, Integer> mettreAJourStatuts() {
        return Map.of("modifiees", echeanceService.mettreAJourStatuts());
    }
}
