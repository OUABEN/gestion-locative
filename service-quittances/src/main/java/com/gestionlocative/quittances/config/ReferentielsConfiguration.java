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
                construire(endpoint, transport),
                endpoint.chemin()
        );
    }

    @Bean
    public BienBailClient bienBailClient(
            ReferentielsProperties properties,
            @Qualifier("referentielsHttpTransport") HttpClient transport) {

        Endpoint biens = properties.biens();
        Endpoint baux = properties.baux();

        RestClient biensClient = construire(biens, transport);
        RestClient bauxClient = construire(baux, transport);

        if (!baux.chemin().contains("{debut}")
                || !baux.chemin().contains("{fin}")) {
            throw new IllegalArgumentException(
                    "Le chemin des baux doit contenir debut et fin."
            );
        }

        return new HttpBienBailClient(
                biensClient,
                biens.chemin(),
                bauxClient,
                baux.chemin()
        );
    }

    private RestClient construire(Endpoint endpoint, HttpClient transport) {
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

        if ("SIMULATION".equals(endpoint.mode())) {
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
        } else {
            throw new IllegalArgumentException(
                    "Le mode doit être SIMULATION ou INTEGRATION."
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
        }

        return builder.build();
    }
}