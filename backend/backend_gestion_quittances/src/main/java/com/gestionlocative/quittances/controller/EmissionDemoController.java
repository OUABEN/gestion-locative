package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.service.EmissionDemoService;
import com.gestionlocative.quittances.service.EmissionDemoTransactions;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quittances/{id}/emission-demo")
public class EmissionDemoController {
    private final EmissionDemoService service;
    public EmissionDemoController(EmissionDemoService service) { this.service = service; }
    public record Demande(Boolean confirmation) {}
    @PostMapping
    public EmissionDemoTransactions.Resultat emettre(Authentication auth, @PathVariable UUID id, @RequestBody Demande demande) {
        return service.emettre(auth, id, demande.confirmation());
    }
    @GetMapping("/verification")
    public EmissionDemoService.Verification verifier(Authentication auth, @PathVariable UUID id) {
        return service.verifierDocument(auth, id);
    }
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(Authentication auth, @PathVariable UUID id) {
        return fichier(service.telecharger(auth, id, false), "application/pdf", "demo-" + id + ".pdf");
    }
    @GetMapping("/preuve")
    public ResponseEntity<byte[]> preuve(Authentication auth, @PathVariable UUID id) {
        return fichier(service.telecharger(auth, id, true), "application/zip", "preuve-demo-" + id + ".zip");
    }
    private static ResponseEntity<byte[]> fichier(byte[] bytes, String type, String nom) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nom).build().toString())
            .contentType(MediaType.parseMediaType(type)).contentLength(bytes.length).body(bytes);
    }
}
