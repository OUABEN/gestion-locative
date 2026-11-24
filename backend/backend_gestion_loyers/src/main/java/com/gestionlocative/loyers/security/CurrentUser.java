package com.gestionlocative.loyers.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthenticatedUser> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /** En-tête Authorization à transmettre aux autres services (JWT de l'utilisateur). */
    public static String bearerHeader() {
        return "Bearer " + get().map(AuthenticatedUser::token)
                .orElseThrow(() -> new IllegalStateException("Aucun jeton utilisateur disponible pour l'appel au service"));
    }
}
