package com.gestionlocative.loyers.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String AUTRE_SECRET = "zyxwvutsrqponmlkjihgfedcba987654";

    @Test
    @DisplayName("UT-LOY-34 Secret trop court -> IllegalStateException au démarrage")
    void ut_loy_34_secret_trop_court_est_refuse() {
        assertThatThrownBy(() -> new JwtService("court"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("UT-LOY-35 Token de service valide -> rôle GESTIONNAIRE lu correctement")
    void ut_loy_35_token_de_service_valide() {
        JwtService service = new JwtService(SECRET);

        Claims claims = service.parse(service.genererTokenService());

        assertThat(claims.get("role", String.class)).isEqualTo("GESTIONNAIRE");
        assertThat(claims.getSubject()).isEqualTo("service-loyers");
    }

    @Test
    @DisplayName("UT-LOY-36 Token signé avec un autre secret -> rejeté")
    void ut_loy_36_token_signe_avec_un_autre_secret_est_rejete() {
        String tokenEtranger = new JwtService(AUTRE_SECRET).genererTokenService();

        assertThatThrownBy(() -> new JwtService(SECRET).parse(tokenEtranger))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("UT-LOY-37 Token expiré -> rejeté")
    void ut_loy_37_token_expire_est_rejete() {
        String expire = Jwts.builder()
                .subject("utilisateur")
                .claim("role", "BAILLEUR")
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> new JwtService(SECRET).parse(expire))
                .isInstanceOf(JwtException.class);
    }
}
