package com.gestionlocative.quittances;

import org.springframework.dao.InvalidDataAccessApiUsageException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.StatutQuittance;
import com.gestionlocative.quittances.entity.StatutSignature;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.repository.ValidationTraceRepository;
import com.gestionlocative.quittances.service.ValidationQuittanceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@SpringBootTest
@Execution(ExecutionMode.SAME_THREAD)
class ValidationQuittanceTransactionTest {

    @Autowired
    private ValidationQuittanceService service;

    @Autowired
    private QuittanceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private ValidationTraceRepository traceRepository;

    private ValidationTraceRepository traceSpy;
    private TransactionTemplate transactions;

    private UUID documentId;
    private String bailleurId;
    private long versionInitiale;

    @BeforeEach
    void preparerDocumentValideEnBase() {
        transactions = new TransactionTemplate(transactionManager);

        traceSpy = AopTestUtils.getUltimateTargetObject(traceRepository);

        bailleurId = "TEST-TX-BAILLEUR-" + UUID.randomUUID();

        documentId = transactions.execute(statut -> {
            Quittance document = Quittance.creerBrouillon(
                    "TEST-TX-BAIL-" + UUID.randomUUID(),
                    "TEST-TX-LOC-" + UUID.randomUUID(),
                    "TEST-TX-BIEN-" + UUID.randomUUID(),
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
                            "bailleurNom", "Bailleur fictif",
                            "locataireNom", "Locataire fictif",
                            "locatairePrenom", "Test",
                            "bienAdresse", "Adresse fictive"
                    )
            );

            Quittance enregistre = repository.saveAndFlush(document);
            versionInitiale = enregistre.getVersion();

            return enregistre.getId();
        });
    }

    @AfterEach
    void nettoyerUniquementLeDocumentDuTest() {
        if (traceSpy != null) {
            reset(traceSpy);
        }

        if (documentId == null || transactions == null) {
            return;
        }

        transactions.executeWithoutResult(statut -> {
            jdbcTemplate.update(
                    "DELETE FROM quittance_audit WHERE quittance_id = ?",
                    documentId
            );

            jdbcTemplate.update(
                    "DELETE FROM quittance_validation WHERE quittance_id = ?",
                    documentId
            );

            jdbcTemplate.update(
                    "DELETE FROM paiement_affectation WHERE quittance_id = ?",
                    documentId
            );

            jdbcTemplate.update(
                    "DELETE FROM quittance WHERE id = ?",
                    documentId
            );
        });
    }

    @Test
    void deuxValidationsConcurrentesNeDoiventProduireQuUnSucces()
            throws Exception {

        CountDownLatch prets = new CountDownLatch(2);
        CountDownLatch depart = new CountDownLatch(1);

        ExecutorService executeur = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> premiere = executeur.submit(
                    () -> tenterValidation(prets, depart)
            );

            Future<Integer> seconde = executeur.submit(
                    () -> tenterValidation(prets, depart)
            );

            assertTrue(
                    prets.await(10, TimeUnit.SECONDS),
                    "Les deux tâches doivent être prêtes avant le départ."
            );

            depart.countDown();

            List<Integer> resultats = new ArrayList<>();

            resultats.add(premiere.get(20, TimeUnit.SECONDS));
            resultats.add(seconde.get(20, TimeUnit.SECONDS));

            Collections.sort(resultats);

            assertEquals(
                    List.of(200, 409),
                    resultats,
                    "Une seule validation doit réussir."
            );

            Quittance document = repository.findById(documentId)
                    .orElseThrow();

            assertEquals(
                    StatutQuittance.EN_ATTENTE_SIGNATURE,
                    document.getStatut()
            );

            assertEquals(
                    StatutSignature.EN_ATTENTE,
                    document.getStatutSignature()
            );

            assertEquals(
                    versionInitiale + 1,
                    document.getVersion().longValue()
            );

            assertEquals(1L, compterValidations());
            assertEquals(1L, compterAudits());

            assertNull(document.getNumero());
            assertNull(document.getEmisLe());

        } finally {
            depart.countDown();
            executeur.shutdownNow();

            assertTrue(
                    executeur.awaitTermination(15, TimeUnit.SECONDS),
                    "Les tâches de validation doivent être terminées."
            );
        }
    }

    @Test
    void echecApresEcritureAuditDoitAnnulerTouteLaValidation() {
        doAnswer(invocation -> {
            invocation.callRealMethod();

            assertEquals(
                    1L,
                    compterValidations(),
                    "La validation doit avoir été écrite dans la transaction."
            );

            assertEquals(
                    1L,
                    compterAudits(),
                    "L'audit doit avoir été écrit dans la transaction."
            );

            throw new IllegalStateException(
                    "PANNE_SIMULEE_APRES_ECRITURES"
            );
        }).when(traceSpy).enregistrer(
                eq(documentId),
                anyString(),
                anyString(),
                any(Instant.class),
                anyLong(),
                anyString()
        );

        InvalidDataAccessApiUsageException exception = assertThrows(
                InvalidDataAccessApiUsageException.class,
                () -> service.valider(
                        identiteBailleur(),
                        documentId,
                        true
                )
        );

        IllegalStateException cause = assertInstanceOf(
                IllegalStateException.class,
                exception.getMostSpecificCause()
        );

        assertEquals(
                "PANNE_SIMULEE_APRES_ECRITURES",
                cause.getMessage()
        );

        Quittance document = repository.findById(documentId)
                .orElseThrow();

        assertEquals(
                StatutQuittance.BROUILLON,
                document.getStatut()
        );

        assertEquals(
                StatutSignature.NON_SIGNE,
                document.getStatutSignature()
        );

        assertEquals(
                versionInitiale,
                document.getVersion().longValue()
        );

        assertEquals(0L, compterValidations());
        assertEquals(0L, compterAudits());

        assertNull(document.getNumero());
        assertNull(document.getEmisLe());
    }

    private int tenterValidation(
            CountDownLatch prets,
            CountDownLatch depart
    ) throws InterruptedException {
        prets.countDown();

        if (!depart.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException(
                    "Le signal de départ n'a pas été reçu."
            );
        }

        try {
            service.valider(
                    identiteBailleur(),
                    documentId,
                    true
            );

            return 200;

        } catch (ResponseStatusException exception) {
            return exception.getStatusCode().value();
        }
    }

    private long compterValidations() {
        Long nombre = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quittance_validation "
                        + "WHERE quittance_id = ?",
                Long.class,
                documentId
        );

        return nombre.longValue();
    }

    private long compterAudits() {
        Long nombre = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quittance_audit "
                        + "WHERE quittance_id = ? "
                        + "AND operation = 'VALIDATION'",
                Long.class,
                documentId
        );

        return nombre.longValue();
    }

    private JwtAuthenticationToken identiteBailleur() {
        Jwt jwt = Jwt.withTokenValue("jeton-test")
                .header("alg", "RS256")
                .subject("acteur-test-transaction")
                .issuer("urn:gestionlocative:dev")
                .claim("bailleur_id", bailleurId)
                .build();

        return new JwtAuthenticationToken(
                jwt,
                List.of(
                        new SimpleGrantedAuthority("ROLE_BAILLEUR")
                )
        );
    }
}