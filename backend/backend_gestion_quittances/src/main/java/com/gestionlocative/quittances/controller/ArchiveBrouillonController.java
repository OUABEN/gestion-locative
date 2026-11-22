package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.dto.ArchiveBrouillonResponse;
import com.gestionlocative.quittances.dto.PageArchivesBrouillonResponse;
import com.gestionlocative.quittances.service.ConsultationArchivesBrouillonService;
import org.springframework.web.bind.annotation.RequestParam;
import com.gestionlocative.quittances.service.ArchivageBrouillonService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
public class ArchiveBrouillonController {

    private final ArchivageBrouillonService service;

    private final ConsultationArchivesBrouillonService consultation;

    public ArchiveBrouillonController(
            ArchivageBrouillonService service,
            ConsultationArchivesBrouillonService consultation) {
        this.service = service;
        this.consultation = consultation;
    }

    @GetMapping("/api/v1/quittances/{id}/archives-brouillon")
    public ResponseEntity<PageArchivesBrouillonResponse> consulter(
            Authentication authentication,
            @PathVariable("id") UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "taille", defaultValue = "10") int taille) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(consultation.consulter(authentication, id, page, taille));
    }

    @PostMapping("/api/v1/quittances/{id}/archives-brouillon")
    public ResponseEntity<ArchiveBrouillonResponse> archiver(
            Authentication authentication,
            @PathVariable("id") UUID id
    ) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.archiver(authentication, id));
    }

    @GetMapping(
            value = "/api/v1/quittances/{id}/archives-brouillon/{archiveId}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> telecharger(
            Authentication authentication,
            @PathVariable("id") UUID id,
            @PathVariable("archiveId") UUID archiveId
    ) {
        byte[] contenu = service.telecharger(
                authentication,
                id,
                archiveId
        );

        String disposition = ContentDisposition.attachment()
                .filename("brouillon-" + id + "-" + archiveId + ".pdf")
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