package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.pdf.ApercuPdfService;
import com.gestionlocative.quittances.service.PreparationApercuService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
public class QuittanceApercuController {

    private final PreparationApercuService preparationService;
    private final ApercuPdfService pdfService;

    public QuittanceApercuController(
            PreparationApercuService preparationService,
            ApercuPdfService pdfService
    ) {
        this.preparationService = preparationService;
        this.pdfService = pdfService;
    }

    @GetMapping(
            value = "/api/v1/quittances/{id}/preview",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> previsualiser(
            Authentication authentication,
            @PathVariable("id") UUID id
    ) throws IOException {
        var donnees = preparationService.charger(authentication, id);
        byte[] contenu = pdfService.generer(donnees);

        String disposition = ContentDisposition.inline()
                .filename("brouillon-" + id + ".pdf")
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(contenu.length)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(contenu);
    }
}