package com.gestionlocative.quittances;

import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.repository.ValidationTraceRepository;
import com.gestionlocative.quittances.security.DecodeurJwtCommun;
import com.gestionlocative.quittances.security.IdentiteAudit;
import com.gestionlocative.quittances.security.ValidationJwt;
import com.gestionlocative.quittances.service.ValidationQuittanceService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ValidationQuittanceServiceTest {

    @Autowired
    private ValidationQuittanceService service;

    @Autowired
    private QuittanceRepository repository;

    @Autowired
    private ValidationTraceRepository traceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void doitValiderEtConserverUneTraceSansEmettre() {
        String bailleur = identifiant();
        Quittance document = creer(bailleur);

        var resultat = service.valider(
                identite("BAILLEUR", bailleur),
                document.getId(),
                true
        );

        assertEquals("EN_ATTENTE_SIGNATURE", resultat.statut());
        assertEquals("EN_ATTENTE", resultat.statutSignature());
        assertNull(document.getEmisLe());
        assertNull(document.getNumero());

        Long validations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quittance_validation "
                        + "WHERE quittance_id = ?",
                Long.class,
                document.getId()
        );

        Long audits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quittance_audit "
                        + "WHERE quittance_id = ? "
                        + "AND operation = 'VALIDATION' "
                        + "AND resultat = 'SUCCES'",
                Long.class,
                document.getId()
        );

        assertEquals(1L, validations.longValue());
        assertEquals(1L, audits.longValue());
    }

    @Test
    void doitTracerLeSubDuJwtCommunVerifieSansIssuer() throws Exception {
        String secret = "secret-fictif-validation-commune-32-octets";
        String sujet = UUID.randomUUID().toString();
        NimbusJwtDecoder decodeur = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(
                        secret.getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256"
                ))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        ValidationJwt.configurerCommun(decodeur);

        Jwt jwt = new DecodeurJwtCommun(decodeur).decode(
                signerJwtCommun(secret, sujet)
        );

        assertNull(jwt.getClaimAsString("iss"));
        assertEquals(sujet, jwt.getSubject());
        assertEquals(sujet, jwt.getClaimAsString("bailleur_id"));

        Quittance document = creer(sujet);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_BAILLEUR"))
        );
        ValidationQuittanceService serviceCommun =
                new ValidationQuittanceService(
                        repository,
                        traceRepository,
                        new IdentiteAudit("COMMUN")
                );

        serviceCommun.valider(authentication, document.getId(), true);

        assertEquals(
                sujet,
                jdbcTemplate.queryForObject(
                        "SELECT acteur FROM quittance_validation WHERE quittance_id = ?",
                        String.class,
                        document.getId()
                )
        );
        assertEquals(
                IdentiteAudit.SOURCE_JWT_COMMUN,
                jdbcTemplate.queryForObject(
                        "SELECT emetteur_identite FROM quittance_validation "
                                + "WHERE quittance_id = ?",
                        String.class,
                        document.getId()
                )
        );
    }

    @Test
    void doitRefuserUneDeuxiemeValidation() {
        String bailleur = identifiant();
        Quittance document = creer(bailleur);
        var utilisateur = identite("BAILLEUR", bailleur);

        service.valider(utilisateur, document.getId(), true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.valider(
                        utilisateur,
                        document.getId(),
                        true
                )
        );

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void doitRefuserUnAutreBailleur() {
        Quittance document = creer(identifiant());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.valider(
                        identite("BAILLEUR", identifiant()),
                        document.getId(),
                        true
                )
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void doitRefuserUnLocataire() {
        Quittance document = creer(identifiant());

        assertThrows(
                AccessDeniedException.class,
                () -> service.valider(
                        identite("LOCATAIRE", identifiant()),
                        document.getId(),
                        true
                )
        );
    }

    @Test
    void doitExigerConfirmation() {
        String bailleur = identifiant();
        Quittance document = creer(bailleur);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.valider(
                        identite("BAILLEUR", bailleur),
                        document.getId(),
                        false
                )
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    private Quittance creer(String bailleur) {
        Quittance document = Quittance.creerBrouillon(
                identifiant(),
                identifiant(),
                identifiant(),
                bailleur,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                new BigDecimal("3000.00"),
                new BigDecimal("200.00"),
                new BigDecimal("3000.00"),
                new BigDecimal("200.00"),
                TypeDocument.QUITTANCE_INTEGRALE,
                Map.of(
                        "versionSnapshot", "1",
                        "bailleurNom", "Bailleur fictif",
                        "locataireNom", "Locataire fictif",
                        "locatairePrenom", "Test",
                        "bienAdresse", "Adresse fictive"
                )
        );

        return repository.saveAndFlush(document);
    }

    private JwtAuthenticationToken identite(
            String role,
            String identifiant
    ) {
        String claim = "BAILLEUR".equals(role)
                ? "bailleur_id"
                : "locataire_id";

        Jwt jwt = Jwt.withTokenValue("jeton-test")
                .header("alg", "RS256")
                .subject("sujet-test")
                .issuer("urn:gestionlocative:dev")
                .claim(claim, identifiant)
                .build();

        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    private String identifiant() {
        return "TEST-VALIDATION-" + UUID.randomUUID();
    }

    private String signerJwtCommun(String secret, String sujet)
            throws Exception {
        Instant maintenant = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(sujet)
                .claim("role", "BAILLEUR")
                .issueTime(Date.from(maintenant.minusSeconds(60)))
                .expirationTime(Date.from(maintenant.plusSeconds(900)))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims
        );
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}