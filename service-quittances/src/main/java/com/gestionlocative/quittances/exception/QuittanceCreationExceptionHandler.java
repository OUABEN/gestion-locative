package com.gestionlocative.quittances.exception;

import com.gestionlocative.quittances.controller.QuittanceCreationController;
import com.gestionlocative.quittances.controller.QuittanceRemplacementController;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(
        assignableTypes = {
                QuittanceCreationController.class,
                QuittanceRemplacementController.class
        }
)
public class QuittanceCreationExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> accesInterdit(
            AccessDeniedException exception
    ) {
        return erreur(
                HttpStatus.FORBIDDEN,
                "ACCES_INTERDIT",
                "Vous n'êtes pas autorisé à préparer ce document."
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> requeteInvalide(
            ResponseStatusException exception
    ) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(
                exception.getStatusCode(),
                exception.getReason() == null
                        ? "La demande ne peut pas être traitée."
                        : exception.getReason()
        );

        String code = switch (exception.getStatusCode().value()) {
            case 404 -> "DOCUMENT_INTROUVABLE";
            case 409 -> "TRANSITION_INTERDITE";
            default -> "REQUETE_INVALIDE";
        };

        probleme.setProperty("code", code);

        return ResponseEntity.status(exception.getStatusCode())
                .body(probleme);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> jsonInvalide(
            HttpMessageNotReadableException exception
    ) {
        return erreur(
                HttpStatus.BAD_REQUEST,
                "JSON_INVALIDE",
                "Le corps JSON est absent ou invalide. "
                        + "Les dates doivent utiliser le format AAAA-MM-JJ."
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> identifiantInvalide(
            MethodArgumentTypeMismatchException exception
    ) {
        return erreur(
                HttpStatus.BAD_REQUEST,
                "IDENTIFIANT_INVALIDE",
                "L'identifiant du document doit être un UUID valide."
        );
    }

    @ExceptionHandler(RegleMetierException.class)
    public ResponseEntity<ProblemDetail> regleMetier(
            RegleMetierException exception
    ) {
        if ("ACCES_INTERDIT".equals(exception.getCode())
                || "ACTEUR_ABSENT".equals(exception.getCode())) {
            return erreur(
                    HttpStatus.FORBIDDEN,
                    "ACCES_INTERDIT",
                    "Vous n'êtes pas autorisé à préparer ce document."
            );
        }

        return erreur(
                HttpStatus.valueOf(422),
                exception.getCode(),
                "Les données métier ne permettent pas de préparer le document."
        );
    }

    @ExceptionHandler(ServiceExterneException.class)
    public ResponseEntity<ProblemDetail> serviceExterne(
            ServiceExterneException exception
    ) {
        org.slf4j.LoggerFactory
                .getLogger(QuittanceCreationExceptionHandler.class)
                .warn(
                        "Echec source metier : service={}, code={}, statutHttp={}",
                        exception.getService(),
                        exception.getCode(),
                        exception.getStatutHttp()
                );

        return erreur(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SOURCE_METIER_INDISPONIBLE",
                "Une source métier est indisponible ou sa réponse "
                        + "ne permet pas de poursuivre."
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> integrite(
            DataIntegrityViolationException exception
    ) {
        if (estDoublonDocumentActif(exception)) {
            return erreur(
                    HttpStatus.CONFLICT,
                    "QUITTANCE_DEJA_EXISTANTE",
                    "Un document actif existe déjà pour cette opération."
            );
        }

        return erreur(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ENREGISTREMENT_IMPOSSIBLE",
                "Le brouillon n'a pas pu être enregistré."
        );
    }

    private boolean estDoublonDocumentActif(Throwable exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                String contrainte = violation.getConstraintName();

                if ("uq_quittance_integrale_active".equals(contrainte)
                        || "uq_quittance_remplacement_actif".equals(contrainte)) {
                    return true;
                }
            }

            cause = cause.getCause();
        }

        return false;
    }

    private ResponseEntity<ProblemDetail> erreur(
            HttpStatus statut,
            String code,
            String detail
    ) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(
                statut,
                detail
        );

        probleme.setProperty("code", code);

        return ResponseEntity.status(statut).body(probleme);
    }
}