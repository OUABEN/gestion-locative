package com.gestionlocative.quittances.suivi;

import com.gestionlocative.quittances.exception.ServiceExterneException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = SuiviQuittanceController.class)
public class SuiviExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> statut(ResponseStatusException ex) {
        return reponse(ex.getStatusCode(), "REQUETE_INVALIDE", ex.getReason() == null ? "Demande invalide." : ex.getReason());
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> acces(AccessDeniedException ex) {
        return reponse(HttpStatus.FORBIDDEN, "ACCES_INTERDIT", "Votre compte ne permet pas cette opération.");
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> parametre(MethodArgumentTypeMismatchException ex) {
        return reponse(HttpStatus.BAD_REQUEST, "PARAMETRE_INVALIDE", "Vérifiez les dates et l'identifiant du document.");
    }
    @ExceptionHandler(ServiceExterneException.class)
    public ResponseEntity<ProblemDetail> source(ServiceExterneException ex) {
        return reponse(HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_METIER_INDISPONIBLE",
                "Le contrôle n'a pas abouti : le service Paiements est indisponible ou sa réponse est incohérente. Aucun résultat de conformité n'est disponible.");
    }
    private ResponseEntity<ProblemDetail> reponse(HttpStatusCode statut, String code, String detail) {
        var p = ProblemDetail.forStatusAndDetail(statut, detail); p.setProperty("code", code);
        return ResponseEntity.status(statut).cacheControl(CacheControl.noStore()).body(p);
    }
}
