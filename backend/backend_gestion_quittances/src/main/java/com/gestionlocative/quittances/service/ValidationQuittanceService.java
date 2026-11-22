package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.dto.ValidationQuittanceResponse;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.StatutQuittance;
import com.gestionlocative.quittances.entity.StatutSignature;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.repository.ValidationTraceRepository;
import com.gestionlocative.quittances.security.IdentiteAudit;
import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class ValidationQuittanceService {

    private final QuittanceRepository repository;
    private final ValidationTraceRepository traceRepository;
    private final IdentiteAudit identiteAudit;

    public ValidationQuittanceService(
            QuittanceRepository repository,
            ValidationTraceRepository traceRepository,
            IdentiteAudit identiteAudit
    ) {
        this.repository = repository;
        this.traceRepository = traceRepository;
        this.identiteAudit = identiteAudit;
    }

    @Transactional(timeout = 10)
    public ValidationQuittanceResponse valider(
            Authentication authentication,
            UUID id,
            Boolean confirmation
    ) {
        LecteurQuittances lecteur =
                LecteurQuittances.depuis(authentication);

        if (!lecteur.bailleur()) {
            throw new AccessDeniedException(
                    "La validation est réservée au bailleur."
            );
        }

        if (!Boolean.TRUE.equals(confirmation)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La confirmation explicite est obligatoire."
            );
        }

        IdentiteAudit.Acteur contexteAudit =
                identiteAudit.depuis(authentication);
        String acteur = contexteAudit.sujet();
        String emetteur = contexteAudit.emetteurIdentite();

        Quittance document = repository.trouverPourModification(
                id,
                lecteur.identifiant()
        ).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document introuvable."
                )
        );

        boolean statutAutorise =
                document.getStatut() == StatutQuittance.BROUILLON
                        || document.getStatut()
                        == StatutQuittance.EN_ATTENTE_VALIDATION;

        if (!statutAutorise
                || document.getStatutSignature() != StatutSignature.NON_SIGNE
                || document.getEmisLe() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le document n'est plus dans un état permettant "
                            + "cette validation."
            );
        }

        String statutAvant = document.getStatut().name();
        long versionAvant = document.getVersion();
        Instant date = Instant.now();

        document.validerPourSignature();

        repository.saveAndFlush(document);

        traceRepository.enregistrer(
                document.getId(),
                acteur,
                emetteur,
                date,
                versionAvant,
                statutAvant
        );

        return new ValidationQuittanceResponse(
                document.getId(),
                document.getStatut().name(),
                document.getStatutSignature().name(),
                date
        );
    }

}