package com.gestionlocative.quittances.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public final class IdentiteAudit {

    public static final String SOURCE_JWT_COMMUN = "JWT_COMMUN";

    private final String modeJwt;

    public IdentiteAudit(
            @Value("${quittances.securite.mode}") String modeJwt
    ) {
        this.modeJwt = modeJwt;
    }

    public Acteur depuis(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt)
                || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Une identité JWT authentifiée est nécessaire pour l'audit."
            );
        }

        String sujet = jwt.getToken().getSubject();
        if (sujet == null || sujet.isBlank() || sujet.length() > 200) {
            throw new AccessDeniedException(
                    "Le sujet JWT ne permet pas une traçabilité suffisante."
            );
        }

        String emetteurIdentite;
        if ("COMMUN".equals(modeJwt)) {
            // Provenance d'authentification, jamais un issuer prétendu du JWT.
            emetteurIdentite = SOURCE_JWT_COMMUN;
        } else {
            emetteurIdentite = jwt.getToken().getClaimAsString("iss");
            if (emetteurIdentite == null
                    || emetteurIdentite.isBlank()
                    || emetteurIdentite.length() > 500) {
                throw new AccessDeniedException(
                        "L'émetteur JWT ne permet pas une traçabilité suffisante."
                );
            }
        }

        return new Acteur(sujet, emetteurIdentite);
    }

    public record Acteur(String sujet, String emetteurIdentite) {
    }
}
