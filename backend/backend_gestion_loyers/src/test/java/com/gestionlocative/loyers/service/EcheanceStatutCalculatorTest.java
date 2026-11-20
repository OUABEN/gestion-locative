package com.gestionlocative.loyers.service;

import static com.gestionlocative.loyers.TestFixtures.echeance;
import static com.gestionlocative.loyers.TestFixtures.props;
import static org.assertj.core.api.Assertions.assertThat;

import com.gestionlocative.loyers.config.LoyersProperties;
import com.gestionlocative.loyers.model.Echeance;
import com.gestionlocative.loyers.model.StatutEcheance;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Échéance de référence : novembre 2026, 1000.00 DH, date limite = 5 novembre 2026. */
class EcheanceStatutCalculatorTest {

    private final EcheanceStatutCalculator calculateur = new EcheanceStatutCalculator(props());
    private final Echeance e = echeance("1000.00", 11, 2026, StatutEcheance.A_PAYER);

    @Test
    @DisplayName("UT-LOY-01 Total payé égal au montant -> PAYEE")
    void ut_loy_01_total_egal_montant_donne_payee() {
        assertThat(calculateur.calculer(e, new BigDecimal("1000.00"), LocalDate.of(2026, 11, 3)))
                .isEqualTo(StatutEcheance.PAYEE);
    }

    @Test
    @DisplayName("UT-LOY-02 Avant la date limite -> A_PAYER")
    void ut_loy_02_avant_date_limite_donne_a_payer() {
        assertThat(calculateur.calculer(e, BigDecimal.ZERO, LocalDate.of(2026, 11, 4)))
                .isEqualTo(StatutEcheance.A_PAYER);
    }

    @Test
    @DisplayName("UT-LOY-03 Le jour même de la date limite -> A_PAYER (cas limite)")
    void ut_loy_03_jour_de_la_date_limite_donne_a_payer() {
        assertThat(calculateur.calculer(e, BigDecimal.ZERO, LocalDate.of(2026, 11, 5)))
                .isEqualTo(StatutEcheance.A_PAYER);
    }

    @Test
    @DisplayName("UT-LOY-04 Lendemain de la date limite -> EN_RETARD (cas limite)")
    void ut_loy_04_lendemain_date_limite_donne_en_retard() {
        assertThat(calculateur.calculer(e, BigDecimal.ZERO, LocalDate.of(2026, 11, 6)))
                .isEqualTo(StatutEcheance.EN_RETARD);
    }

    @Test
    @DisplayName("UT-LOY-05 30 jours après la date limite -> EN_RETARD (cas limite)")
    void ut_loy_05_trente_jours_apres_donne_en_retard() {
        assertThat(calculateur.calculer(e, BigDecimal.ZERO, LocalDate.of(2026, 12, 5)))
                .isEqualTo(StatutEcheance.EN_RETARD);
    }

    @Test
    @DisplayName("UT-LOY-06 31 jours après la date limite -> IMPAYEE (cas limite)")
    void ut_loy_06_trente_et_un_jours_apres_donne_impayee() {
        assertThat(calculateur.calculer(e, BigDecimal.ZERO, LocalDate.of(2026, 12, 6)))
                .isEqualTo(StatutEcheance.IMPAYEE);
    }

    @Test
    @DisplayName("UT-LOY-07 Paiement partiel après la date limite -> EN_RETARD")
    void ut_loy_07_paiement_partiel_en_retard_reste_en_retard() {
        assertThat(calculateur.calculer(e, new BigDecimal("500.00"), LocalDate.of(2026, 11, 20)))
                .isEqualTo(StatutEcheance.EN_RETARD);
    }

    @Test
    @DisplayName("UT-LOY-08 Jour d'échéance 31 en février -> dernier jour du mois")
    void ut_loy_08_jour_31_en_fevrier_ramene_a_fin_de_mois() {
        EcheanceStatutCalculator c = new EcheanceStatutCalculator(
                new LoyersProperties(31, 30, props().services()));
        assertThat(c.dateLimite(2, 2027)).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(c.dateLimite(2, 2028)).isEqualTo(LocalDate.of(2028, 2, 29));
    }
}
