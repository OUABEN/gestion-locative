package com.gestionlocative.quittances.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.gestionlocative.quittances.exception.RegleMetierException;
import com.gestionlocative.quittances.exception.ServiceExterneException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BienBailCollectifClientTest {
    @Test
    void refuseUnePeriodePartielleNonContractualisee() {
        RestClient client = RestClient.create("http://localhost:8081");
        var adaptateur = new BienBailCollectifClient(client, "/api/biens/{id}",
                client, "/api/baux/{id}");
        assertThrows(RegleMetierException.class,
                () -> adaptateur.consulterBail("bail-123", LocalDate.of(2026, 10, 2),
                        LocalDate.of(2026, 10, 31)));
    }

    @Test
    void refuseUnBienSansProprietaireVerifie() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8081");
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(builder).build();
        serveur.expect(requestTo("http://localhost:8081/api/biens/bien-123"))
                .andRespond(withSuccess("{\"id\":\"bien-123\",\"adresse\":\"Agadir\",\"ville\":\"Agadir\",\"charges\":0}",
                        MediaType.APPLICATION_JSON));
        var client = new BienBailCollectifClient(builder.build(), "/api/biens/{id}",
                builder.build(), "/api/baux/{id}");
        assertThrows(ServiceExterneException.class, () -> client.consulterBien("bien-123"));
        serveur.verify();
    }
}
