package com.gestionlocative.quittances.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quittances.referentiels")
public record ReferentielsProperties(
        Endpoint locataires,
        Endpoint biens,
        Endpoint baux
) {

    public record Endpoint(
            String mode,
            String baseUrl,
            String chemin,
            String token
    ) {

        @Override
        public String toString() {
            return "Endpoint[configuration masquée]";
        }
    }
}