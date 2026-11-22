package com.gestionlocative.quittances.exception;

import com.gestionlocative.quittances.controller.QuittanceApercuController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@RestControllerAdvice(assignableTypes = QuittanceApercuController.class)
public class QuittanceApercuExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(QuittanceApercuExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> statut(
            ResponseStatusException exception
    ) {
        String code = exception.getStatusCode().value() == 404
                ? "DOCUMENT_INTROUVABLE"
                : "APERCU_INDISPONIBLE";

        return erreur(
                exception.getStatusCode().value(),
                code,
                exception.getReason() == null
                        ? "La prévisualisation est indisponible."
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
                "Vous n'êtes pas autorisé à prévisualiser ce document."
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> identifiant(
            MethodArgumentTypeMismatchException exception
    ) {
        return erreur(
                400,
                "IDENTIFIANT_INVALIDE",
                "L'identifiant du document doit être un UUID valide."
        );
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ProblemDetail> generation(
            IOException exception
    ) {
        LOGGER.error(
                "Échec de génération de l'aperçu PDF. Type : {}",
                exception.getClass().getSimpleName()
        );

        return erreur(
                500,
                "GENERATION_PDF_IMPOSSIBLE",
                "La prévisualisation n'a pas pu être générée."
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