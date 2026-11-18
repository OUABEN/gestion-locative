package com.gestionlocative.quittances;
import com.gestionlocative.quittances.client.*;
import com.gestionlocative.quittances.config.*;
import com.gestionlocative.quittances.config.ReferentielsProperties.Endpoint;
import com.gestionlocative.quittances.exception.ServiceExterneException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BiensLocalClientTest {
    private static final String ID = "0f717fed-23b1-4697-bd1d-5dedf0307e60";
    @Test void adapteAdresseEtRefuseUnAutreBien() throws Exception {
        var serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.createContext("/api/biens/", e -> {
            byte[] corps = ("{\"id\":\"" + ID + "\",\"adresse\":\"15 rue du Test\",\"ville\":\"Agadir\",\"charges\":200}").getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type", "application/json");
            e.sendResponseHeaders(200, corps.length);
            e.getResponseBody().write(corps);
            e.close();
        });
        serveur.start();
        try {
            var delegate = mock(BienBailClient.class);
            var client = new BiensLocalClient(delegate, RestClient.builder()
                    .baseUrl("http://127.0.0.1:" + serveur.getAddress().getPort()).build(), ID);
            assertEquals("15 rue du Test, Agadir", client.consulterBien(ID).adresse());
            assertEquals("BAILLEUR-TEST", client.consulterBien(ID).bailleurId());
            assertThrows(ServiceExterneException.class, () -> client.consulterBien("autre"));
            var debut = LocalDate.of(2026, 10, 1);
            client.consulterBail("bail", debut, debut);
            verify(delegate).consulterBail("bail", debut, debut);
        } finally { serveur.stop(0); }
    }
    @Test void exigeDemoBauxSimulesEtJeton() {
        var config = new ReferentielsConfiguration();
        ReflectionTestUtils.setField(config, "bienIntegrationId", ID);
        var biens = new Endpoint("BIENS_LOCAL", "http://groupe16-biens-integration:8081", "/api/biens/{id}", "test-token");
        var baux = new Endpoint("SIMULATION", "http://services-demo:8091", "/baux/{id}?debut={debut}&fin={fin}", "");
        try (var transport = HttpClient.newHttpClient()) {
            assertThrows(IllegalArgumentException.class, () -> config.bienBailClient(new ReferentielsProperties(null, biens, baux), transport));
            ReflectionTestUtils.setField(config, "dockerDemo", true);
            assertInstanceOf(BiensLocalClient.class, config.bienBailClient(new ReferentielsProperties(null, biens, baux), transport));
            var sansToken = new Endpoint("BIENS_LOCAL", biens.baseUrl(), biens.chemin(), "");
            assertThrows(IllegalArgumentException.class, () -> config.bienBailClient(new ReferentielsProperties(null, sansToken, baux), transport));
            var distant = new Endpoint("BIENS_LOCAL", "http://example.com", biens.chemin(), "token");
            assertThrows(IllegalArgumentException.class, () -> config.bienBailClient(new ReferentielsProperties(null, distant, baux), transport));
            var bauxReels = new Endpoint("INTEGRATION", baux.baseUrl(), baux.chemin(), "token");
            assertThrows(IllegalArgumentException.class, () -> config.bienBailClient(new ReferentielsProperties(null, biens, bauxReels), transport));
        }
    }
}
