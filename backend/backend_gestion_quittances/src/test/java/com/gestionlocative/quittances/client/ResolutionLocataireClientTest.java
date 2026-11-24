package com.gestionlocative.quittances.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResolutionLocataireClientTest {

    @Test
    void refuseResolutionSansModeUtilisateur() {
        var builder = RestClient.builder()
                .baseUrl("http://localhost:8082");

        var serveur = MockRestServiceServer.bindTo(builder).build();

        var client = new HttpLocataireClient(
                builder.build(),
                "/api/locataires/{id}/quittance-summary",
                true,
                false);

        assertThrows(AccessDeniedException.class,
                client::identifierConnecte);

        serveur.verify();
    }

    @Test
    void accepteResolutionEnModeUtilisateur() {
        var builder = RestClient.builder()
                .baseUrl("http://localhost:8082");

        var serveur = MockRestServiceServer.bindTo(builder).build();

        serveur.expect(requestTo(
                "http://localhost:8082/api/locataires/me"))
                .andRespond(withSuccess(
                        "{\"id\":\"fiche-locataire-test\"}",
                        MediaType.APPLICATION_JSON));

        var client = new HttpLocataireClient(
                builder.build(),
                "/api/locataires/{id}/quittance-summary",
                true,
                true);

        assertEquals("fiche-locataire-test",
                client.identifierConnecte());

        serveur.verify();
    }


    @Test
    void refuseResolutionParDefaut() {
        LocataireClient client = id -> null;

        assertThrows(AccessDeniedException.class,
                client::identifierConnecte);
    }

    @Test
    void refuseResolutionSurErreursAuthentification() {
        for (int statut : new int[]{401, 403, 404}) {
            var builder = RestClient.builder()
                    .baseUrl("http://localhost:8082");

            var serveur = MockRestServiceServer
                    .bindTo(builder).build();

            serveur.expect(requestTo(
                    "http://localhost:8082/api/locataires/me"))
                    .andRespond(org.springframework.test.web.client
                            .response.MockRestResponseCreators
                            .withStatus(org.springframework.http
                                    .HttpStatus.valueOf(statut)));

            var client = new HttpLocataireClient(
                    builder.build(),
                    "/api/locataires/{id}/quittance-summary",
                    true, true);

            assertThrows(AccessDeniedException.class,
                    client::identifierConnecte);

            serveur.verify();
        }
    }

    @Test
    void signaleIndisponibiliteDuService() {
        var builder = RestClient.builder()
                .baseUrl("http://localhost:8082");

        var serveur = MockRestServiceServer.bindTo(builder).build();

        serveur.expect(requestTo(
                "http://localhost:8082/api/locataires/me"))
                .andRespond(org.springframework.test.web.client
                        .response.MockRestResponseCreators
                        .withServerError());

        var client = new HttpLocataireClient(
                builder.build(),
                "/api/locataires/{id}/quittance-summary",
                true, true);

        assertThrows(
                com.gestionlocative.quittances.exception
                        .ServiceExterneException.class,
                client::identifierConnecte);

        serveur.verify();
    }

    @Test
    void refuseFicheSansIdentifiantValide() {
        for (String json : new String[]{
                "{}",
                "{\"id\":\"\"}",
                "{\"id\":\"   \"}",
                "{\"id\":\" locataire-test \"}"
        }) {
            var builder = RestClient.builder()
                    .baseUrl("http://localhost:8082");

            var serveur = MockRestServiceServer
                    .bindTo(builder).build();

            serveur.expect(requestTo(
                    "http://localhost:8082/api/locataires/me"))
                    .andRespond(withSuccess(
                            json, MediaType.APPLICATION_JSON));

            var client = new HttpLocataireClient(
                    builder.build(),
                    "/api/locataires/{id}/quittance-summary",
                    true, true);

            assertThrows(
                    com.gestionlocative.quittances.exception
                            .ServiceExterneException.class,
                    client::identifierConnecte);

            serveur.verify();
        }
    }

    @Test
    void signaleErreurDeTransport() {
        var builder = RestClient.builder()
                .baseUrl("http://localhost:8082");

        var serveur = MockRestServiceServer.bindTo(builder).build();

        serveur.expect(requestTo(
                "http://localhost:8082/api/locataires/me"))
                .andRespond(requete -> {
                    throw new java.io.IOException(
                            "Connexion interrompue.");
                });

        var client = new HttpLocataireClient(
                builder.build(),
                "/api/locataires/{id}/quittance-summary",
                true, true);

        assertThrows(
                com.gestionlocative.quittances.exception
                        .ServiceExterneException.class,
                client::identifierConnecte);

        serveur.verify();
    }

}