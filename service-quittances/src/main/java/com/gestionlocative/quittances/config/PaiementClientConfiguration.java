package com.gestionlocative.quittances.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Set;

import com.gestionlocative.quittances.client.HttpPaiementClient;
import com.gestionlocative.quittances.client.PaiementClient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class PaiementClientConfiguration {
    @Value("${DARLOC_DOCKER_DEMO:false}")
    private boolean dockerDemo;


    @Bean(name = "paiementHttpTransport", destroyMethod = "close")
    public HttpClient paiementHttpTransport() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Bean
    public PaiementClient paiementClient(
            @Qualifier("paiementHttpTransport") HttpClient transport,
            @Value("${quittances.integration.paiements.base-url}") String baseUrl,
            @Value("${quittances.integration.paiements.chemin}") String chemin,
            @Value("${quittances.integration.paiements.mode}") String mode,
            @Value("${quittances.integration.paiements.token:}") String token) {

        URI uri = URI.create(baseUrl);

        if (uri.getHost() == null
                || !Set.of("http", "https").contains(uri.getScheme())
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "L'adresse du service Paiements est invalide."
            );
        }

        if (!chemin.startsWith("/")
                || chemin.startsWith("//")
                || !chemin.contains("{bailId}")
                || !chemin.contains("{debut}")
                || !chemin.contains("{fin}")) {
            throw new IllegalArgumentException(
                    "Le chemin Paiements doit contenir les paramètres attendus."
            );
        }

        if ("SIMULATION".equals(mode)) {
            if (!ReseauDemo.sourceAutorisee(uri, dockerDemo)) {
                throw new IllegalArgumentException(
                        "Le mode SIMULATION est limité à une adresse locale."
                );
            }

            if (!token.isBlank()) {
                throw new IllegalArgumentException(
                        "Ne configure pas de jeton réel en mode SIMULATION."
                );
            }

        } else if ("INTEGRATION".equals(mode)) {
            if (token.isBlank()) {
                throw new IllegalArgumentException(
                        "Un jeton de service est requis en mode INTEGRATION."
                );
            }

        } else {
            throw new IllegalArgumentException(
                    "Le mode Paiements doit être SIMULATION ou INTEGRATION."
            );
        }

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(transport);

        factory.setReadTimeout(Duration.ofSeconds(5));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                );

        if ("INTEGRATION".equals(mode)) {
            builder.defaultHeaders(headers -> headers.setBearerAuth(token));
        }

        return new HttpPaiementClient(builder.build(), chemin);
    }
}