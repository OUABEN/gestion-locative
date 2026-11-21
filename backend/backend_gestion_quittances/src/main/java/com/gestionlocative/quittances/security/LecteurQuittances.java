package com.gestionlocative.quittances.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record LecteurQuittances(
        boolean bailleur,
        String identifiant
) {

    public static LecteurQuittances depuis(
            Authentication authentication
    ) {
        if (!(authentication instanceof JwtAuthenticationToken jwt)
                || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Une identité JWT authentifiée est nécessaire."
            );
        }

        boolean estBailleur = possedeRole(authentication, "ROLE_BAILLEUR");
        boolean estLocataire = possedeRole(authentication, "ROLE_LOCATAIRE");
        boolean estGestionnaire = possedeRole(authentication, "ROLE_GESTIONNAIRE");

        if (estGestionnaire) {
            throw new AccessDeniedException(
                    "Le périmètre documentaire du rôle GESTIONNAIRE n'est pas défini."
            );
        }

        if (estBailleur == estLocataire) {
            throw new AccessDeniedException(
                    "Un rôle unique BAILLEUR ou LOCATAIRE est nécessaire."
            );
        }

        String nomClaim = estBailleur ? "bailleur_id" : "locataire_id";
        Object valeur = jwt.getToken().getClaims().get(nomClaim);

        if (!(valeur instanceof String identifiant)
                || identifiant.isBlank()
                || identifiant.length() > 100
                || !identifiant.equals(identifiant.trim())) {
            throw new AccessDeniedException(
                    "L'identifiant métier de l'utilisateur est invalide."
            );
        }

        return new LecteurQuittances(estBailleur, identifiant);
    }

    private static boolean possedeRole(
            Authentication authentication,
            String role
    ) {
        return authentication.getAuthorities().stream()
                .anyMatch(autorite -> role.equals(autorite.getAuthority()));
    }
}