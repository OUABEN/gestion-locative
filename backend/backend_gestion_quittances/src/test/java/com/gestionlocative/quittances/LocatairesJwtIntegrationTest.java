package com.gestionlocative.quittances;

import com.gestionlocative.quittances.client.LocataireClient;
import com.gestionlocative.quittances.config.ReferentielsConfiguration;
import com.gestionlocative.quittances.config.ReferentielsProperties;
import com.gestionlocative.quittances.config.ReferentielsProperties.Endpoint;
import com.gestionlocative.quittances.exception.ServiceExterneException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocatairesJwtIntegrationTest {

    private static final String ID = "a1111111-1111-1111-1111-111111111111";
    private static final String CHEMIN = "/api/v1/locataires/{id}/quittance-summary";
    private static final String JETON = "jeton-test-locataires";

    private HttpServer serveur;
    private HttpClient transport;
    private volatile String reponse;
    private final AtomicReference<String> cheminRecu = new AtomicReference<>();
    private final AtomicReference<String> authorizationRecue = new AtomicReference<>();

    @BeforeEach
    void preparer() throws IOException {
        reponse = """
                {
                  "id": "a1111111-1111-1111-1111-111111111111",
                  "nom": "El Amrani",
                  "prenom": "Yassine",
                  "nomComplet": "Yassine El Amrani",
                  "cin": "AB123456",
                  "email": "yassine@example.test",
                  "telephone": "0661001122"
                }
                """;
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.createContext("/", exchange -> {
            cheminRecu.set(exchange.getRequestURI().getPath());
            authorizationRecue.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] corps = reponse.getBytes(StandardCharsets.UTF_8);
            try {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, corps.length);
                exchange.getResponseBody().write(corps);
            } finally {
                exchange.close();
            }
        });
        serveur.start();
        transport = HttpClient.newHttpClient();
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        if (serveur != null) {
            serveur.stop(0);
        }
        if (transport != null) {
            transport.close();
        }
    }

    @Test
    void litLaSyntheseReelleEtTransmetLeJwtUtilisateur() {
        authentifier();

        var locataire = client(CHEMIN).consulter(ID);

        assertEquals(ID, locataire.id());
        assertEquals("El Amrani", locataire.nom());
        assertEquals("Yassine", locataire.prenom());
        assertEquals("yassine@example.test", locataire.email());
        assertEquals("1", locataire.versionContrat());
        assertFalse(locataire.emailVerifie());
        assertEquals("Bearer " + JETON, authorizationRecue.get());
        assertEquals("/api/v1/locataires/" + ID + "/quittance-summary", cheminRecu.get());
    }

    @Test
    void accepteUneSyntheseSansAdresseEmail() {
        authentifier();
        reponse = reponse.replace("\"yassine@example.test\"", "null");

        var locataire = client(CHEMIN).consulter(ID);

        assertNull(locataire.email());
        assertFalse(locataire.emailVerifie());
    }

    @Test
    void refuseUneSyntheseDontLIdentifiantNeCorrespondPas() {
        authentifier();
        reponse = reponse.replace(ID, "a2222222-2222-2222-2222-222222222222");

        var erreur = assertThrows(ServiceExterneException.class,
                () -> client(CHEMIN).consulter(ID));

        assertEquals("REPONSE_INCOHERENTE", erreur.getCode());
    }

    @Test
    void refuseUnCheminNonAdapteAuContratReel() {
        for (String chemin : List.of(
                "/api/v1/locataires/{id}",
                "/api/locataires/{id}")) {
            assertThrows(IllegalArgumentException.class, () -> client(chemin));
        }
    }

    private LocataireClient client(String chemin) {
        String baseUrl = "http://127.0.0.1:" + serveur.getAddress().getPort();
        Endpoint endpoint = new Endpoint("UTILISATEUR", baseUrl, chemin, "");
        return new ReferentielsConfiguration().locataireClient(
                new ReferentielsProperties(endpoint, null, null), transport);
    }

    private void authentifier() {
        Jwt jwt = new Jwt(
                JETON,
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(300),
                Map.of("alg", "HS256"),
                Map.of("sub", "utilisateur-1", "role", "BAILLEUR")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of()));
    }
}
