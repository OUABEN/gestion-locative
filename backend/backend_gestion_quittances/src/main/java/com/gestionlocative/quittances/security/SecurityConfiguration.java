package com.gestionlocative.quittances.security;

import com.gestionlocative.quittances.config.ReseauDemo;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfiguration {
    @Value("${DARLOC_DOCKER_DEMO:false}")
    private boolean dockerDemo;

    public static Collection<GrantedAuthority> autoritesRoleCommun(Jwt jwt) {
        Object role = jwt.getClaims().get("role");
        if ("BAILLEUR".equals(role)
                || "GESTIONNAIRE".equals(role)
                || "LOCATAIRE".equals(role)) {
            return java.util.List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );
        }
        return new LinkedHashSet<>();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${quittances.securite.mode}") String mode,
            @Value("${quittances.securite.issuer}") String issuer,
            @Value("${quittances.securite.audience}") String audience,
            @Value("${quittances.securite.cle-publique}") String clePublique,
            @Value("${quittances.securite.jwt-secret:}") String jwtSecret,
            @Value("${server.address}") String adresseServeur) throws Exception {

        if (!"COMMUN".equals(mode) && (issuer.isBlank() || audience.isBlank())) {
            throw new IllegalArgumentException(
                    "L'émetteur et l'audience JWT doivent être configurés."
            );
        }

        NimbusJwtDecoder decoder;

        if ("LOCAL".equals(mode)) {
            if (!ReseauDemo.ecouteAutorisee(adresseServeur, dockerDemo)
                    || !"urn:gestionlocative:dev".equals(issuer)
                    || !"service-quittances".equals(audience)) {
                throw new IllegalArgumentException(
                        "Le mode JWT LOCAL est réservé à la démonstration sur 127.0.0.1."
                );
            }

            byte[] contenu = Files.readAllBytes(Path.of(clePublique));

            RSAPublicKey cle = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(contenu));

            decoder = NimbusJwtDecoder.withPublicKey(cle)
                    .signatureAlgorithm(SignatureAlgorithm.RS256)
                    .build();

        } else if ("INTEGRATION".equals(mode)) {
            URI uri = URI.create(issuer);

            if (!"https".equals(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException(
                        "Le mode INTEGRATION exige un émetteur HTTPS valide."
                );
            }

            decoder = NimbusJwtDecoder.withIssuerLocation(issuer)
                    .jwsAlgorithm(SignatureAlgorithm.RS256)
                    .build();

        } else if ("COMMUN".equals(mode)) {
            if (jwtSecret.isBlank()) {
                throw new IllegalArgumentException(
                        "Le secret JWT commun doit être configuré."
                );
            }

            byte[] secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
            if (secret.length < 32) {
                throw new IllegalArgumentException(
                        "Le secret JWT commun doit contenir au moins 32 octets."
                );
            }

            decoder = NimbusJwtDecoder
                    .withSecretKey(new SecretKeySpec(secret, "HmacSHA256"))
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();

        } else {
            throw new IllegalArgumentException(
                    "Le mode JWT doit être LOCAL, INTEGRATION ou COMMUN."
            );
        }

        if ("COMMUN".equals(mode)) {
            ValidationJwt.configurerCommun(decoder);
            return new DecodeurJwtCommun(decoder);
        } else {
            ValidationJwt.configurer(decoder, issuer, audience);
        }

        return decoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${quittances.securite.mode}") String mode)
            throws Exception {

        JwtAuthenticationConverter authentication =
                new JwtAuthenticationConverter();

        if ("COMMUN".equals(mode)) {
            authentication.setJwtGrantedAuthoritiesConverter(jwt ->
                    autoritesRoleCommun(jwt)
            );
        } else {
            JwtGrantedAuthoritiesConverter roles =
                    new JwtGrantedAuthoritiesConverter();

            roles.setAuthoritiesClaimName("roles");
            roles.setAuthorityPrefix("ROLE_");
            authentication.setJwtGrantedAuthoritiesConverter(roles);
        }

        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                )
                .authorizeHttpRequests(authorisations -> authorisations
                    .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/",
                                "/quittances",
                                "/css/quittances.css",
                                "/js/quittances.js",
                                "/js/panneau-document.js",
                                "/vendor/bootstrap/bootstrap.min.css"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/systeme/etat",
                                "/health"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/identite"
                        ).authenticated()
                        .requestMatchers(
                                "/api/v1/quittances",
                                "/api/v1/quittances/**"
                        ).hasAnyRole("BAILLEUR", "LOCATAIRE")
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(authentication)
                        )
                );

        return http.build();
    }
}