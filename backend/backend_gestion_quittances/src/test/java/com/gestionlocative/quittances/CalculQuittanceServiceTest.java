package com.gestionlocative.quittances;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.gestionlocative.quittances.dto.ResultatCalcul;
import com.gestionlocative.quittances.dto.SituationPeriode;
import com.gestionlocative.quittances.dto.SituationPeriode.Affectation;
import com.gestionlocative.quittances.dto.SituationPeriode.Nature;
import com.gestionlocative.quittances.dto.SituationPeriode.StatutPaiement;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.exception.RegleMetierException;
import com.gestionlocative.quittances.service.CalculQuittanceService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculQuittanceServiceTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 10, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 10, 31);

    private final CalculQuittanceService service = new CalculQuittanceService();

    @Test
    void doitCalculerUneQuittanceIntegraleAvecPlusieursVersements() {
        ResultatCalcul resultat = service.calculer(situation(List.of(
                affectation("A1", "P1", Nature.LOYER, "1500.00", StatutPaiement.CONFIRME),
                affectation("A2", "P2", Nature.LOYER, "1500.00", StatutPaiement.CONFIRME),
                affectation("A3", "P2", Nature.CHARGES, "200.00", StatutPaiement.CONFIRME)
        )));

        assertEquals(TypeDocument.QUITTANCE_INTEGRALE, resultat.typeDocument());
        assertEquals(new BigDecimal("3200.00"), resultat.totalPaye());
        assertEquals(3, resultat.affectationsRetenues().size());
    }

    @Test
    void doitCalculerUnRecuPartielAvecPrecisionDecimale() {
        ResultatCalcul resultat = service.calculer(situation(List.of(
                affectation("A1", "P1", Nature.LOYER, "1000.10", StatutPaiement.CONFIRME),
                affectation("A2", "P2", Nature.LOYER, "500.15", StatutPaiement.CONFIRME)
        )));

        assertEquals(TypeDocument.RECU_PARTIEL, resultat.typeDocument());
        assertEquals(new BigDecimal("1500.25"), resultat.totalPaye());
    }

    @Test
    void doitIgnorerUneRepetitionIdentique() {
        Affectation affectation = affectation(
                "A1", "P1", Nature.LOYER, "1000.00", StatutPaiement.CONFIRME
        );

        ResultatCalcul resultat = service.calculer(
                situation(List.of(affectation, affectation))
        );

        assertEquals(new BigDecimal("1000.00"), resultat.totalPaye());
        assertEquals(1, resultat.affectationsRetenues().size());
    }

    @Test
    void doitRefuserUnIdentifiantAffectationContradictoire() {
        verifierErreur("AFFECTATION_CONFLICTUELLE", situation(List.of(
                affectation("A1", "P1", Nature.LOYER, "1000.00", StatutPaiement.CONFIRME),
                affectation("A1", "P1", Nature.LOYER, "1200.00", StatutPaiement.CONFIRME)
        )));
    }

    @Test
    void doitExclurePaiementsAnnulesRemboursesEtEnAttente() {
        ResultatCalcul resultat = service.calculer(situation(List.of(
                affectation("A1", "P1", Nature.LOYER, "100.00", StatutPaiement.CONFIRME),
                affectation("A2", "P2", Nature.LOYER, "900.00", StatutPaiement.ANNULE),
                affectation("A3", "P3", Nature.LOYER, "800.00", StatutPaiement.REMBOURSE),
                affectation("A4", "P4", Nature.LOYER, "700.00", StatutPaiement.EN_ATTENTE)
        )));

        assertEquals(new BigDecimal("100.00"), resultat.totalPaye());
    }

    @Test
    void doitRetenirSeulementLaPeriodeDemandee() {
        Affectation novembre = new Affectation(
                "A2", "P1", "BAIL-1",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                Nature.LOYER,
                new BigDecimal("3000.00"),
                "MAD",
                StatutPaiement.CONFIRME
        );

        ResultatCalcul resultat = service.calculer(situation(List.of(
                affectation("A1", "P1", Nature.LOYER, "3000.00", StatutPaiement.CONFIRME),
                novembre
        )));

        assertEquals(new BigDecimal("3000.00"), resultat.totalPaye());
        assertEquals(TypeDocument.RECU_PARTIEL, resultat.typeDocument());
        assertEquals(1, resultat.affectationsRetenues().size());
    }

    @Test
    void doitExclureDepotEtExcedent() {
        ResultatCalcul resultat = service.calculer(situation(List.of(
                affectation("A1", "P1", Nature.LOYER, "3000.00", StatutPaiement.CONFIRME),
                affectation("A2", "P1", Nature.CHARGES, "200.00", StatutPaiement.CONFIRME),
                affectation("A3", "P2", Nature.DEPOT_GARANTIE, "3000.00", StatutPaiement.CONFIRME),
                affectation("A4", "P1", Nature.EXCEDENT, "500.00", StatutPaiement.CONFIRME)
        )));

        assertEquals(new BigDecimal("3200.00"), resultat.totalPaye());
        assertEquals(2, resultat.affectationsRetenues().size());
    }

    @Test
    void doitRefuserUneSurAffectation() {
        verifierErreur("AFFECTATION_SUPERIEURE_AU_DU", situation(List.of(
                affectation("A1", "P1", Nature.LOYER, "3200.00", StatutPaiement.CONFIRME)
        )));
    }

    @Test
    void doitRefuserUnMontantNegatif() {
        verifierErreur("MONTANT_INVALIDE", situation(List.of(
                affectation("A1", "P1", Nature.LOYER, "-10.00", StatutPaiement.CONFIRME)
        )));
    }

    @Test
    void doitRefuserUneFractionInferieureAuCentime() {
        verifierErreur("PRECISION_INVALIDE", situation(List.of(
                affectation("A1", "P1", Nature.LOYER, "100.125", StatutPaiement.CONFIRME)
        )));
    }

    @Test
    void doitRefuserUnePeriodeHorsBail() {
        SituationPeriode situation = new SituationPeriode(
                "BAIL-1",
                LocalDate.of(2026, 10, 15),
                null,
                DEBUT,
                FIN,
                new BigDecimal("3000.00"),
                new BigDecimal("200.00"),
                "MAD",
                List.of()
        );

        verifierErreur("PERIODE_HORS_BAIL", situation);
    }

    @Test
    void doitAccepterUnePeriodeHistoriqueDUnBailTermine() {
        LocalDate debutHistorique = LocalDate.of(2020, 1, 1);
        LocalDate finHistorique = LocalDate.of(2020, 1, 31);

        Affectation affectation = new Affectation(
                "A1", "P1", "BAIL-1",
                debutHistorique,
                finHistorique,
                Nature.LOYER,
                new BigDecimal("1000.00"),
                "MAD",
                StatutPaiement.CONFIRME
        );

        SituationPeriode situation = new SituationPeriode(
                "BAIL-1",
                debutHistorique,
                LocalDate.of(2020, 12, 31),
                debutHistorique,
                finHistorique,
                new BigDecimal("1000.00"),
                new BigDecimal("0.00"),
                "MAD",
                List.of(affectation)
        );

        assertEquals(
                TypeDocument.QUITTANCE_INTEGRALE,
                service.calculer(situation).typeDocument()
        );
    }

    @Test
    void doitRefuserUnePeriodeSansPaiementConfirme() {
        verifierErreur("AUCUN_PAIEMENT_CONFIRME", situation(List.of()));
    }

    @Test
    void doitRefuserUneAffectationCouvrantPlusieursMoisSansRepartition() {
        Affectation affectation = new Affectation(
                "A1", "P1", "BAIL-1",
                DEBUT,
                LocalDate.of(2026, 11, 30),
                Nature.LOYER,
                new BigDecimal("3000.00"),
                "MAD",
                StatutPaiement.CONFIRME
        );

        verifierErreur(
                "AFFECTATION_PERIODE_AMBIGUE",
                situation(List.of(affectation))
        );
    }

    private SituationPeriode situation(List<Affectation> affectations) {
        return new SituationPeriode(
                "BAIL-1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                DEBUT,
                FIN,
                new BigDecimal("3000.00"),
                new BigDecimal("200.00"),
                "MAD",
                affectations
        );
    }

    private Affectation affectation(
            String id,
            String paiementId,
            Nature nature,
            String montant,
            StatutPaiement statut) {

        return new Affectation(
                id,
                paiementId,
                "BAIL-1",
                DEBUT,
                FIN,
                nature,
                new BigDecimal(montant),
                "MAD",
                statut
        );
    }

    private void verifierErreur(String code, SituationPeriode situation) {
        RegleMetierException exception = assertThrows(
                RegleMetierException.class,
                () -> service.calculer(situation)
        );

        assertEquals(code, exception.getCode());
    }
}