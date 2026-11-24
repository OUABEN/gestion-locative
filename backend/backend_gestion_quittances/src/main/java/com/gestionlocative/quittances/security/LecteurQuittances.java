package com.gestionlocative.quittances.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record LecteurQuittances(
        boolean bailleur,
        String identifiant,
        boolean gestionnaire
) {

    public LecteurQuittances(boolean bailleur, String identifiant) {
        this(bailleur, identifiant, false);
    }

    public boolean locataire() {
        return !bailleur && !gestionnaire;
    }

    public boolean peutEcrire() {
        return bailleur || gestionnaire;
    }

    public static LecteurQuittances depuis(Authentication authentication) {

        if (!(authentication instanceof JwtAuthenticationToken jwt)
                || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Une identite JWT authentifiee est necessaire."
            );
        }

        boolean estBailleur =
                possedeRole(authentication, "ROLE_BAILLEUR");

        boolean estLocataire =
                possedeRole(authentication, "ROLE_LOCATAIRE");

        boolean estGestionnaire =
                possedeRole(authentication, "ROLE_GESTIONNAIRE");

        int nombreRoles = (estBailleur ? 1 : 0)
                + (estLocataire ? 1 : 0)
                + (estGestionnaire ? 1 : 0);

        if (nombreRoles != 1) {
            throw new AccessDeniedException(
                    "Un role metier unique est necessaire."
            );
        }

        if (estGestionnaire) {
            String sujet = jwt.getToken().getSubject();

            if (!identifiantValide(sujet)) {
                throw new AccessDeniedException(
                        "Identifiant du gestionnaire invalide."
                );
            }

            return new LecteurQuittances(false, sujet, true);
        }

        String nomClaim = estBailleur
                ? "bailleur_id"
                : "locataire_id";

        Object valeur = jwt.getToken().getClaims().get(nomClaim);

        if (!(valeur instanceof String identifiant)
                || !identifiantValide(identifiant)) {
            throw new AccessDeniedException(
                    "Identifiant metier de l'utilisateur invalide."
            );
        }

        return new LecteurQuittances(
                estBailleur, identifiant, false
        );
    }

    private static boolean identifiantValide(String identifiant) {
        return identifiant != null
                && !identifiant.isBlank()
                && identifiant.length() <= 100
                && identifiant.equals(identifiant.trim());
    }

    private static boolean possedeRole(
            Authentication authentication,
            String role
    ) {
        return authentication.getAuthorities().stream()
                .anyMatch(autorite ->
                        role.equals(autorite.getAuthority()));
    }
}