package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.service.EmissionReelleService;
import com.gestionlocative.quittances.service.EmissionReelleTransactions;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quittances/{id}/emission")
public class EmissionReelleController {
    private final EmissionReelleService service;

    public EmissionReelleController(EmissionReelleService service) {
        this.service = service;
    }
    public record Demande(Boolean confirmation) {}

    @PostMapping
    public EmissionReelleTransactions.Resultat emettre(Authentication auth,
            @PathVariable UUID id, @RequestBody Demande demande) {
        return service.emettre(auth, id, demande == null ? null : demande.confirmation());
    }

    @GetMapping("/verification")
    public EmissionReelleTransactions.Verification verifier(Authentication auth,
            @PathVariable UUID id) {
        return service.verifier(auth, id);
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> telechargerPdf(Authentication auth,
            @PathVariable UUID id) {
        byte[] contenu = service.telechargerPdf(auth, id);
        return fichier(contenu, "application/pdf", "quittance-" + id + ".pdf");
    }

    @GetMapping(value = "/preuve", produces = "application/zip")
    public ResponseEntity<byte[]> telechargerPreuve(Authentication auth,
            @PathVariable UUID id) {
        byte[] contenu = service.telechargerPreuve(auth, id);
        return fichier(contenu, "application/zip", "preuve-" + id + ".zip");
    }

    private ResponseEntity<byte[]> fichier(byte[] contenu, String type, String nom) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nom).build().toString())
                .contentType(MediaType.parseMediaType(type)).contentLength(contenu.length)
                .body(contenu);
    }
}
