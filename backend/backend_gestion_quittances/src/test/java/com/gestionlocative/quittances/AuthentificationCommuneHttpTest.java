package com.gestionlocative.quittances;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.util.Base64URL;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class AuthentificationCommuneHttpTest {

    private static final String SECRET =
            "secret-fictif-http-quittances-32-octets";

    @Value("${local.server.port}")
    private int port;

    @DynamicPropertySource
    static void proprietesCommunes(DynamicPropertyRegistry proprietes) {
        proprietes.add("quittances.securite.mode", () -> "COMMUN");
        proprietes.add("quittances.securite.jwt-secret", () -> SECRET);
    }

    @Test
    void doitRefuserIdentiteSansJwt() throws Exception {
        assertEquals(401, appelerGet("/api/v1/identite", null).statusCode());
    }

    @Test
    void doitAccepterHs256ValideSurIdentite() throws Exception {
        String token = signerHs256(
                "compte-uuid-test",
                "BAILLEUR",
                Instant.now().plusSeconds(900),
                true
        );

        HttpResponse<String> reponse =
                appelerGet("/api/v1/identite", token);

        assertEquals(200, reponse.statusCode());
        assertTrue(reponse.body().contains("\"sujet\":\"compte-uuid-test\""));
        assertTrue(reponse.body().contains("ROLE_BAILLEUR"));
    }

    @Test
    void doitRefuserUneSignatureHs256Incorrecte() throws Exception {
        String token = signerHs256(
                "compte-uuid-test",
                "BAILLEUR",
                Instant.now().plusSeconds(900),
                true,
                "mauvais-secret-fictif-32-octets-xxxx"
        );

        assertEquals(401,
                appelerGet("/api/v1/identite", token).statusCode());
    }

    @Test
    void doitRefuserUnJwtExpire() throws Exception {
        String token = signerHs256(
                "compte-uuid-test",
                "LOCATAIRE",
                Instant.now().minusSeconds(120),
                true
        );

        assertEquals(401,
                appelerGet("/api/v1/identite", token).statusCode());
    }

    @Test
    void doitRefuserLesClaimsObligatoiresAbsents() throws Exception {
        String sansSub = signerHs256(null, "BAILLEUR",
                Instant.now().plusSeconds(900), true);
        String sansRole = signerHs256("compte-uuid-test", null,
                Instant.now().plusSeconds(900), true);
        String sansIat = signerHs256("compte-uuid-test", "BAILLEUR",
                Instant.now().plusSeconds(900), false);
        String sansExp = signerHs256("compte-uuid-test", "BAILLEUR",
                null, true);

        assertEquals(401,
                appelerGet("/api/v1/identite", sansSub).statusCode());
        assertEquals(401,
                appelerGet("/api/v1/identite", sansRole).statusCode());
        assertEquals(401,
                appelerGet("/api/v1/identite", sansIat).statusCode());
        assertEquals(401,
                appelerGet("/api/v1/identite", sansExp).statusCode());
    }

    @Test
    void doitRefuserRs256EtNoneEnModeCommun() throws Exception {
        String rs256 = signerRs256(
                "compte-uuid-test",
                "BAILLEUR",
                Instant.now().plusSeconds(900)
        );
        String none = signerNone(
                "compte-uuid-test",
                "BAILLEUR",
                Instant.now().plusSeconds(900)
        );

        assertEquals(401,
                appelerGet("/api/v1/identite", rs256).statusCode());
        assertEquals(401,
                appelerGet("/api/v1/identite", none).statusCode());
    }

    @Test
    void doitRefuserUnRoleInconnu() throws Exception {
        String token = signerHs256(
                "compte-uuid-test",
                "ADMIN",
                Instant.now().plusSeconds(900),
                true
        );

        assertEquals(401,
                appelerGet("/api/v1/identite", token).statusCode());
    }

    @Test
    void doitAutoriserIdentiteSansRattachementMaisRefuserCreation() throws Exception {
        String token = signerHs256(
                "compte-sans-rattachement",
                "BAILLEUR",
                Instant.now().plusSeconds(900),
                true
        );

        assertEquals(200,
                appelerGet("/api/v1/identite", token).statusCode());
        assertEquals(403,
                appelerPost("/api/v1/quittances", token,
                        """
                        {
                          "bailId":"BAIL-TEST",
                          "periodeDebut":"2026-10-01",
                          "periodeFin":"2026-10-31"
                        }
                        """).statusCode());
    }

    private HttpResponse<String> appelerGet(String chemin, String token)
            throws Exception {
        HttpRequest.Builder requete = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + chemin))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET();
        if (token != null) {
            requete.header("Authorization", "Bearer " + token);
        }
        return HttpClient.newHttpClient().send(
                requete.build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> appelerPost(
            String chemin,
            String token,
            String corps
    ) throws Exception {
        HttpRequest.Builder requete = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + chemin))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corps));
        if (token != null) {
            requete.header("Authorization", "Bearer " + token);
        }
        return HttpClient.newHttpClient().send(
                requete.build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private String signerHs256(
            String sub,
            String role,
            Instant expiration,
            boolean inclureIat
    ) throws Exception {
        return signerHs256(sub, role, expiration, inclureIat, SECRET);
    }

    private String signerHs256(
            String sub,
            String role,
            Instant expiration,
            boolean inclureIat,
            String secret
    ) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder();
        if (sub != null) {
            claims.subject(sub);
        }
        if (role != null) {
            claims.claim("role", role);
        }
        if (inclureIat) {
            claims.issueTime(Date.from(Instant.now().minusSeconds(60)));
        }
        if (expiration != null) {
            claims.expirationTime(Date.from(expiration));
        }

        SignedJWT jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims.build()
        );
        jwt.sign(new MACSigner(
                secret.getBytes(StandardCharsets.UTF_8)
        ));
        return jwt.serialize();
    }

    private String signerRs256(
            String sub,
            String role,
            Instant expiration
    ) throws Exception {
        KeyPairGenerator generateur =
                KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        KeyPair paire = generateur.generateKeyPair();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(sub)
                .issueTime(Date.from(Instant.now().minusSeconds(60)))
                .expirationTime(Date.from(expiration))
                .claim("role", role)
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.RS256),
                claims
        );
        jwt.sign(new RSASSASigner(
                (RSAPrivateKey) paire.getPrivate()
        ));
        return jwt.serialize();
    }

    private String signerNone(
            String sub,
            String role,
            Instant expiration
    ) throws Exception {
        String payload = "{\"sub\":\"" + sub
                + "\",\"role\":\"" + role
                + "\",\"iat\":" + Instant.now().minusSeconds(60).getEpochSecond()
                + ",\"exp\":" + expiration.getEpochSecond()
                + "}";
        return "eyJhbGciOiJub25lIn0."
                + Base64URL.encode(payload.getBytes(StandardCharsets.UTF_8)).toString()
                + ".";
    }
}
