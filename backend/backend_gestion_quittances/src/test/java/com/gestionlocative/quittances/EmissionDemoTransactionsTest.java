package com.gestionlocative.quittances;

import com.gestionlocative.quittances.document.SignatureLocale;
import com.gestionlocative.quittances.dto.DonneesApercu;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.mapper.QuittanceConsultationMapper;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.service.EmissionDemoTransactions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
@Execution(ExecutionMode.SAME_THREAD)
class EmissionDemoTransactionsTest {

    @Autowired
    private EmissionDemoTransactions transactions;

    @Autowired
    private QuittanceRepository quittances;

    @Autowired
    private QuittanceConsultationMapper mapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction;
    private UUID quittanceId;
    private String bailleurId;
    private EmissionDemoTransactions.Source source;
    private byte[] pdf;
    private SignatureLocale.Preuve preuve;

    @BeforeEach
    void preparerDocumentValide() {
        transaction = new TransactionTemplate(transactionManager);
        bailleurId = UUID.randomUUID().toString();
        quittanceId = transaction.execute(status -> {
            Quittance document = Quittance.creerBrouillon(
                    "BAIL-TX-" + UUID.randomUUID(),
                    "LOC-TX-" + UUID.randomUUID(),
                    "BIEN-TX-" + UUID.randomUUID(),
                    bailleurId,
                    LocalDate.of(2026, 10, 1),
                    LocalDate.of(2026, 10, 31),
                    new BigDecimal("3000.00"),
                    new BigDecimal("200.00"),
                    new BigDecimal("3000.00"),
                    new BigDecimal("200.00"),
                    TypeDocument.QUITTANCE_INTEGRALE,
                    Map.of(
                            "versionSnapshot", "1",
                            "referenceBail", "BAIL-TX",
                            "debutBail", "2026-01-01",
                            "bailleurNom", "Bailleur de test",
                            "locataireNom", "Locataire de test",
                            "locatairePrenom", "Test",
                            "bienAdresse", "Adresse de test"
                    )
            );
            document.validerPourSignature();
            return quittances.saveAndFlush(document).getId();
        });

        Quittance document = quittances.findById(quittanceId)
                .orElseThrow();
        Instant date = Instant.now().plusSeconds(30);
        String annee = Integer.toString(
                date.atZone(ZoneId.of("Africa/Casablanca")).getYear()
        );
        source = new EmissionDemoTransactions.Source(
                new DonneesApercu(mapper.convertir(document), List.of()),
                document.getVersion(),
                "DEMO-QT-" + annee + "-000001",
                date
        );
        pdf = "%PDF-1.7\nsynthetic demo test".getBytes(
                java.nio.charset.StandardCharsets.US_ASCII
        );
        preuve = new SignatureLocale.Preuve(
                new byte[256],
                new byte[]{1},
                "a".repeat(64)
        );
    }

    @AfterEach
    void supprimerUniquementLesDonneesDuTest() {
        if (transaction == null || quittanceId == null) {
            return;
        }

        transaction.executeWithoutResult(status -> {
            jdbc.update(
                    "DELETE FROM quittance_audit WHERE quittance_id = ?",
                    quittanceId
            );
            jdbc.update(
                    "DELETE FROM emission_demo WHERE quittance_id = ?",
                    quittanceId
            );
            jdbc.update(
                    "DELETE FROM quittance WHERE id = ?",
                    quittanceId
            );
        });
    }

    @Test
    void deuxEmissionsConcurrentesNeProduisentQuUneEmissionDemo()
            throws Exception {
        CountDownLatch prets = new CountDownLatch(2);
        CountDownLatch depart = new CountDownLatch(1);
        ExecutorService executeur = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> premiere = executeur.submit(
                    () -> tenterEmission(prets, depart)
            );
            Future<Integer> seconde = executeur.submit(
                    () -> tenterEmission(prets, depart)
            );

            assertTrue(
                    prets.await(10, TimeUnit.SECONDS),
                    "Les deux tentatives doivent être prêtes avant le départ."
            );
            depart.countDown();

            List<Integer> resultats = new ArrayList<>(
                    List.of(
                            premiere.get(20, TimeUnit.SECONDS),
                            seconde.get(20, TimeUnit.SECONDS)
                    )
            );
            Collections.sort(resultats);

            assertEquals(List.of(200, 409), resultats);
            assertEquals("EMIS", statutDocument());
            assertEquals(1L, compter("emission_demo"));
            assertEquals(1L, compterAuditsEmission());
            assertEquals(1L, nombreNumeroAttribue());

        } finally {
            depart.countDown();
            executeur.shutdownNow();
            assertTrue(
                    executeur.awaitTermination(15, TimeUnit.SECONDS),
                    "Les tentatives concurrentes doivent être terminées."
            );
        }
    }

    @Test
    void echecAuditApresEcritureAnnuleLePdfEtLaTransition()
            throws Exception {
        try (MockedStatic<SignatureLocale> signature =
                     mockStatic(SignatureLocale.class)) {
            autoriserSignatureDeDemoPourLeTest(signature);

            assertThrows(
                    AccessDeniedException.class,
                    () -> transactions.enregistrer(
                            identiteBailleur(false),
                            quittanceId,
                            source,
                            pdf,
                            preuve
                    )
            );
        }

        assertEquals("EN_ATTENTE_SIGNATURE", statutDocument());
        assertNull(numeroDocument());
        assertEquals(0L, compter("emission_demo"));
        assertEquals(0L, compterAuditsEmission());
    }

    private int tenterEmission(
            CountDownLatch prets,
            CountDownLatch depart
    ) throws Exception {
        prets.countDown();
        if (!depart.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException(
                    "Le signal de départ n'a pas été reçu."
            );
        }

        try (MockedStatic<SignatureLocale> signature =
                     mockStatic(SignatureLocale.class)) {
            autoriserSignatureDeDemoPourLeTest(signature);
            transactions.enregistrer(
                    identiteBailleur(true),
                    quittanceId,
                    source,
                    pdf,
                    preuve
            );
            return 200;
        } catch (ResponseStatusException exception) {
            return exception.getStatusCode().value();
        }
    }

    private void autoriserSignatureDeDemoPourLeTest(
            MockedStatic<SignatureLocale> signature
    ) throws Exception {
        signature.when(() -> SignatureLocale.verifier(
                any(byte[].class),
                any(SignatureLocale.Preuve.class),
                eq(source.date())
        )).thenReturn(true);
    }

    private JwtAuthenticationToken identiteBailleur(boolean avecIssuer) {
        Jwt.Builder jeton = Jwt.withTokenValue("jeton-test")
                .header("alg", "RS256")
                .subject("acteur-" + bailleurId)
                .claim("bailleur_id", bailleurId);
        if (avecIssuer) {
            jeton.issuer("urn:gestionlocative:dev");
        }
        return new JwtAuthenticationToken(
                jeton.build(),
                List.of(new SimpleGrantedAuthority("ROLE_BAILLEUR"))
        );
    }

    private String statutDocument() {
        return quittances.findById(quittanceId)
                .orElseThrow()
                .getStatut()
                .name();
    }

    private String numeroDocument() {
        return quittances.findById(quittanceId)
                .orElseThrow()
                .getNumero();
    }

    private long compter(String table) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table
                        + " WHERE quittance_id = ?",
                Long.class,
                quittanceId
        );
    }

    private long compterAuditsEmission() {
        return jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM quittance_audit
                WHERE quittance_id = ?
                  AND operation = 'EMISSION_DEMO'
                """,
                Long.class,
                quittanceId
        );
    }

    private long nombreNumeroAttribue() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM quittance "
                        + "WHERE id = ? AND numero IS NOT NULL",
                Long.class,
                quittanceId
        );
    }
}
