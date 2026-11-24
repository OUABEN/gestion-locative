package com.gestionlocative.quittances;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import com.gestionlocative.quittances.client.HttpBienBailClient;
import com.gestionlocative.quittances.client.HttpLocataireClient;
import com.gestionlocative.quittances.exception.ServiceExterneException;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferentielsClientsTest {

    private HttpServer serveur;
    private HttpClient transport;
    private HttpLocataireClient locataires;
    private HttpLocataireClient locatairesAhmed;
    private HttpBienBailClient biensBaux;

    private volatile int statut;
    private volatile String reponse;

    private final AtomicReference<String> chemin = new AtomicReference<>();
    private final AtomicReference<String> parametres = new AtomicReference<>();

    @BeforeEach
    void preparer() throws IOException {
        statut = 200;
        reponse = "{}";

        serveur = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        serveur.createContext("/", exchange -> {
            chemin.set(exchange.getRequestURI().getPath());
            parametres.set(exchange.getRequestURI().getRawQuery());

            byte[] corps = reponse.getBytes(StandardCharsets.UTF_8);

            try {
                exchange.getResponseHeaders().set(
                        "Content-Type",
                        "application/json"
                );
                exchange.sendResponseHeaders(statut, corps.length);
                exchange.getResponseBody().write(corps);
            } finally {
                exchange.close();
            }
        });

        serveur.start();

        transport = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build();

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(transport);

        factory.setReadTimeout(Duration.ofSeconds(2));

        RestClient client = RestClient.builder()
                .baseUrl("http://127.0.0.1:" + serveur.getAddress().getPort())
                .requestFactory(factory)
                .build();

        locataires = new HttpLocataireClient(
                client,
                "/api/v1/locataires/{id}"
        );

        locatairesAhmed = new HttpLocataireClient(client,
                "/api/v1/locataires/{id}/quittance-summary", true);
        biensBaux = new HttpBienBailClient(
                client,
                "/api/biens/{id}",
                client,
                "/api/v1/baux/{id}/situation?debut={debut}&fin={fin}"
        );
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
    void doitLireUnLocataireSansEmail() {
        reponse = """
                {
                  "versionContrat": "1",
                  "id": "LOC-1",
                  "nom": "Démonstration",
                  "prenom": "Locataire",
                  "email": null,
                  "emailVerifie": false
                }
                """;

        var resultat = locataires.consulter("LOC-1");

        assertEquals("Démonstration", resultat.nom());
        assertNull(resultat.email());
        assertEquals("/api/v1/locataires/LOC-1", chemin.get());
    }

    @Test
    void doitRefuserUnLocataireDifferentDeCeluiDemande() {
        reponse = """
                {
                  "versionContrat": "1",
                  "id": "LOC-2",
                  "nom": "Démonstration",
                  "prenom": "Locataire",
                  "email": null,
                  "emailVerifie": false
                }
                """;

        ServiceExterneException erreur = assertThrows(
                ServiceExterneException.class,
                () -> locataires.consulter("LOC-1")
        );

        assertEquals("REPONSE_INCOHERENTE", erreur.getCode());
    }

    @Test
    void doitAdapterLeContratReelDuBienAvecSonBailleur() {
        reponse = """
                {
                  "id": "BIEN-1",
                  "adresse": "Adresse fictive à Agadir",
                  "ville": "Agadir",
                  "type": "Appartement",
                  "surface": 75,
                  "bailleurId": "BAILLEUR-1",
                  "prixMensuel": 3000,
                  "charges": 200,
                  "statut": "LIBRE"
                }
                """;

        var resultat = biensBaux.consulterBien("BIEN-1");

        assertEquals("Adresse fictive à Agadir, Agadir", resultat.adresse());
        assertEquals("BAILLEUR-1", resultat.bailleurId());
        assertEquals("1", resultat.versionContrat());
        assertEquals("/api/biens/BIEN-1", chemin.get());
    }

    @Test
    void doitLireLaSituationContractuelleDeLaPeriode() {
        reponse = """
                {
                  "versionContrat": "1",
                  "bailId": "BAIL-1",
                  "locataireId": "LOC-1",
                  "bienId": "BIEN-1",
                  "bailleurId": "BAILLEUR-1",
                  "nomBailleur": "Bailleur de démonstration",
                  "debutBail": "2026-01-01",
                  "finBail": null,
                  "periodeDebut": "2026-10-01",
                  "periodeFin": "2026-10-31",
                  "loyerDu": 3000.00,
                  "chargesDues": 200.00,
                  "devise": "MAD"
                }
                """;

        var resultat = biensBaux.consulterBail(
                "BAIL-1",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)
        );

        assertEquals(
                0,
                new BigDecimal("3000.00").compareTo(resultat.loyerDu())
        );

        assertEquals("LOC-1", resultat.locataireId());
        assertNull(resultat.finBail());

        assertEquals("/api/v1/baux/BAIL-1/situation", chemin.get());
        assertTrue(parametres.get().contains("debut=2026-10-01"));
        assertTrue(parametres.get().contains("fin=2026-10-31"));
    }

    @Test
    void doitConserverLeRefusPourUnBailInexistant() {
        statut = 404;
        reponse = "{}";

        ServiceExterneException erreur = assertThrows(
                ServiceExterneException.class,
                () -> biensBaux.consulterBail(
                        "BAIL-INCONNU",
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31)
                )
        );

        assertEquals("BAUX", erreur.getService());
        assertEquals(Integer.valueOf(404), erreur.getStatutHttp());
    }

    @Test
    void doitRefuserUnBienSansAdresse() {
        reponse = """
                {
                  "id": "BIEN-1",
                  "adresse": "",
                  "ville": "Agadir"
                }
                """;

        ServiceExterneException erreur = assertThrows(
                ServiceExterneException.class,
                () -> biensBaux.consulterBien("BIEN-1")
        );

        assertEquals("REPONSE_INCOHERENTE", erreur.getCode());
    }

    @Test
    void doitAccepterUnBienSansVilleSiAdresseEtBailleurValides() {
        reponse = """
                {
                  "id": "BIEN-1",
                  "adresse": "Adresse fictive",
                  "ville": "",
                  "bailleurId": "BAILLEUR-1"
                }
                """;

        var resultat = biensBaux.consulterBien("BIEN-1");

        assertEquals("Adresse fictive", resultat.adresse());
        assertEquals("BAILLEUR-1", resultat.bailleurId());
    }

    @Test
    void doitAdapterLaSyntheseAhmedSansDeclarerEmailVerifie() {
        reponse = """
                {"id":"LOC-1","nom":"TEST","prenom":"Integration",
                 "email":"locataire@example.test","nomComplet":"Integration TEST",
                 "cin":"TEST123","telephone":"0600000000"}
                """;
        var resultat = locatairesAhmed.consulter("LOC-1");
        assertEquals("1", resultat.versionContrat());
        assertEquals("TEST", resultat.nom());
        assertEquals("locataire@example.test", resultat.email());
        assertEquals(Boolean.FALSE, resultat.emailVerifie());
        assertEquals("/api/v1/locataires/LOC-1/quittance-summary", chemin.get());
    }

    @Test
    void doitAdapterAhmedSansEmail() {
        reponse = """
                {"id":"LOC-1","nom":"TEST","prenom":"Integration","email":null}
                """;
        assertNull(locatairesAhmed.consulter("LOC-1").email());
    }

    @Test
    void doitRefuserUneSyntheseAhmedIncompleteOuDifferente() {
        for (String corps : new String[] {
                "{}",
                "{\"id\":\"LOC-2\",\"nom\":\"TEST\",\"prenom\":\"Integration\"}",
                "{\"id\":\"LOC-1\",\"nom\":\" \",\"prenom\":\"Integration\"}",
                "{\"id\":\"LOC-1\",\"nom\":\"TEST\"}"}) {
            reponse = corps;
            assertThrows(ServiceExterneException.class, () -> locatairesAhmed.consulter("LOC-1"));
        }
    }

    @Test
    void doitConserver404Ahmed() {
        statut = 404;
        var erreur = assertThrows(ServiceExterneException.class, () -> locatairesAhmed.consulter("LOC-1"));
        assertEquals(Integer.valueOf(404), erreur.getStatutHttp());
    }

    @Test
    void contratHabituelResteStrict() {
        reponse = "{\"id\":\"LOC-1\",\"nom\":\"TEST\",\"prenom\":\"Integration\"}";
        assertThrows(ServiceExterneException.class, () -> locataires.consulter("LOC-1"));
    }

    @Test
    void doitRefuserUnBienSansBailleur() {
        reponse = """
                {
                  "id": "BIEN-1",
                  "adresse": "Adresse fictive",
                  "ville": "Agadir"
                }
                """;

        ServiceExterneException erreur = assertThrows(
                ServiceExterneException.class,
                () -> biensBaux.consulterBien("BIEN-1")
        );

        assertEquals("REPONSE_INCOHERENTE", erreur.getCode());
    }
}