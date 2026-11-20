package com.gestionlocative.quittances;

import com.gestionlocative.quittances.dto.SituationPeriode;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.StatutQuittance;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.service.ConstructionBrouillonService;
import com.gestionlocative.quittances.service.EnregistrementRemplacementService;
import com.gestionlocative.quittances.service.RemplacementQuittanceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@Execution(ExecutionMode.SAME_THREAD)
class RemplacementQuittanceServiceTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 10, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 10, 31);

    @Autowired
    private RemplacementQuittanceService service;

    @Autowired
    private QuittanceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private ConstructionBrouillonService constructionService;

    @MockitoSpyBean
    private EnregistrementRemplacementService enregistrementService;

    private EnregistrementRemplacementService enregistrementSpy;
    private TransactionTemplate transactions;

    private String bailId;
    private String bailleurId;
    private String locataireId;
    private String bienId;

    @BeforeEach
    void preparer() {
        String suffixe = UUID.randomUUID().toString();

        bailId = "TEST-REMPL-BAIL-" + suffixe;
        bailleurId = "TEST-REMPL-BAILLEUR-" + suffixe;
        locataireId = "TEST-REMPL-LOC-" + suffixe;
        bienId = "TEST-REMPL-BIEN-" + suffixe;

        transactions = new TransactionTemplate(transactionManager);

        enregistrementSpy =
                AopTestUtils.getUltimateTargetObject(enregistrementService);
    }

    @AfterEach
    void nettoyerUniquementLesDonneesDuTest() {
        if (enregistrementSpy != null) {
            reset(enregistrementSpy);
        }

        if (transactions == null || bailId == null) {
            return;
        }

        transactions.executeWithoutResult(statut -> {
            jdbcTemplate.update("""
                    DELETE FROM quittance_audit
                    WHERE quittance_id IN (
                        SELECT id FROM quittance WHERE bail_id = ?
                    )
                    """, bailId);

            jdbcTemplate.update("""
                    DELETE FROM quittance_validation
                    WHERE quittance_id IN (
                        SELECT id FROM quittance WHERE bail_id = ?
                    )
                    """, bailId);

            jdbcTemplate.update("""
                    DELETE FROM paiement_affectation
                    WHERE quittance_id IN (
                        SELECT id FROM quittance WHERE bail_id = ?
                    )
                    """, bailId);

            jdbcTemplate.update("""
                    DELETE FROM quittance
                    WHERE bail_id = ?
                      AND document_precedent_id IS NOT NULL
                    """, bailId);

            jdbcTemplate.update(
                    "DELETE FROM quittance WHERE bail_id = ?",
                    bailId
            );
        });
    }

    @Test
    void doitCreerUnBrouillonLieSansModifierLesAnciennesDonnees() {
        UUID ancienId = creerAncien(true);
        simulerConstruction();

        var resultat = service.preparer(
                identite("BAILLEUR", bailleurId),
                ancienId,
                "Correction documentaire.",
                true
        );

        assertNotEquals(ancienId, resultat.id());
        assertEquals(ancienId, resultat.documentPrecedentId());
        assertEquals("BROUILLON", resultat.statut());
        assertEquals("NON_SIGNE", resultat.statutSignature());

        Quittance ancien = repository.findById(ancienId).orElseThrow();
        Quittance nouveau = repository.findById(resultat.id()).orElseThrow();

        assertEquals(StatutQuittance.ANNULE, ancien.getStatut());
        assertEquals(new BigDecimal("1000.00"), ancien.getTotalPaye());
        assertEquals(
                "Locataire initial",
                ancien.getDonneesFigees().get("locataireNom")
        );

        assertEquals(StatutQuittance.BROUILLON, nouveau.getStatut());
        assertEquals(new BigDecimal("1500.00"), nouveau.getTotalPaye());
        assertEquals(
                "Locataire actualisé",
                nouveau.getDonneesFigees().get("locataireNom")
        );

        UUID lienEnBase = jdbcTemplate.queryForObject(
                "SELECT document_precedent_id FROM quittance WHERE id = ?",
                UUID.class,
                resultat.id()
        );

        assertEquals(ancienId, lienEnBase);
        assertEquals(2L, compterDocuments());
        assertEquals(1L, compterAffectations());
        assertEquals(2L, compterAudits());
    }

    @Test
    void doitRefuserDeuxSuccesseursActifsMemePourUnRecuPartiel() {
        UUID ancienId = creerAncien(true);
        simulerConstruction();

        var utilisateur = identite("BAILLEUR", bailleurId);

        service.preparer(
                utilisateur,
                ancienId,
                "Premier remplacement.",
                true
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> service.preparer(
                        utilisateur,
                        ancienId,
                        "Deuxième remplacement.",
                        true
                )
        );

        assertEquals(2L, compterDocuments());
        assertEquals(1L, compterAffectations());
        assertEquals(2L, compterAudits());
    }

    @Test
    void doitRefuserUnAutreBailleurAvantTouteConstruction() {
        UUID ancienId = creerAncien(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.preparer(
                        identite("BAILLEUR", "AUTRE-BAILLEUR"),
                        ancienId,
                        "Correction.",
                        true
                )
        );

        assertEquals(404, exception.getStatusCode().value());
        verifyNoInteractions(constructionService);
        assertEquals(1L, compterDocuments());
    }

    @Test
    void doitRefuserUnLocataireAvantTouteConstruction() {
        UUID ancienId = creerAncien(true);

        assertThrows(
                AccessDeniedException.class,
                () -> service.preparer(
                        identite("LOCATAIRE", locataireId),
                        ancienId,
                        "Correction.",
                        true
                )
        );

        verifyNoInteractions(constructionService);
        assertEquals(1L, compterDocuments());
    }

    @Test
    void doitRefuserUnDocumentNonAnnule() {
        UUID ancienId = creerAncien(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.preparer(
                        identite("BAILLEUR", bailleurId),
                        ancienId,
                        "Correction.",
                        true
                )
        );

        assertEquals(409, exception.getStatusCode().value());
        verifyNoInteractions(constructionService);
        assertEquals(1L, compterDocuments());
    }

    @Test
    void panneApresToutesLesEcrituresDoitAnnulerLeRemplacement() {
        UUID ancienId = creerAncien(true);
        simulerConstruction();

        doAnswer(invocation -> {
            invocation.callRealMethod();

            assertEquals(2L, compterDocuments());
            assertEquals(1L, compterAffectations());
            assertEquals(2L, compterAudits());

            throw new PanneSimulee();
        }).when(enregistrementSpy).enregistrer(
                eq(ancienId),
                eq(bailleurId),
                anyString(),
                anyString(),
                anyString(),
                any(ConstructionBrouillonService.Resultat.class)
        );

        assertThrows(
                PanneSimulee.class,
                () -> service.preparer(
                        identite("BAILLEUR", bailleurId),
                        ancienId,
                        "Correction avec panne simulée.",
                        true
                )
        );

        assertEquals(1L, compterDocuments());
        assertEquals(0L, compterAffectations());
        assertEquals(0L, compterAudits());

        Quittance ancien = repository.findById(ancienId).orElseThrow();

        assertEquals(StatutQuittance.ANNULE, ancien.getStatut());
        assertEquals(new BigDecimal("1000.00"), ancien.getTotalPaye());
    }

    private UUID creerAncien(boolean annule) {
        return transactions.execute(statut -> {
            Quittance document = brouillon(
                    "1000.00",
                    "Locataire initial"
            );

            if (annule) {
                document.annuler("Annulation du document de test.");
            }

            return repository.saveAndFlush(document).getId();
        });
    }

    private void simulerConstruction() {
        when(constructionService.construire(
                bailleurId,
                bailId,
                DEBUT,
                FIN
        )).thenAnswer(invocation -> {
            assertFalse(
                    TransactionSynchronizationManager
                            .isActualTransactionActive(),
                    "La construction doit se dérouler hors transaction."
            );

            Quittance nouveau = brouillon(
                    "1500.00",
                    "Locataire actualisé"
            );

            var affectation = new SituationPeriode.Affectation(
                    "AFF-REMPL-" + UUID.randomUUID(),
                    "PAY-REMPL-" + UUID.randomUUID(),
                    bailId,
                    DEBUT,
                    FIN,
                    SituationPeriode.Nature.LOYER,
                    new BigDecimal("1500.00"),
                    "MAD",
                    SituationPeriode.StatutPaiement.CONFIRME
            );

            return new ConstructionBrouillonService.Resultat(
                    nouveau,
                    List.of(affectation)
            );
        });
    }

    private Quittance brouillon(
            String montantPaye,
            String nomLocataire
    ) {
        return Quittance.creerBrouillon(
                bailId,
                locataireId,
                bienId,
                bailleurId,
                DEBUT,
                FIN,
                new BigDecimal("3000.00"),
                new BigDecimal("200.00"),
                new BigDecimal(montantPaye),
                new BigDecimal("0.00"),
                TypeDocument.RECU_PARTIEL,
                Map.of(
                        "versionSnapshot", "1",
                        "bailleurNom", "Bailleur fictif",
                        "locataireNom", nomLocataire,
                        "locatairePrenom", "Test",
                        "bienAdresse", "Adresse fictive"
                )
        );
    }

    private long compterDocuments() {
        Long nombre = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quittance WHERE bail_id = ?",
                Long.class,
                bailId
        );

        return nombre.longValue();
    }

    private long compterAffectations() {
        Long nombre = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM paiement_affectation a
                JOIN quittance q ON q.id = a.quittance_id
                WHERE q.bail_id = ?
                """,
                Long.class,
                bailId
        );

        return nombre.longValue();
    }

    private long compterAudits() {
        Long nombre = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM quittance_audit a
                JOIN quittance q ON q.id = a.quittance_id
                WHERE q.bail_id = ?
                """,
                Long.class,
                bailId
        );

        return nombre.longValue();
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
                .subject("acteur-test-remplacement")
                .issuer("urn:gestionlocative:dev")
                .claim(claim, identifiant)
                .build();

        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    private static final class PanneSimulee extends RuntimeException {

        private PanneSimulee() {
            super("PANNE_SIMULEE_APRES_REMPLACEMENT");
        }
    }
}