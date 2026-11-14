package com.gestionlocative.loyers.service;

import static com.gestionlocative.loyers.TestFixtures.clockAt;
import static com.gestionlocative.loyers.TestFixtures.echeance;
import static com.gestionlocative.loyers.TestFixtures.props;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gestionlocative.loyers.dto.CreatePaiementRequest;
import com.gestionlocative.loyers.dto.PaiementResponse;
import com.gestionlocative.loyers.exception.BusinessRuleException;
import com.gestionlocative.loyers.exception.ResourceNotFoundException;
import com.gestionlocative.loyers.model.Echeance;
import com.gestionlocative.loyers.model.Paiement;
import com.gestionlocative.loyers.model.StatutEcheance;
import com.gestionlocative.loyers.repository.EcheanceRepository;
import com.gestionlocative.loyers.repository.PaiementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Échéance de référence : novembre 2026, 1000.00 DH, date limite = 5 novembre 2026. */
@ExtendWith(MockitoExtension.class)
class PaiementServiceTest {

    private static final LocalDate AVANT_LIMITE = LocalDate.of(2026, 11, 3);
    private static final LocalDate APRES_LIMITE = LocalDate.of(2026, 11, 20);

    @Mock
    PaiementRepository paiementRepository;
    @Mock
    EcheanceRepository echeanceRepository;

    private final EcheanceStatutCalculator calculateur = new EcheanceStatutCalculator(props());

    private PaiementService service(LocalDate aujourdhui) {
        return new PaiementService(paiementRepository, echeanceRepository, calculateur, clockAt(aujourdhui));
    }

    private Echeance echeanceExistante(StatutEcheance statut) {
        Echeance e = echeance("1000.00", 11, 2026, statut);
        when(echeanceRepository.findById(e.getId())).thenReturn(Optional.of(e));
        return e;
    }

    private void paiementsDejaVerses(Echeance e, String total) {
        when(paiementRepository.totalPayePourEcheance(e.getId())).thenReturn(new BigDecimal(total));
    }

    private void saveRenvoieSonArgument() {
        when(paiementRepository.save(any(Paiement.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreatePaiementRequest requete(Echeance e, String montant, LocalDate date) {
        return new CreatePaiementRequest(e.getId(), new BigDecimal(montant), date, "VIREMENT");
    }

    @Test
    @DisplayName("UT-LOY-09 Paiement total -> échéance PAYEE")
    void ut_loy_09_paiement_total_marque_echeance_payee() {
        Echeance e = echeanceExistante(StatutEcheance.A_PAYER);
        paiementsDejaVerses(e, "0");
        saveRenvoieSonArgument();

        PaiementResponse r = service(AVANT_LIMITE).enregistrer(requete(e, "1000.00", AVANT_LIMITE));

        assertThat(r.montant()).isEqualByComparingTo("1000.00");
        assertThat(e.getStatut()).isEqualTo(StatutEcheance.PAYEE);
        verify(echeanceRepository).save(e);
    }

    @Test
    @DisplayName("UT-LOY-10 Paiement partiel avant la date limite -> reste A_PAYER")
    void ut_loy_10_paiement_partiel_avant_limite_reste_a_payer() {
        Echeance e = echeanceExistante(StatutEcheance.A_PAYER);
        paiementsDejaVerses(e, "0");
        saveRenvoieSonArgument();

        service(AVANT_LIMITE).enregistrer(requete(e, "400.00", AVANT_LIMITE));

        assertThat(e.getStatut()).isEqualTo(StatutEcheance.A_PAYER);
    }

    @Test
    @DisplayName("UT-LOY-11 Paiement partiel après la date limite -> EN_RETARD")
    void ut_loy_11_paiement_partiel_apres_limite_donne_en_retard() {
        Echeance e = echeanceExistante(StatutEcheance.A_PAYER);
        paiementsDejaVerses(e, "0");
        saveRenvoieSonArgument();

        service(APRES_LIMITE).enregistrer(requete(e, "400.00", APRES_LIMITE));

        assertThat(e.getStatut()).isEqualTo(StatutEcheance.EN_RETARD);
    }

    @Test
    @DisplayName("UT-LOY-12 Échéance en retard réglée en totalité -> PAYEE")
    void ut_loy_12_echeance_en_retard_reglee_devient_payee() {
        Echeance e = echeanceExistante(StatutEcheance.EN_RETARD);
        paiementsDejaVerses(e, "0");
        saveRenvoieSonArgument();

        service(APRES_LIMITE).enregistrer(requete(e, "1000.00", APRES_LIMITE));

        assertThat(e.getStatut()).isEqualTo(StatutEcheance.PAYEE);
    }

    @Test
    @DisplayName("UT-LOY-13 Second paiement qui complète le montant -> PAYEE")
    void ut_loy_13_second_paiement_complete_le_montant() {
        Echeance e = echeanceExistante(StatutEcheance.A_PAYER);
        paiementsDejaVerses(e, "600.00");
        saveRenvoieSonArgument();

        service(AVANT_LIMITE).enregistrer(requete(e, "400.00", AVANT_LIMITE));

        assertThat(e.getStatut()).isEqualTo(StatutEcheance.PAYEE);
    }

    @Test
    @DisplayName("UT-LOY-14 Date de paiement absente -> date du jour")
    void ut_loy_14_date_absente_utilise_la_date_du_jour() {
        Echeance e = echeanceExistante(StatutEcheance.A_PAYER);
        paiementsDejaVerses(e, "0");
        saveRenvoieSonArgument();

        PaiementResponse r = service(AVANT_LIMITE).enregistrer(requete(e, "100.00", null));

        assertThat(r.datePaiement()).isEqualTo(AVANT_LIMITE);
    }

    @Test
    @DisplayName("UT-LOY-15 Échéance introuvable -> ResourceNotFoundException")
    void ut_loy_15_echeance_introuvable_est_refusee() {
        UUID id = UUID.randomUUID();
        when(echeanceRepository.findById(id)).thenReturn(Optional.empty());
        CreatePaiementRequest req = new CreatePaiementRequest(id, new BigDecimal("100.00"), null, "ESPECES");

        assertThatThrownBy(() -> service(AVANT_LIMITE).enregistrer(req))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(paiementRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-LOY-16 Échéance déjà payée -> paiement refusé")
    void ut_loy_16_echeance_deja_payee_est_refusee() {
        Echeance e = echeanceExistante(StatutEcheance.PAYEE);

        assertThatThrownBy(() -> service(AVANT_LIMITE).enregistrer(requete(e, "100.00", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("déjà payée");
        verify(paiementRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-LOY-17 Montant supérieur au reste à payer -> paiement refusé")
    void ut_loy_17_surpaiement_est_refuse() {
        Echeance e = echeanceExistante(StatutEcheance.A_PAYER);
        paiementsDejaVerses(e, "600.00");

        assertThatThrownBy(() -> service(AVANT_LIMITE).enregistrer(requete(e, "500.00", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reste");
        verify(paiementRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-LOY-18 Date de paiement dans le futur -> paiement refusé")
    void ut_loy_18_date_future_est_refusee() {
        Echeance e = echeanceExistante(StatutEcheance.A_PAYER);

        assertThatThrownBy(() -> service(AVANT_LIMITE).enregistrer(requete(e, "100.00", LocalDate.of(2026, 11, 4))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("futur");
        verify(paiementRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-LOY-19 Lister les paiements d'une échéance inexistante -> ResourceNotFoundException")
    void ut_loy_19_lister_paiements_echeance_inexistante() {
        UUID id = UUID.randomUUID();
        when(echeanceRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service(AVANT_LIMITE).listerParEcheance(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("UT-LOY-20 Historique d'un bail sans échéance -> liste vide")
    void ut_loy_20_historique_bail_sans_echeance_est_vide() {
        UUID bailId = UUID.randomUUID();
        when(echeanceRepository.findByBailIdOrderByAnneeAscMoisAsc(bailId)).thenReturn(List.of());

        assertThat(service(AVANT_LIMITE).historiqueParBail(bailId)).isEmpty();
        verifyNoInteractions(paiementRepository);
    }

    @Test
    @DisplayName("UT-LOY-21 Historique d'un bail -> paiements de ses échéances")
    void ut_loy_21_historique_bail_retourne_les_paiements() {
        UUID bailId = UUID.randomUUID();
        Echeance e1 = echeance("1000.00", 10, 2026, StatutEcheance.PAYEE);
        Echeance e2 = echeance("1000.00", 11, 2026, StatutEcheance.PAYEE);
        when(echeanceRepository.findByBailIdOrderByAnneeAscMoisAsc(bailId)).thenReturn(List.of(e1, e2));
        Paiement p = new Paiement(e2.getId(), new BigDecimal("1000.00"), LocalDate.of(2026, 11, 3), "VIREMENT");
        when(paiementRepository.findByEcheanceIdInOrderByDatePaiementDesc(List.of(e1.getId(), e2.getId())))
                .thenReturn(List.of(p));

        List<PaiementResponse> historique = service(AVANT_LIMITE).historiqueParBail(bailId);

        assertThat(historique).hasSize(1);
        assertThat(historique.get(0).echeanceId()).isEqualTo(e2.getId());
    }
}
