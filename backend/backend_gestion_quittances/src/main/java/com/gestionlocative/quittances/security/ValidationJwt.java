package com.gestionlocative.quittances.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

public final class ValidationJwt {

    private ValidationJwt() {
    }

    public static void configurerCommun(NimbusJwtDecoder decoder) {
        OAuth2TokenValidator<Jwt> contenuObligatoire = jwt -> {
            Object role = jwt.getClaims().get("role");
            Object iat = jwt.getClaims().get("iat");
            Object exp = jwt.getClaims().get("exp");
            boolean roleReconnu = role instanceof String
                    && Set.of("BAILLEUR", "GESTIONNAIRE", "LOCATAIRE")
                            .contains(role);
            long iatSecondes = iat instanceof Instant instant
                    ? instant.getEpochSecond()
                    : iat instanceof Number number
                            ? number.longValue()
                            : -1L;
            long expSecondes = exp instanceof Instant instant
                    ? instant.getEpochSecond()
                    : exp instanceof Number number
                            ? number.longValue()
                            : -1L;
            boolean tempsNumerique = iatSecondes > 0
                    && expSecondes > iatSecondes;

            boolean valide =
                    jwt.getSubject() != null
                            && !jwt.getSubject().isBlank()
                            && jwt.getIssuedAt() != null
                            && jwt.getExpiresAt() != null
                            && jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                            && tempsNumerique
                            && roleReconnu;

            if (valide) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                            "invalid_token",
                            "Le jeton commun ne contient pas les informations attendues.",
                            null
                    )
            );
        };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        new JwtTimestampValidator(Duration.ofSeconds(30)),
                        contenuObligatoire
                )
        );
    }

    public static void configurer(
            NimbusJwtDecoder decoder,
            String issuer,
            String audience) {

        OAuth2TokenValidator<Jwt> contenuObligatoire = jwt -> {
            boolean valide =
                    jwt.getSubject() != null
                            && !jwt.getSubject().isBlank()
                            && jwt.getIssuedAt() != null
                            && jwt.getExpiresAt() != null
                            && jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                            && jwt.getAudience().contains(audience);

            if (valide) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                            "invalid_token",
                            "Le jeton ne contient pas les informations attendues.",
                            null
                    )
            );
        };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        new JwtTimestampValidator(Duration.ofSeconds(30)),
                        new JwtIssuerValidator(issuer),
                        contenuObligatoire
                )
        );
    }
}