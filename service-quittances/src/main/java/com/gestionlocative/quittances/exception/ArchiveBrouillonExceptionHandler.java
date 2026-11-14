package com.gestionlocative.quittances.exception;

import com.gestionlocative.quittances.controller.ArchiveBrouillonController;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestControllerAdvice(assignableTypes = ArchiveBrouillonController.class)
public class ArchiveBrouillonExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> statut(
            ResponseStatusException exception
    ) {
        int statut = exception.getStatusCode().value();

        return erreur(
                statut,
                statut == 404 ? "ARCHIVE_INTROUVABLE" : "ARCHIVAGE_REFUSE",
                exception.getReason() == null
                        ? "L'opération ne peut pas être réalisée."
                        : exception.getReason()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> acces(
            AccessDeniedException exception
    ) {
        return erreur(
                403,
                "ACCES_INTERDIT",
                "Vous n'êtes pas autorisé à accéder à cette archive."
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> identifiant(
            MethodArgumentTypeMismatchException exception
    ) {
        return erreur(
                400,
                "IDENTIFIANT_INVALIDE",
                "Les identifiants doivent être des UUID valides."
        );
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ProblemDetail> doublon(
            DuplicateKeyException exception
    ) {
        return erreur(
                409,
                "ARCHIVE_EN_CONFLIT",
                "L'enregistrement entre en conflit avec une archive existante."
        );
    }

    @ExceptionHandler(StockagePdfException.class)
    public ResponseEntity<ProblemDetail> stockage(
            StockagePdfException exception
    ) {
        int statut = switch (exception.getCode()) {
            case "PDF_ABSENT" -> 404;
            case "INTEGRITE_PDF_INVALIDE" -> 409;
            default -> 500;
        };

        return erreur(
                statut,
                exception.getCode(),
                "Le fichier ne peut pas être enregistré ou téléchargé."
        );
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ProblemDetail> generation(IOException exception) {
        return erreur(
                500,
                "GENERATION_PDF_IMPOSSIBLE",
                "Le PDF n'a pas pu être généré."
        );
    }

    private ResponseEntity<ProblemDetail> erreur(
            int statut,
            String code,
            String detail
    ) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(statut),
                detail
        );

        probleme.setProperty("code", code);

        return ResponseEntity.status(statut)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(probleme);
    }
}