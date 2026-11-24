package com.gestionlocative.loyers.controller;

import com.gestionlocative.loyers.dto.BailResponse;
import com.gestionlocative.loyers.dto.CreateBailRequest;
import com.gestionlocative.loyers.dto.TerminerBailRequest;
import com.gestionlocative.loyers.model.StatutBail;
import com.gestionlocative.loyers.security.Roles;
import com.gestionlocative.loyers.service.AccesService;
import com.gestionlocative.loyers.service.BailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/baux")
@Tag(name = "Baux")
public class BailController {

    private final BailService bailService;
    private final AccesService acces;

    public BailController(BailService bailService, AccesService acces) {
        this.bailService = bailService;
        this.acces = acces;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Créer un bail",
            description = "Vérifie le locataire et la disponibilité du bien, enregistre le bail, passe le bien à OCCUPE "
                    + "et génère l'échéance du mois. 409 si le bien est déjà loué.")
    public BailResponse creer(@Valid @RequestBody CreateBailRequest request) {
        return bailService.creer(request);
    }

    @GetMapping
    @PreAuthorize(Roles.TOUS)
    @Operation(summary = "Lister les baux (le locataire ne voit que les siens)")
    public List<BailResponse> lister(@RequestParam(required = false) StatutBail statut,
                                     @RequestParam(required = false) UUID bienId,
                                     @RequestParam(required = false) UUID locataireId) {
        List<BailResponse> baux = bailService.lister(statut, bienId, locataireId);
        if (acces.estLocataire()) {
            Set<UUID> miens = acces.bauxAccessibles();
            baux = baux.stream().filter(b -> miens.contains(b.id())).toList();
        }
        return baux;
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.TOUS)
    @Operation(summary = "Consulter un bail")
    public BailResponse trouver(@PathVariable UUID id) {
        BailResponse bail = bailService.trouver(id);
        acces.verifierBail(bail.id());
        return bail;
    }

    @PostMapping("/{id}/terminer")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Terminer un bail", description = "Passe le bail à TERMINE et remet le bien à LIBRE.")
    public BailResponse terminer(@PathVariable UUID id,
                                 @RequestBody(required = false) TerminerBailRequest request) {
        return bailService.terminer(id, request != null ? request.dateFin() : null);
    }
}
