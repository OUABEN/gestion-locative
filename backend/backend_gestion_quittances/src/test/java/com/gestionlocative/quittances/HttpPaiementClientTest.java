package com.gestionlocative.quittances;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import com.gestionlocative.quittances.client.HttpPaiementClient;
import com.gestionlocative.quittances.exception.ServiceExterneException;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpPaiementClientTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 10, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 10, 31);

    private static final String REPONSE_VALIDE = """
            {
              "versionContrat": "1",
              "complet": true,
              "bailId": "BAIL-1",
              "periodeDebut": "2026-10-01",
              "periodeFin": "2026-10-31",
              "affectations": [
                {
                  "affectationId": "AFF-1",
                  "paiementId": "PAY-1",
                  "bailId": "BAIL-1",
                  "periodeDebut": "2026-10-01",
                  "periodeFin": "2026-10-31",
                  "nature": "LOYER",
                  "montant": 1500.00,
                  "devise": "MAD",
                  "statut": "CONFIRME"
                }
              ]
            }
            """;

    private HttpServer serveur;
    private HttpClient transport;
    private HttpPaiementClient client;

    private volatile int statut;
    private volatile String reponse;
    private volatile long delaiMillis;

    private final AtomicReference<String> methode = new AtomicReference<>();
    private final AtomicReference<String> requete = new AtomicReference<>();

    @BeforeEach
    void preparer() throws IOException {
        statut = 200;
        reponse = REPONSE_VALIDE;
        delaiMillis = 0;

        serveur = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        serveur.createContext("/api/v1/paiements/affectations", exchange -> {
            methode.set(exchange.getRequestMethod());
            requete.set(exchange.getRequestURI().getRawQuery());

            try {
                if (delaiMillis > 0) {
                    Thread.sleep(delaiMillis);
                }

                byte[] corps = reponse.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set(
                        "Content-Type",
                        "application/json"
                );

                exchange.sendResponseHeaders(statut, corps.length);
                exchange.getResponseBody().write(corps);

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException exception) {
                // Le client peut fermer la connexion lors du test de timeout.
            } finally {
                exchange.close();
            }
        });

        serveur.start();

        transport = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build();

        client = creerClient(Duration.ofSeconds(2));
    }

    @AfterEach
    void nettoyer() {
        if (serveur != null) {
            serveur.stop(0);
        }

        if (transport != null) {
            transport.close();
        }
    }

    @Test
    void doitLireLesAffectationsParGet() {
        var affectations = client.consulterAffectations("BAIL-1", DEBUT, FIN);

        assertEquals(1, affectations.size());
        assertEquals("AFF-1", affectations.getFirst().affectationId());
        assertEquals("GET", methode.get());

        assertTrue(requete.get().contains("bailId=BAIL-1"));
        assertTrue(requete.get().contains("debut=2026-10-01"));
        assertTrue(requete.get().contains("fin=2026-10-31"));
    }

    @Test
    void doitConserverUneErreur404() {
        statut = 404;
        reponse = "{}";

        ServiceExterneException exception = appelerAvecErreur();

        assertEquals("ERREUR_HTTP_EXTERNE", exception.getCode());
        assertEquals(Integer.valueOf(404), exception.getStatutHttp());
    }

    @Test
    void doitConserverUneErreur503() {
        statut = 503;
        reponse = "{}";

        ServiceExterneException exception = appelerAvecErreur();

        assertEquals("ERREUR_HTTP_EXTERNE", exception.getCode());
        assertEquals(Integer.valueOf(503), exception.getStatutHttp());
    }

    @Test
    void doitRefuserUnJsonInvalide() {
        reponse = "{ ceci est invalide";

        assertEquals("REPONSE_ILLISIBLE", appelerAvecErreur().getCode());
    }

    @Test
    void doitRefuserUneReponseIncomplete() {
        reponse = REPONSE_VALIDE.replace(
                "\"complet\": true",
                "\"complet\": false"
        );

        assertEquals("REPONSE_INCOHERENTE", appelerAvecErreur().getCode());
    }

    @Test
    void doitInterrompreUneAttenteTropLongue() {
        delaiMillis = 1000;
        client = creerClient(Duration.ofMillis(100));

        assertEquals("SERVICE_INJOIGNABLE", appelerAvecErreur().getCode());
    }

    private HttpPaiementClient creerClient(Duration timeout) {
        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(transport);

        factory.setReadTimeout(timeout);

        RestClient restClient = RestClient.builder()
                .baseUrl("http://127.0.0.1:" + serveur.getAddress().getPort())
                .requestFactory(factory)
                .build();

        return new HttpPaiementClient(
                restClient,
                "/api/v1/paiements/affectations?bailId={bailId}&debut={debut}&fin={fin}"
        );
    }

    private ServiceExterneException appelerAvecErreur() {
        return assertThrows(
                ServiceExterneException.class,
                () -> client.consulterAffectations("BAIL-1", DEBUT, FIN)
        );
    }
}