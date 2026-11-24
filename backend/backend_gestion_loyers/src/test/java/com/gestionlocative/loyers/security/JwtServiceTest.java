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

    /** Jeton au même format que celui émis par backend_gestion_biens : sub, role, email (HS256). */
    private static String jeton(String secret, String role, Instant expiration) {
        return Jwts.builder()
                .subject("utilisateur-1")
                .claim("role", role)
                .claim("email", "ayoub@locative.ma")
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    @DisplayName("UT-LOY-34 Secret trop court -> IllegalStateException au démarrage")
    void ut_loy_34_secret_trop_court_est_refuse() {
        assertThatThrownBy(() -> new JwtService("court"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("UT-LOY-35 Jeton valide -> sujet, rôle et e-mail lus correctement")
    void ut_loy_35_jeton_valide() {
        Claims claims = new JwtService(SECRET).parse(jeton(SECRET, "BAILLEUR", Instant.now().plusSeconds(300)));

        assertThat(claims.getSubject()).isEqualTo("utilisateur-1");
        assertThat(claims.get("role", String.class)).isEqualTo("BAILLEUR");
        assertThat(claims.get("email", String.class)).isEqualTo("ayoub@locative.ma");
    }

    @Test
    @DisplayName("UT-LOY-36 Jeton signé avec un autre secret -> rejeté")
    void ut_loy_36_jeton_signe_avec_un_autre_secret_est_rejete() {
        String jetonEtranger = jeton(AUTRE_SECRET, "BAILLEUR", Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> new JwtService(SECRET).parse(jetonEtranger))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("UT-LOY-37 Jeton expiré -> rejeté")
    void ut_loy_37_jeton_expire_est_rejete() {
        String expire = jeton(SECRET, "BAILLEUR", Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> new JwtService(SECRET).parse(expire))
                .isInstanceOf(JwtException.class);
    }
}
