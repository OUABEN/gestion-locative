package com.gestionlocative.quittances.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentiteAuditTest {

    @Test
    void modeCommunTraceLeSujetSansPretendreAUnIssuer() {
        var authentication = authentification(
                Jwt.withTokenValue("test")
                        .header("alg", "HS256")
                        .subject("sub-verifie")
                        .claim("iss", "issuer-non-contractuel")
                        .build()
        );

        IdentiteAudit.Acteur acteur =
                new IdentiteAudit("COMMUN").depuis(authentication);

        assertEquals("sub-verifie", acteur.sujet());
        assertEquals(
                IdentiteAudit.SOURCE_JWT_COMMUN,
                acteur.emetteurIdentite()
        );
    }

    @Test
    void modesRs256ConserventIssuer() {
        Jwt jwt = Jwt.withTokenValue("test")
                .header("alg", "RS256")
                .subject("sujet-local")
                .issuer("urn:gestionlocative:dev")
                .build();
        var authentication = authentification(jwt);

        for (String mode : List.of("LOCAL", "INTEGRATION")) {
            IdentiteAudit.Acteur acteur =
                    new IdentiteAudit(mode).depuis(authentication);

            assertEquals("sujet-local", acteur.sujet());
            assertEquals("urn:gestionlocative:dev", acteur.emetteurIdentite());
        }
    }

    @Test
    void modesRs256RefusentIssuerAbsent() {
        var authentication = authentification(
                Jwt.withTokenValue("test")
                        .header("alg", "RS256")
                        .subject("sujet-local")
                        .build()
        );

        assertThrows(
                AccessDeniedException.class,
                () -> new IdentiteAudit("LOCAL").depuis(authentication)
        );
        assertThrows(
                AccessDeniedException.class,
                () -> new IdentiteAudit("INTEGRATION").depuis(authentication)
        );
    }

    private JwtAuthenticationToken authentification(Jwt jwt) {
        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_BAILLEUR"))
        );
    }
}
