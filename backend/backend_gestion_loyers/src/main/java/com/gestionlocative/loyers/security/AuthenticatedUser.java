package com.gestionlocative.loyers.security;

import java.security.Principal;

/** Utilisateur authentifié par le JWT (sub, role, email) ; le jeton est conservé pour être transmis aux autres services. */
public record AuthenticatedUser(String id, String role, String email, String token) implements Principal {

    @Override
    public String getName() {
        return id;
    }

    /** Le jeton ne doit jamais apparaître dans les journaux. */
    @Override
    public String toString() {
        return "AuthenticatedUser[id=" + id + ", role=" + role + "]";
    }
}
