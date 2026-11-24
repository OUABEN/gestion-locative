package com.gestionlocative.quittances;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;

import com.gestionlocative.quittances.security.ValidationJwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.util.Base64URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.gestionlocative.quittances.security.SecurityConfiguration;
import com.gestionlocative.quittances.security.LecteurQuittances;
import com.gestionlocative.quittances.security.DecodeurJwtCommun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void doitAccepterUnJetonCommunHS256Valide() throws Exception {
        JwtDecoder decodeurCommun = decodeurCommun(
                "secret-fictif-commun-quittances-32-octets"
        );
        String sub = "59bc86bc-06e3-45a1-82bb-539bbaf671eb";
        String token = signerCommun(
                "secret-fictif-commun-quittances-32-octets",
                JWSAlgorithm.HS256,
                sub,
                "BAILLEUR",
                Instant.now().plusSeconds(900),
                true
        );

        Jwt jetonDecode = decodeurCommun.decode(token);

        assertEquals(sub, jetonDecode.getSubject());
        assertEquals("BAILLEUR", jetonDecode.getClaim("role"));
        assertEquals(sub, jetonDecode.getClaimAsString("bailleur_id"));
        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(
                        jetonDecode,
                        SecurityConfiguration.autoritesRoleCommun(jetonDecode)
                );
        assertEquals(
                sub,
                LecteurQuittances.depuis(authentication).identifiant()
        );
    }

    @Test
    void doitDeriverBailleurDuSubEtIgnorerUnBailleurIdFourni() throws Exception {
        String secret = "secret-fictif-commun-quittances-32-octets";
        String sub = "8f58f2dd-2302-4c65-96db-2c25cff527c1";
        JwtDecoder decodeurCommun = decodeurCommun(secret);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(sub)
                .issueTime(Date.from(Instant.now().minusSeconds(60)))
                .expirationTime(Date.from(Instant.now().plusSeconds(900)))
                .claim("role", "BAILLEUR")
                .claim("bailleur_id", "AUTRE-BAILLEUR")
                .build();
        SignedJWT token = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims
        );
        token.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));

        Jwt jetonDecode = decodeurCommun.decode(token.serialize());

        assertEquals(sub, jetonDecode.getClaimAsString("bailleur_id"));
    }

    @Test
    void neDerivePasDeRattachementBailleurPourUnLocataire() throws Exception {
        JwtDecoder decodeurCommun = decodeurCommun(
                "secret-fictif-commun-quittances-32-octets"
        );
        String token = signerCommun(
                "secret-fictif-commun-quittances-32-octets",
                JWSAlgorithm.HS256,
                "0e8c1ad4-fb25-4879-a912-1e9ff4319260",
                "LOCATAIRE",
                Instant.now().plusSeconds(900),
                true
        );

        Jwt jetonDecode = decodeurCommun.decode(token);

        assertNull(jetonDecode.getClaim("bailleur_id"));
    }

    @Test
    void doitRefuserUneSignatureCommunIncorrecte() throws Exception {
        JwtDecoder decodeurCommun = decodeurCommun(
                "secret-fictif-commun-quittances-32-octets"
        );
        String token = signerCommun(
                "autre-secret-fictif-32-octets-aaaaaaaa",
                JWSAlgorithm.HS256,
                "compte-uuid",
                "BAILLEUR",
                Instant.now().plusSeconds(900),
                true
        );

        assertThrows(JwtException.class, () -> decodeurCommun.decode(token));
    }

    @Test
    void doitRefuserUnJetonCommunExpire() throws Exception {
        JwtDecoder decodeurCommun = decodeurCommun(
                "secret-fictif-commun-quittances-32-octets"
        );
        String token = signerCommun(
                "secret-fictif-commun-quittances-32-octets",
                JWSAlgorithm.HS256,
                "compte-uuid",
                "LOCATAIRE",
                Instant.now().minusSeconds(120),
                true
        );

        assertThrows(JwtException.class, () -> decodeurCommun.decode(token));
    }

    @Test
    void doitRefuserUnAlgorithmeCommunInterdit() throws Exception {
        JwtDecoder decodeurCommun = decodeurCommun(
                "secret-fictif-commun-quittances-32-octets"
        );
        String token = signerCommun(
                "secret-fictif-commun-quittances-64-octets-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                JWSAlgorithm.HS512,
                "compte-uuid",
                "BAILLEUR",
                Instant.now().plusSeconds(900),
                true
        );

        assertThrows(JwtException.class, () -> decodeurCommun.decode(token));
    }

    @Test
    void doitRefuserUnJetonCommunNonSigne() throws Exception {
        JwtDecoder decodeurCommun = decodeurCommun(
                "secret-fictif-commun-quittances-32-octets"
        );
        String payload = Base64URL.encode(
                "{\"sub\":\"compte-uuid\",\"role\":\"BAILLEUR\",\"iat\":"
                        + Instant.now().minusSeconds(60).getEpochSecond()
                        + ",\"exp\":"
                        + Instant.now().plusSeconds(900).getEpochSecond()
                        + "}"
        ).toString();
        String token = "eyJhbGciOiJub25lIn0." + payload + ".";

        assertThrows(JwtException.class, () -> decodeurCommun.decode(token));
    }

    @Test
    void doitRefuserLesClaimsCommunsAbsents() throws Exception {
        JwtDecoder decodeurCommun = decodeurCommun(
                "secret-fictif-commun-quittances-32-octets"
        );

        String sansSub = signerCommun(
                "secret-fictif-commun-quittances-32-octets",
                JWSAlgorithm.HS256,
                null,
                "BAILLEUR",
                Instant.now().plusSeconds(900),
                true
        );
        String sansRole = signerCommun(
                "secret-fictif-commun-quittances-32-octets",
                JWSAlgorithm.HS256,
                "compte-uuid",
                null,
                Instant.now().plusSeconds(900),
                true
        );
        String sansIat = signerCommun(
                "secret-fictif-commun-quittances-32-octets",
                JWSAlgorithm.HS256,
                "compte-uuid",
                "BAILLEUR",
                Instant.now().plusSeconds(900),
                false
        );
        String sansExp = signerCommun(
                "secret-fictif-commun-quittances-32-octets",
                JWSAlgorithm.HS256,
                "compte-uuid",
                "BAILLEUR",
                null,
                true
        );

        assertThrows(JwtException.class, () -> decodeurCommun.decode(sansSub), "sansSub");
        assertThrows(JwtException.class, () -> decodeurCommun.decode(sansRole), "sansRole");
        assertThrows(JwtException.class, () -> decodeurCommun.decode(sansIat), "sansIat");
        assertThrows(JwtException.class, () -> decodeurCommun.decode(sansExp), "sansExp");
    }

    @Test
    void doitRefuserUnRoleCommunInconnu() throws Exception {
        JwtDecoder decodeurCommun = decodeurCommun(
                "secret-fictif-commun-quittances-32-octets"
        );
        String token = signerCommun(
                "secret-fictif-commun-quittances-32-octets",
                JWSAlgorithm.HS256,
                "compte-uuid",
                "ADMIN",
                Instant.now().plusSeconds(900),
                true
        );

        assertThrows(JwtException.class, () -> decodeurCommun.decode(token));
    }

    @Test
    void doitConvertirLesRolesCommunsEnAutoritesSpring() {
        Jwt bailleur = Jwt.withTokenValue("test")
                .header("alg", "HS256")
                .claim("role", "BAILLEUR")
                .build();
        Jwt locataire = Jwt.withTokenValue("test")
                .header("alg", "HS256")
                .claim("role", "LOCATAIRE")
                .build();
        Jwt gestionnaire = Jwt.withTokenValue("test")
                .header("alg", "HS256")
                .claim("role", "GESTIONNAIRE")
                .build();

        assertEquals(
                "ROLE_BAILLEUR",
                SecurityConfiguration.autoritesRoleCommun(bailleur)
                        .iterator().next().getAuthority()
        );
        assertEquals(
                "ROLE_LOCATAIRE",
                SecurityConfiguration.autoritesRoleCommun(locataire)
                        .iterator().next().getAuthority()
        );
        assertEquals(
                "ROLE_GESTIONNAIRE",
                SecurityConfiguration.autoritesRoleCommun(gestionnaire)
                        .iterator().next().getAuthority()
        );
    }

    @Test
    void doitRefuserUnCompteSansRattachementMetier() {
        Jwt jwt = Jwt.withTokenValue("test")
                .header("alg", "HS256")
                .subject("compte-sans-rattachement")
                .claim("role", "BAILLEUR")
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_BAILLEUR"))
        );

        assertThrows(
                AccessDeniedException.class,
                () -> LecteurQuittances.depuis(authentication)
        );
    }

    @Test
    void doitReconnaitreGestionnaireAvecDroitsEcriture() {
        Jwt jwt = Jwt.withTokenValue("test")
                .header("alg", "HS256")
                .subject("compte-gestionnaire")
                .claim("role", "GESTIONNAIRE")
                .build();

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(
                        jwt,
                        List.of(new SimpleGrantedAuthority("ROLE_GESTIONNAIRE"))
                );

        LecteurQuittances lecteur =
                LecteurQuittances.depuis(authentication);

        assertEquals("compte-gestionnaire", lecteur.identifiant());
        assertEquals(true, lecteur.gestionnaire());
        assertEquals(true, lecteur.peutEcrire());
        assertEquals(false, lecteur.bailleur());
        assertEquals(false, lecteur.locataire());
    }

    private JwtDecoder decodeurCommun(String secret) {
        NimbusJwtDecoder decodeur = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(
                        secret.getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256"
                ))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        ValidationJwt.configurerCommun(decodeur);
        return new DecodeurJwtCommun(decodeur);
    }

    private String signerCommun(
            String secret,
            JWSAlgorithm algorithme,
            String subject,
            String role,
            Instant expiration,
            boolean inclureIat
    ) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder();
        if (subject != null) {
            claims.subject(subject);
        }
        if (inclureIat) {
            claims.issueTime(Date.from(Instant.now().minusSeconds(600)));
        }
        if (expiration != null) {
            claims.expirationTime(Date.from(expiration));
        }
        if (role != null) {
            claims.claim("role", role);
        }

        SignedJWT jwt = new SignedJWT(
                new JWSHeader(algorithme),
                claims.build()
        );
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
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