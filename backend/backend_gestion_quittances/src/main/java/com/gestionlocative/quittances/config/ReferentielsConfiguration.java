package com.gestionlocative.quittances.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Set;

import com.gestionlocative.quittances.client.BienBailClient;
import com.gestionlocative.quittances.client.HttpBienBailClient;
import com.gestionlocative.quittances.client.HttpLocataireClient;
import com.gestionlocative.quittances.client.LocataireClient;
import com.gestionlocative.quittances.config.ReferentielsProperties.Endpoint;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ReferentielsProperties.class)
public class ReferentielsConfiguration {
    @Value("${DARLOC_DOCKER_DEMO:false}")
    private boolean dockerDemo;


    @Value("${DARLOC_BIEN_INTEGRATION_ID:}")
    private String bienIntegrationId = "";

    @Bean(name = "referentielsHttpTransport", destroyMethod = "close")
    public HttpClient referentielsHttpTransport() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Bean
    public LocataireClient locataireClient(
            ReferentielsProperties properties,
            @Qualifier("referentielsHttpTransport") HttpClient transport) {

        Endpoint endpoint = properties.locataires();

        return new HttpLocataireClient(
                construire(endpoint, transport, true),
                endpoint.chemin(),
                "LOCATAIRES_LOCAL".equals(endpoint.mode())
        );
    }

    @Bean
    public BienBailClient bienBailClient(
            ReferentielsProperties properties,
            @Qualifier("referentielsHttpTransport") HttpClient transport) {

        Endpoint biens = properties.biens();
        Endpoint baux = properties.baux();

        boolean biensLocal = "BIENS_LOCAL".equals(biens.mode());
        if (biensLocal) {
            if (!dockerDemo || !"SIMULATION".equals(baux.mode())
                    || !"http://groupe16-biens-integration:8081".equals(biens.baseUrl())
                    || !"/api/biens/{id}".equals(biens.chemin())) {
                throw new IllegalArgumentException("BIENS_LOCAL exige Docker demo, le service Biens local et des baux simules.");
            }
            java.util.UUID.fromString(bienIntegrationId);
            biens = new Endpoint("INTEGRATION", biens.baseUrl(), biens.chemin(), biens.token());
        }
        RestClient biensClient = construire(biens, transport);
        RestClient bauxClient = construire(baux, transport);

        if (!baux.chemin().contains("{debut}")
                || !baux.chemin().contains("{fin}")) {
            throw new IllegalArgumentException(
                    "Le chemin des baux doit contenir debut et fin."
            );
        }

        BienBailClient delegate = new HttpBienBailClient(
                biensClient,
                biens.chemin(),
                bauxClient,
                baux.chemin()
        );
        return biensLocal ? new com.gestionlocative.quittances.client.BiensLocalClient(
                delegate, biensClient, bienIntegrationId) : delegate;
    }

    private RestClient construire(Endpoint endpoint, HttpClient transport) {
        return construire(endpoint, transport, false);
    }

    private RestClient construire(Endpoint endpoint, HttpClient transport, boolean locataires) {
        if (endpoint == null
                || endpoint.baseUrl() == null
                || endpoint.chemin() == null) {
            throw new IllegalArgumentException(
                    "La configuration du service externe est incomplète."
            );
        }

        URI uri = URI.create(endpoint.baseUrl());

        if (uri.getHost() == null
                || !Set.of("http", "https").contains(uri.getScheme())
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "L'adresse du service externe est invalide."
            );
        }

        if (!endpoint.chemin().startsWith("/")
                || endpoint.chemin().startsWith("//")
                || !endpoint.chemin().contains("{id}")) {
            throw new IllegalArgumentException(
                    "Le chemin doit être relatif au service et contenir id."
            );
        }

        String token = endpoint.token() == null ? "" : endpoint.token();

        if ("LOCATAIRES_LOCAL".equals(endpoint.mode())) {
            boolean boucleLocale = Set.of("127.0.0.1", "localhost", "::1", "[::1]").contains(uri.getHost());
            boolean conteneurLocal = dockerDemo
                    && "locataires-integration".equals(uri.getHost()) && uri.getPort() == 8082;
            if (!locataires || !"http".equals(uri.getScheme())
                    || !(boucleLocale || conteneurLocal) || !token.isBlank()) {
                throw new IllegalArgumentException("LOCATAIRES_LOCAL exige le service local sans jeton.");
            }
        } else if ("SIMULATION".equals(endpoint.mode())) {
            if (!ReseauDemo.sourceAutorisee(uri, dockerDemo)
                    || !token.isBlank()) {
                throw new IllegalArgumentException(
                        "La simulation exige une adresse locale et aucun jeton réel."
                );
            }
        } else if ("INTEGRATION".equals(endpoint.mode())) {
            if (token.isBlank()) {
                throw new IllegalArgumentException(
                        "Un jeton de service est requis pour l'intégration."
                );
            }
        } else if ("UTILISATEUR".equals(endpoint.mode())) {
            if (!token.isBlank()) {
                throw new IllegalArgumentException(
                        "Le mode UTILISATEUR interdit les jetons statiques."
                );
            }
        } else {
            throw new IllegalArgumentException(
                    "Le mode doit être SIMULATION, INTEGRATION ou UTILISATEUR."
            );
        }

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(transport);

        factory.setReadTimeout(Duration.ofSeconds(5));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(endpoint.baseUrl())
                .requestFactory(factory)
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                );

        if ("INTEGRATION".equals(endpoint.mode())) {
            builder.defaultHeaders(headers -> headers.setBearerAuth(token));
        } else if ("UTILISATEUR".equals(endpoint.mode())) {
            builder.requestInterceptor(new JwtUtilisateurInterceptor());
        }

        return builder.build();
    }
}