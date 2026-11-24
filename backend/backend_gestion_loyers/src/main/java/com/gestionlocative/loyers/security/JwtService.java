package com.gestionlocative.loyers.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Valide les JWT émis par backend_gestion_biens (HS256, secret partagé JWT_SECRET).
 * Contenu attendu : sub = id utilisateur, role = BAILLEUR | GESTIONNAIRE | LOCATAIRE, email.
 */
@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret:}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret (variable JWT_SECRET) doit contenir au moins 32 caractères");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /** Lève JwtException si le token est invalide, falsifié ou expiré. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
