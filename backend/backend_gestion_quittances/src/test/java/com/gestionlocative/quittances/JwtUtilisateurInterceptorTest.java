package com.gestionlocative.quittances;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.gestionlocative.quittances.config.JwtUtilisateurInterceptor;
import com.gestionlocative.quittances.config.PaiementClientConfiguration;
import com.gestionlocative.quittances.config.ReferentielsConfiguration;
import com.gestionlocative.quittances.config.ReferentielsProperties;
import com.gestionlocative.quittances.config.ReferentielsProperties.Endpoint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilisateurInterceptorTest {

    private final JwtUtilisateurInterceptor interceptor =
            new JwtUtilisateurInterceptor();

    @AfterEach
    void nettoyerContexte() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transmetLeJetonDeLUtilisateurAuthentifie() throws Exception {
        Jwt jeton = new Jwt(
                "jwt-utilisateur-de-test",
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(300),
                Map.of("alg", "HS256"),
                Map.of("sub", "utilisateur-1", "role", "BAILLEUR")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jeton, List.of())
        );

        var requete = requete();
        var authorization = new AtomicReference<String>();

        interceptor.intercept(requete, new byte[0], (req, body) -> {
            authorization.set(req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        });

        assertEquals("Bearer jwt-utilisateur-de-test", authorization.get());
    }

    @Test
    void refuseUnAppelSansAuthentification() {
        var requete = requete();
        assertThrows(AccessDeniedException.class, () ->
                interceptor.intercept(requete, new byte[0],
                        (req, body) -> new MockClientHttpResponse(
                                new byte[0], HttpStatus.OK)));
        assertNull(requete.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void refuseUneAuthentificationNonJwt() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test", "mot-de-passe-invalide", List.of()
                )
        );
        assertThrows(AccessDeniedException.class, () ->
                interceptor.intercept(requete(), new byte[0],
                        (req, body) -> new MockClientHttpResponse(
                                new byte[0], HttpStatus.OK)));
    }

    @Test
    void referentielsAcceptentLeModeUtilisateurSansTokenStatique() {
        var config = new ReferentielsConfiguration();
        var endpoint = new Endpoint(
                "UTILISATEUR", "http://127.0.0.1:18082",
                "/api/v1/locataires/{id}/quittance-summary", ""
        );

        try (HttpClient transport = HttpClient.newHttpClient()) {
            assertNotNull(config.locataireClient(
                    new ReferentielsProperties(endpoint, null, null),
                    transport
            ));

            var avecJetonFixe = new Endpoint(
                    "UTILISATEUR", endpoint.baseUrl(),
                    endpoint.chemin(), "jeton-statique-interdit"
            );
            assertThrows(IllegalArgumentException.class, () ->
                    config.locataireClient(new ReferentielsProperties(
                            avecJetonFixe, null, null), transport));
        }
    }

    @Test
    void paiementsAcceptentLeModeUtilisateurSansTokenStatique() {
        var config = new PaiementClientConfiguration();
        String chemin = "/api/v1/paiements/affectations"
                + "?bailId={bailId}&debut={debut}&fin={fin}";

        try (HttpClient transport = HttpClient.newHttpClient()) {
            assertNotNull(config.paiementClient(
                    transport, "http://127.0.0.1:18083",
                    chemin, "UTILISATEUR", ""
            ));

            assertThrows(IllegalArgumentException.class, () ->
                    config.paiementClient(
                            transport, "http://127.0.0.1:18083",
                            chemin, "UTILISATEUR", "jeton-statique-interdit"
                    ));
        }
    }

    private static MockClientHttpRequest requete() {
        return new MockClientHttpRequest(
                HttpMethod.GET, URI.create("http://127.0.0.1:18081/verification")
        );
    }
}
