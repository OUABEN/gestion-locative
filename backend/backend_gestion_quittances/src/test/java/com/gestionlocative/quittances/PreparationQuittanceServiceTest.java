package com.gestionlocative.quittances;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gestionlocative.quittances.client.BienBailClient;
import com.gestionlocative.quittances.client.LocataireClient;
import com.gestionlocative.quittances.client.PaiementClient;
import com.gestionlocative.quittances.dto.ReferentielsExternes.BailPeriode;
import com.gestionlocative.quittances.dto.ReferentielsExternes.Bien;
import com.gestionlocative.quittances.dto.ReferentielsExternes.Locataire;
import com.gestionlocative.quittances.dto.SituationPeriode.Affectation;
import com.gestionlocative.quittances.dto.SituationPeriode.Nature;
import com.gestionlocative.quittances.dto.SituationPeriode.StatutPaiement;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.StatutQuittance;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.exception.RegleMetierException;
import com.gestionlocative.quittances.exception.ServiceExterneException;
import com.gestionlocative.quittances.repository.PaiementAffectationRepository;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.service.EnregistrementBrouillonService;
import com.gestionlocative.quittances.service.PreparationQuittanceService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
class PreparationQuittanceServiceTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 10, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 10, 31);

    @Autowired
    private PreparationQuittanceService preparation;

    @Autowired
    private EnregistrementBrouillonService enregistrement;

    @Autowired
    private QuittanceRepository quittances;

    @Autowired
    private PaiementAffectationRepository affectationsRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private BienBailClient biensBaux;

    @MockitoBean
    private LocataireClient locataires;

    @MockitoBean
    private PaiementClient paiements;

    private String bailId;

    @BeforeEach
    void preparerReponses() {
        bailId = "TEST-PREPARATION-" + UUID.randomUUID();

        BailPeriode bail = new BailPeriode(
                "1",
                bailId,
                "LOC-TEST",
                "BIEN-TEST",
                "BAILLEUR-TEST",
                "Bailleur fictif",
                LocalDate.of(2026, 1, 1),
                null,
                DEBUT,
                FIN,
                new BigDecimal("3000.00"),
                new BigDecimal("200.00"),
                "MAD"
        );

        when(biensBaux.consulterBail(bailId, DEBUT, FIN)).thenAnswer(invocation -> {
            verifierAbsenceTransaction();
            return bail;
        });

        when(biensBaux.consulterBien("BIEN-TEST")).thenAnswer(invocation -> {
            verifierAbsenceTransaction();
            return new Bien(
                    "1", "BIEN-TEST", "Adresse fictive à Agadir", "BAILLEUR-TEST"
            );
        });

        when(locataires.consulter("LOC-TEST")).thenAnswer(invocation -> {
            verifierAbsenceTransaction();
            return new Locataire(
                    "1", "LOC-TEST", "Locataire fictif", "Test", null, false
            );
        });

        when(paiements.consulterAffectations(bailId, DEBUT, FIN))
                .thenAnswer(invocation -> {
                    verifierAbsenceTransaction();
                    return List.of(
                            affectation("AFF-1", Nature.LOYER, "3000.00"),
                            affectation("AFF-2", Nature.CHARGES, "200.00")
                    );
                });
    }

    @AfterEach
    void nettoyerSeulementLesDocumentsDuTest() {
        if (bailId == null) {
            return;
        }

        jdbc.update(
                """
                DELETE FROM paiement_affectation
                WHERE quittance_id IN (
                    SELECT id FROM quittance WHERE bail_id = ?
                )
                """,
                bailId
        );

        jdbc.update(
                "DELETE FROM quittance WHERE bail_id = ?",
                bailId
        );
    }

    @Test
    void doitEnregistrerLeBrouillonEtSesAffectations() {
        UUID id = preparation.preparer(
                "BAILLEUR-TEST", bailId, DEBUT, FIN
        );

        Quittance document = quittances.findById(id).orElseThrow();

        assertEquals(StatutQuittance.BROUILLON, document.getStatut());
        assertEquals(TypeDocument.QUITTANCE_INTEGRALE, document.getTypeDocument());
        assertEquals(new BigDecimal("3200.00"), document.getTotalPaye());
        assertNull(document.getNumero());
        assertNull(document.getPdfReference());

        assertEquals(
                "Adresse fictive à Agadir",
                document.getDonneesFigees().get("bienAdresse")
        );

        var copies = affectationsRepository
                .findByQuittance_IdOrderByAffectationExterneIdAsc(id);

        assertEquals(2, copies.size());
        assertEquals("AFF-1", copies.getFirst().getAffectationExterneId());
    }

    @Test
    void doitEnregistrerUnRecuPartiel() {
        when(paiements.consulterAffectations(bailId, DEBUT, FIN))
                .thenReturn(List.of(
                        affectation("AFF-PARTIELLE", Nature.LOYER, "1500.00")
                ));

        UUID id = preparation.preparer(
                "BAILLEUR-TEST", bailId, DEBUT, FIN
        );

        Quittance document = quittances.findById(id).orElseThrow();

        assertEquals(TypeDocument.RECU_PARTIEL, document.getTypeDocument());
        assertEquals(new BigDecimal("1500.00"), document.getTotalPaye());
    }

    @Test
    void doitRefuserUnAutreBailleurAvantLesAutresLectures() {
        String subBailleur = UUID.randomUUID().toString();
        RegleMetierException erreur = assertThrows(
                RegleMetierException.class,
                () -> preparation.preparer(
                        subBailleur, bailId, DEBUT, FIN
                )
        );

        assertEquals("ACCES_INTERDIT", erreur.getCode());
        verifyNoInteractions(locataires, paiements);
        assertEquals(0, compterDocuments());
    }

    @Test
    void doitRefuserUnBienSansPreuveDePropriete() {
        when(biensBaux.consulterBien("BIEN-TEST")).thenReturn(
                new Bien("1", "BIEN-TEST", "Adresse fictive à Agadir", null)
        );

        RegleMetierException erreur = assertThrows(
                RegleMetierException.class,
                () -> preparation.preparer(
                        "BAILLEUR-TEST", bailId, DEBUT, FIN
                )
        );

        assertEquals("BIEN_BAIL_INCOHERENT", erreur.getCode());
        verifyNoInteractions(locataires, paiements);
        assertEquals(0, compterDocuments());
    }

    @Test
    void neDoitRienEnregistrerSiPaiementsEstIndisponible() {
        when(paiements.consulterAffectations(bailId, DEBUT, FIN))
                .thenThrow(new ServiceExterneException(
                        "PAIEMENTS",
                        "SERVICE_INJOIGNABLE",
                        "Indisponibilité simulée.",
                        null
                ));

        assertThrows(
                ServiceExterneException.class,
                () -> preparation.preparer(
                        "BAILLEUR-TEST", bailId, DEBUT, FIN
                )
        );

        assertEquals(0, compterDocuments());
    }

    @Test
    void doitRefuserUnDeuxiemeBrouillonIntegralActif() {
        preparation.preparer("BAILLEUR-TEST", bailId, DEBUT, FIN);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> preparation.preparer(
                        "BAILLEUR-TEST", bailId, DEBUT, FIN
                )
        );

        assertEquals(1, compterDocuments());
    }

    @Test
    void doitAnnulerTouteLEcritureSiUneAffectationEchoue() {
        Quittance brouillon = Quittance.creerBrouillon(
                bailId,
                "LOC-TEST",
                "BIEN-TEST",
                "BAILLEUR-TEST",
                DEBUT,
                FIN,
                new BigDecimal("3000.00"),
                new BigDecimal("200.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("0.00"),
                TypeDocument.RECU_PARTIEL,
                Map.of("origine", "test transactionnel")
        );

        Affectation source = affectation(
                "AFF-DOUBLON", Nature.LOYER, "500.00"
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> enregistrement.enregistrer(
                        brouillon,
                        List.of(source, source)
                )
        );

        assertEquals(0, compterDocuments());
    }

    private Affectation affectation(
            String id,
            Nature nature,
            String montant) {

        return new Affectation(
                id,
                "PAY-TEST",
                bailId,
                DEBUT,
                FIN,
                nature,
                new BigDecimal(montant),
                "MAD",
                StatutPaiement.CONFIRME
        );
    }

    private int compterDocuments() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM quittance WHERE bail_id = ?",
                Integer.class,
                bailId
        );
    }

    private void verifierAbsenceTransaction() {
        assertFalse(
                TransactionSynchronizationManager.isActualTransactionActive(),
                "Un appel externe ne doit pas être exécuté dans une transaction."
        );
    }


    private org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
            identitePreparation(String role) {

        var jwt = org.springframework.security.oauth2.jwt.Jwt
                .withTokenValue("jeton-test-preparation")
                .header("alg", "HS256")
                .subject("UTILISATEUR-TEST")
                .claim("role", role)
                .claim("locataire_id", "LOC-TEST")
                .claim("bailleur_id", "BAILLEUR-TEST")
                .build();

        return new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                jwt,
                List.of(new org.springframework.security.core.authority
                        .SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Test
    void gestionnairePrepareDocumentPourLeVraiBailleur() {
        UUID id = preparation.preparerPourGestionnaire(
                identitePreparation("GESTIONNAIRE"),
                bailId, DEBUT, FIN);

        Quittance document = quittances.findById(id).orElseThrow();

        assertEquals("BAILLEUR-TEST", document.getBailleurId());
        assertEquals(StatutQuittance.BROUILLON, document.getStatut());
        assertEquals(new BigDecimal("3200.00"), document.getTotalPaye());
        assertEquals(2, affectationsRepository
                .findByQuittance_IdOrderByAffectationExterneIdAsc(id).size());
    }

    @Test
    void locataireNePeutUtiliserPreparationGestionnaire() {
        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> preparation.preparerPourGestionnaire(
                        identitePreparation("LOCATAIRE"),
                        bailId, DEBUT, FIN));

        verifyNoInteractions(biensBaux, locataires, paiements);
        assertEquals(0, compterDocuments());
    }

    @Test
    void bailleurNePeutUtiliserPreparationGestionnaire() {
        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> preparation.preparerPourGestionnaire(
                        identitePreparation("BAILLEUR"),
                        bailId, DEBUT, FIN));

        verifyNoInteractions(biensBaux, locataires, paiements);
        assertEquals(0, compterDocuments());
    }

    @Test
    void refusePreparationSansIdentifiantBailleur() {
        var erreur = assertThrows(
                RegleMetierException.class,
                () -> preparation.preparer(null, bailId, DEBUT, FIN));

        assertEquals("ACTEUR_ABSENT", erreur.getCode());
        verifyNoInteractions(biensBaux, locataires, paiements);
    }

    @Test
    void refusePreparationAvecIdentifiantBailleurVide() {
        var erreur = assertThrows(
                RegleMetierException.class,
                () -> preparation.preparer("   ", bailId, DEBUT, FIN));

        assertEquals("ACTEUR_ABSENT", erreur.getCode());
        verifyNoInteractions(biensBaux, locataires, paiements);
    }

}