package com.gestionlocative.quittances;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class EtatServiceHttpTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void doitRetournerEtatOperationnel() throws Exception {
        HttpResponse<String> response =
                appeler("/api/v1/systeme/etat", null);

        assertEquals(
                200,
                response.statusCode(),
                "La route d'état doit être accessible sans authentification."
        );

        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("");

        assertTrue(
                contentType.contains("application/json"),
                "La réponse doit être au format JSON."
        );

        String corpsNormalise = response.body()
                .replaceAll("\\s+", "");

        assertTrue(
                corpsNormalise.contains("\"service\":\"service-quittances\""),
                "La réponse doit identifier notre service."
        );

        assertTrue(
                corpsNormalise.contains("\"statut\":\"OPERATIONNEL\""),
                "La réponse doit indiquer que le service est opérationnel."
        );
    }

    @Test
    void doitRefuserIdentiteSansJeton() throws Exception {
        HttpResponse<String> response =
                appeler("/api/v1/identite", null);

        assertEquals(
                401,
                response.statusCode(),
                "La route d'identité doit refuser une requête sans jeton."
        );
    }

    @Test
    void doitRefuserIdentiteAvecJetonInvalide() throws Exception {
        HttpResponse<String> response =
                appeler("/api/v1/identite", "faux-jeton");

        assertEquals(
                401,
                response.statusCode(),
                "La route d'identité doit refuser un jeton invalide."
        );
    }

    private HttpResponse<String> appeler(
            String chemin,
            String jeton
    ) throws Exception {

        HttpRequest.Builder requete = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + chemin))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET();

        if (jeton != null) {
            requete.header("Authorization", "Bearer " + jeton);
        }

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

            return client.send(
                    requete.build(),
                    HttpResponse.BodyHandlers.ofString()
            );
        }
    }
}