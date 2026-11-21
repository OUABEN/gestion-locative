package com.gestionlocative.quittances.exception;

import com.gestionlocative.quittances.controller.QuittanceConsultationController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import com.gestionlocative.quittances.controller.QuittanceHistoriqueController;

@RestControllerAdvice(
        assignableTypes = {
                QuittanceConsultationController.class,
                QuittanceHistoriqueController.class
        }
)
public class QuittanceConsultationExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> traiterStatut(
            ResponseStatusException exception
    ) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(
                exception.getStatusCode(),
                exception.getReason() == null
                        ? "La demande ne peut pas être traitée."
                        : exception.getReason()
        );

        probleme.setProperty(
                "code",
                exception.getStatusCode().value() == 404
                        ? "DOCUMENT_INTROUVABLE"
                        : "REQUETE_INVALIDE"
        );

        return ResponseEntity.status(exception.getStatusCode())
                .body(probleme);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> accesInterdit(
            AccessDeniedException exception
    ) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Votre identité ne permet pas cette consultation."
        );

        probleme.setProperty("code", "ACCES_INTERDIT");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(probleme);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> parametreInvalide(
            MethodArgumentTypeMismatchException exception
    ) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Un paramètre est invalide. Vérifiez l'UUID du document "
                        + "et les nombres utilisés pour la pagination."
        );

        probleme.setProperty("code", "PARAMETRE_INVALIDE");

        return ResponseEntity.badRequest().body(probleme);
    }
}