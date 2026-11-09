package com.gestionlocative.loyers.service;

import static com.gestionlocative.loyers.TestFixtures.bail;
import static com.gestionlocative.loyers.TestFixtures.clockAt;
import static com.gestionlocative.loyers.TestFixtures.echeance;
import static com.gestionlocative.loyers.TestFixtures.props;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestionlocative.loyers.client.BailDto;
import com.gestionlocative.loyers.client.LocatairesClient;
import com.gestionlocative.loyers.dto.EcheanceResponse;
import com.gestionlocative.loyers.dto.GenerationResultat;
import com.gestionlocative.loyers.exception.BusinessRuleException;
import com.gestionlocative.loyers.exception.ResourceNotFoundException;
import com.gestionlocative.loyers.model.Echeance;
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

@ExtendWith(MockitoExtension.class)
class EcheanceServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 3);

    @Mock
    EcheanceRepository echeanceRepository;
    @Mock
    PaiementRepository paiementRepository;
    @Mock
    LocatairesClient locatairesClient;

    private final EcheanceStatutCalculator calculateur = new EcheanceStatutCalculator(props());

    private EcheanceService service(LocalDate aujourdhui) {
        return new EcheanceService(echeanceRepository, paiementRepository, locatairesClient,
                calculateur, clockAt(aujourdhui));
    }

    @Test
    @DisplayName("UT-LOY-22 Générer l'échéance d'un bail actif -> A_PAYER au montant du loyer")
    void ut_loy_22_generer_pour_bail_actif() {
        BailDto b = bail("ACTIF", LocalDate.of(2026, 1, 1), null, "1500.00");
        when(locatairesClient.trouverBail(b.id())).thenReturn(Optional.of(b));
        when(echeanceRepository.existsByBailIdAndMoisAndAnnee(b.id(), 11, 2026)).thenReturn(false);
        when(echeanceRepository.save(any(Echeance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paiementRepository.totalPayePourEcheance(any())).thenReturn(BigDecimal.ZERO);

        EcheanceResponse r = service(AUJOURDHUI).genererPourBail(b.id(), 11, 2026);

        assertThat(r.bailId()).isEqualTo(b.id());
        assertThat(r.montant()).isEqualByComparingTo("1500.00");
        assertThat(r.statut()).isEqualTo(StatutEcheance.A_PAYER);
        assertThat(r.dateLimite()).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    @Test
    @DisplayName("UT-LOY-23 Échéance générée pour un mois ancien -> statut IMPAYEE")
    void ut_loy_23_generer_pour_mois_passe_donne_impayee() {
        BailDto b = bail("ACTIF", LocalDate.of(2026, 1, 1), null, "1500.00");
        when(locatairesClient.trouverBail(b.id())).thenReturn(Optional.of(b));
        when(echeanceRepository.existsByBailIdAndMoisAndAnnee(b.id(), 9, 2026)).thenReturn(false);
        when(echeanceRepository.save(any(Echeance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paiementRepository.totalPayePourEcheance(any())).thenReturn(BigDecimal.ZERO);

        EcheanceResponse r = service(AUJOURDHUI).genererPourBail(b.id(), 9, 2026);

        assertThat(r.statut()).isEqualTo(StatutEcheance.IMPAYEE);
    }

    @Test
    @DisplayName("UT-LOY-24 Bail introuvable -> ResourceNotFoundException")
    void ut_loy_24_bail_introuvable() {
        UUID id = UUID.randomUUID();
        when(locatairesClient.trouverBail(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(AUJOURDHUI).genererPourBail(id, 11, 2026))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(echeanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-LOY-25 Bail TERMINE -> génération refusée")
    void ut_loy_25_bail_termine_est_refuse() {
        BailDto b = bail("TERMINE", LocalDate.of(2025, 1, 1), LocalDate.of(2026, 12, 31), "1500.00");
        when(locatairesClient.trouverBail(b.id())).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service(AUJOURDHUI).genererPourBail(b.id(), 11, 2026))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pas actif");
        verify(echeanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-LOY-26 Bail qui commence après le mois demandé -> refusé")
    void ut_loy_26_bail_pas_encore_commence_est_refuse() {
        BailDto b = bail("ACTIF", LocalDate.of(2027, 1, 1), null, "1500.00");
        when(locatairesClient.trouverBail(b.id())).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service(AUJOURDHUI).genererPourBail(b.id(), 11, 2026))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ne couvre pas");
        verify(echeanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-LOY-27 Bail terminé avant le mois demandé -> refusé")
    void ut_loy_27_bail_fini_avant_le_mois_est_refuse() {
        BailDto b = bail("ACTIF", LocalDate.of(2025, 1, 1), LocalDate.of(2026, 10, 31), "1500.00");
        when(locatairesClient.trouverBail(b.id())).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service(AUJOURDHUI).genererPourBail(b.id(), 11, 2026))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ne couvre pas");
        verify(echeanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-LOY-28 Échéance déjà existante pour ce bail et ce mois -> refusé (doublon)")
    void ut_loy_28_doublon_est_refuse() {
        BailDto b = bail("ACTIF", LocalDate.of(2026, 1, 1), null, "1500.00");
        when(locatairesClient.trouverBail(b.id())).thenReturn(Optional.of(b));
        when(echeanceRepository.existsByBailIdAndMoisAndAnnee(b.id(), 11, 2026)).thenReturn(true);

        assertThatThrownBy(() -> service(AUJOURDHUI).genererPourBail(b.id(), 11, 2026))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("existe déjà");
        verify(echeanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-LOY-29 Génération mensuelle : 1 créée, 1 déjà existante, 1 bail non concerné ignoré")
    void ut_loy_29_generation_mensuelle() {
        BailDto nouveau = bail("ACTIF", LocalDate.of(2026, 1, 1), null, "1500.00");
        BailDto existant = bail("ACTIF", LocalDate.of(2026, 1, 1), null, "2000.00");
        BailDto futur = bail("ACTIF", LocalDate.of(2027, 1, 1), null, "1800.00");
        when(locatairesClient.listerBauxActifs()).thenReturn(List.of(nouveau, existant, futur));
        when(echeanceRepository.existsByBailIdAndMoisAndAnnee(nouveau.id(), 11, 2026)).thenReturn(false);
        when(echeanceRepository.existsByBailIdAndMoisAndAnnee(existant.id(), 11, 2026)).thenReturn(true);
        when(echeanceRepository.save(any(Echeance.class))).thenAnswer(inv -> inv.getArgument(0));

        GenerationResultat r = service(AUJOURDHUI).genererPourMois(11, 2026);

        assertThat(r.creees()).isEqualTo(1);
        assertThat(r.dejaExistantes()).isEqualTo(1);
        verify(echeanceRepository, times(1)).save(any(Echeance.class));
    }

    @Test
    @DisplayName("UT-LOY-30 Mise à jour des statuts : A_PAYER -> EN_RETARD, EN_RETARD -> IMPAYEE, inchangé sinon")
    void ut_loy_30_mise_a_jour_des_statuts() {
        Echeance basculeEnRetard = echeance("1000.00", 11, 2026, StatutEcheance.A_PAYER);
        Echeance basculeImpayee = echeance("1000.00", 8, 2026, StatutEcheance.EN_RETARD);
        Echeance inchangee = echeance("1000.00", 12, 2026, StatutEcheance.A_PAYER);
        when(echeanceRepository.findByStatutIn(anyCollection()))
                .thenReturn(List.of(basculeEnRetard, basculeImpayee, inchangee));
        when(paiementRepository.totalPayePourEcheance(any())).thenReturn(BigDecimal.ZERO);

        int modifiees = service(LocalDate.of(2026, 11, 10)).mettreAJourStatuts();

        assertThat(modifiees).isEqualTo(2);
        assertThat(basculeEnRetard.getStatut()).isEqualTo(StatutEcheance.EN_RETARD);
        assertThat(basculeImpayee.getStatut()).isEqualTo(StatutEcheance.IMPAYEE);
        assertThat(inchangee.getStatut()).isEqualTo(StatutEcheance.A_PAYER);
        verify(echeanceRepository).save(basculeEnRetard);
        verify(echeanceRepository).save(basculeImpayee);
        verify(echeanceRepository, never()).save(inchangee);
    }

    @Test
    @DisplayName("UT-LOY-31 Recherche par bail et par statut -> seules les échéances du statut demandé")
    void ut_loy_31_recherche_par_bail_et_statut() {
        UUID bailId = UUID.randomUUID();
        Echeance payee = echeance("1000.00", 9, 2026, StatutEcheance.PAYEE);
        Echeance enRetard = echeance("1000.00", 10, 2026, StatutEcheance.EN_RETARD);
        Echeance aPayer = echeance("1000.00", 11, 2026, StatutEcheance.A_PAYER);
        when(echeanceRepository.findByBailIdOrderByAnneeAscMoisAsc(bailId))
                .thenReturn(List.of(payee, enRetard, aPayer));
        when(paiementRepository.totalPayePourEcheance(any())).thenReturn(BigDecimal.ZERO);

        List<EcheanceResponse> r = service(AUJOURDHUI).rechercher(bailId, StatutEcheance.EN_RETARD);

        assertThat(r).hasSize(1);
        assertThat(r.get(0).id()).isEqualTo(enRetard.getId());
    }

    @Test
    @DisplayName("UT-LOY-32 Recherche sans filtre -> toutes les échéances")
    void ut_loy_32_recherche_sans_filtre() {
        when(echeanceRepository.findAll()).thenReturn(List.of(
                echeance("1000.00", 10, 2026, StatutEcheance.PAYEE),
                echeance("1000.00", 11, 2026, StatutEcheance.A_PAYER)));
        when(paiementRepository.totalPayePourEcheance(any())).thenReturn(BigDecimal.ZERO);

        assertThat(service(AUJOURDHUI).rechercher(null, null)).hasSize(2);
    }

    @Test
    @DisplayName("UT-LOY-33 Consulter une échéance inexistante -> ResourceNotFoundException")
    void ut_loy_33_echeance_inexistante() {
        UUID id = UUID.randomUUID();
        when(echeanceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(AUJOURDHUI).trouver(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
