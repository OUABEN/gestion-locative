package com.gestionlocative.quittances.exception;

import com.gestionlocative.quittances.controller.QuittanceValidationController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import com.gestionlocative.quittances.controller.QuittanceAnnulationController;

@RestControllerAdvice(
        assignableTypes = {
                QuittanceValidationController.class,
                QuittanceAnnulationController.class
        }
)
public class QuittanceValidationExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> statut(
            ResponseStatusException exception
    ) {
        int statut = exception.getStatusCode().value();

        String code = switch (statut) {
            case 404 -> "DOCUMENT_INTROUVABLE";
            case 409 -> "TRANSITION_INTERDITE";
            default -> "REQUETE_INVALIDE";
        };

        return erreur(
                statut,
                code,
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
                "Vous n'êtes pas autorisé à effectuer cette opération."
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemDetail> requete(Exception exception) {
        return erreur(
                400,
                "REQUETE_INVALIDE",
                "Vérifiez l'UUID et le corps JSON de la demande."
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

        return ResponseEntity.status(statut).body(probleme);
    }
}