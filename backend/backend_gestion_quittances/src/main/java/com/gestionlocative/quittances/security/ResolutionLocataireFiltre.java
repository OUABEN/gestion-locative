package com.gestionlocative.quittances.security;

import com.gestionlocative.quittances.client.LocataireClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public final class ResolutionLocataireFiltre
        extends OncePerRequestFilter {

    private final LocataireClient locataires;

    public ResolutionLocataireFiltre(LocataireClient locataires) {
        this.locataires = locataires;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        var contexte = SecurityContextHolder.getContext();
        var authentication = contexte.getAuthentication();

        String chemin = request.getServletPath();

        boolean routeQuittances =
                "/api/v1/quittances".equals(chemin)
                || chemin.startsWith("/api/v1/quittances/");

        if (!routeQuittances
                || !(authentication instanceof JwtAuthenticationToken jwt)
                || !authentication.isAuthenticated()
                || jwt.getAuthorities().stream().noneMatch(
                        a -> "ROLE_LOCATAIRE".equals(a.getAuthority()))) {

            chain.doFilter(request, response);
            return;
        }

        if (!"GET".equals(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            String identifiant = locataires.identifierConnecte();

            if (identifiant == null
                    || identifiant.isBlank()
                    || identifiant.length() > 100
                    || !identifiant.equals(identifiant.trim())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            Jwt original = jwt.getToken();

            var claims = new HashMap<>(original.getClaims());
            claims.put("locataire_id", identifiant);

            Jwt enrichi = new Jwt(
                    original.getTokenValue(),
                    original.getIssuedAt(),
                    original.getExpiresAt(),
                    original.getHeaders(),
                    claims
            );

            var identite = new JwtAuthenticationToken(
                    enrichi,
                    jwt.getAuthorities(),
                    jwt.getName()
            );

            identite.setDetails(jwt.getDetails());
            contexte.setAuthentication(identite);

        } catch (AccessDeniedException exception) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;

        } catch (com.gestionlocative.quittances.exception.ServiceExterneException exception) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        chain.doFilter(request, response);
    }
}