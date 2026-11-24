package com.gestionlocative.quittances;

import com.gestionlocative.quittances.config.ReferentielsConfiguration;
import com.gestionlocative.quittances.config.ReferentielsProperties;
import com.gestionlocative.quittances.config.ReferentielsProperties.Endpoint;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class LocatairesLocalConfigurationTest {
    private Endpoint endpoint(String url, String token) {
        return new Endpoint("LOCATAIRES_LOCAL", url, "/api/v1/locataires/{id}/quittance-summary", token);
    }
    @Test void autoriseUniquementLeServiceLocalSansJeton() {
        var config = new ReferentielsConfiguration();
        try (var transport = HttpClient.newHttpClient()) {
            assertNotNull(config.locataireClient(new ReferentielsProperties(
                    endpoint("http://127.0.0.1:18082", ""), null, null), transport));
            for (var e : new Endpoint[] { endpoint("http://example.com", ""),
                    endpoint("http://127.0.0.1:18082", "secret"),
                    endpoint("http://locataires-integration:8082", "") }) {
                assertThrows(IllegalArgumentException.class, () -> config.locataireClient(
                        new ReferentielsProperties(e, null, null), transport));
            }
            ReflectionTestUtils.setField(config, "dockerDemo", true);
            assertNotNull(config.locataireClient(new ReferentielsProperties(
                    endpoint("http://locataires-integration:8082", ""), null, null), transport));
            assertThrows(IllegalArgumentException.class, () -> config.bienBailClient(
                    new ReferentielsProperties(null, endpoint("http://127.0.0.1:18082", ""),
                            endpoint("http://127.0.0.1:18082", "")), transport));
        }
    }
    @Test void integrationAuthentifieeExigeToujoursUnJeton() {
        var config = new ReferentielsConfiguration();
        try (var transport = HttpClient.newHttpClient()) {
            var e = new Endpoint("INTEGRATION", "http://127.0.0.1:18082", "/locataires/{id}", "");
            assertThrows(IllegalArgumentException.class, () -> config.locataireClient(
                    new ReferentielsProperties(e, null, null), transport));
        }
    }
}
