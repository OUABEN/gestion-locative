package com.gestionlocative.quittances;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.gestionlocative.quittances.security.ValidationJwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationJwtTest {

    private KeyPair paire;
    private NimbusJwtDecoder decoder;

    @BeforeEach
    void preparer() throws Exception {
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        paire = generateur.generateKeyPair();

        decoder = NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) paire.getPublic())
                .build();

        ValidationJwt.configurer(
                decoder,
                "urn:gestionlocative:dev",
                "service-quittances"
        );
    }

    @Test
    void doitAccepterUnJetonValide() throws Exception {
        String token = signer(
                paire,
                "urn:gestionlocative:dev",
                "service-quittances",
                Instant.now().plusSeconds(900)
        );

        assertEquals("demo-1", decoder.decode(token).getSubject());
    }

    @Test
    void doitRefuserUneAutreSignature() throws Exception {
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);

        String token = signer(
                generateur.generateKeyPair(),
                "urn:gestionlocative:dev",
                "service-quittances",
                Instant.now().plusSeconds(900)
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void doitRefuserUnJetonExpire() throws Exception {
        String token = signer(
                paire,
                "urn:gestionlocative:dev",
                "service-quittances",
                Instant.now().minusSeconds(120)
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void doitRefuserUneAutreAudience() throws Exception {
        String token = signer(
                paire,
                "urn:gestionlocative:dev",
                "autre-service",
                Instant.now().plusSeconds(900)
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void doitRefuserUnAutreEmetteur() throws Exception {
        String token = signer(
                paire,
                "urn:autre-emetteur",
                "service-quittances",
                Instant.now().plusSeconds(900)
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void doitRefuserUneExpirationAbsente() throws Exception {
        String token = signer(
                paire,
                "urn:gestionlocative:dev",
                "service-quittances",
                null
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    private String signer(
            KeyPair cle,
            String issuer,
            String audience,
            Instant expiration) throws Exception {

        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject("demo-1")
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(Instant.now().minusSeconds(600)))
                .claim("roles", List.of("BAILLEUR"))
                .claim("bailleur_id", "BAILLEUR-TEST");

        if (expiration != null) {
            claims.expirationTime(Date.from(expiration));
        }

        SignedJWT jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.RS256),
                claims.build()
        );

        jwt.sign(new RSASSASigner((RSAPrivateKey) cle.getPrivate()));

        return jwt.serialize();
    }
}