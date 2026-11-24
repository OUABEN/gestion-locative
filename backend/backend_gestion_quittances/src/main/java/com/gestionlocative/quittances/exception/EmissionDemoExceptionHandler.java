package com.gestionlocative.quittances.exception;

import com.gestionlocative.quittances.controller.EmissionDemoController;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = EmissionDemoController.class)
public class EmissionDemoExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> statut(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(ProblemDetail.forStatusAndDetail(e.getStatusCode(),
            e.getReason() == null ? "Opération impossible." : e.getReason()));
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> interdit(AccessDeniedException e) { return erreur(403,"Accès interdit."); }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ProblemDetail> invalide(Exception e) { return erreur(400,"Identifiant ou corps JSON invalide."); }
    @ExceptionHandler(ServiceExterneException.class)
    public ResponseEntity<ProblemDetail> externe(ServiceExterneException e) { return erreur(503,"Impossible de vérifier les paiements auprès du service source."); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> autre(Exception e) { return erreur(500,"Émission indisponible. Consultez l'état du document avant de réessayer."); }
    private ResponseEntity<ProblemDetail> erreur(int statut, String message) {
        return ResponseEntity.status(statut).body(ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(statut),message));
    }
}
