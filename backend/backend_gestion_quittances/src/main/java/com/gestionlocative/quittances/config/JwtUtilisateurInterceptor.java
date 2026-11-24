package com.gestionlocative.quittances.config;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Transmet uniquement le JWT de l'utilisateur authentifie au service appele.
 * Aucun jeton de service configure n'est utilise dans ce mode.
 */
public final class JwtUtilisateurInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwt)
                || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Un JWT utilisateur authentifie est requis pour cet appel interservice."
            );
        }

        request.getHeaders().setBearerAuth(jwt.getToken().getTokenValue());
        return execution.execute(request, body);
    }
}
