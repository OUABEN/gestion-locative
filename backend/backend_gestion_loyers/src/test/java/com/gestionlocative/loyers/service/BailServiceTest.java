package com.gestionlocative.loyers.service;

import static com.gestionlocative.loyers.TestFixtures.bail;
import static com.gestionlocative.loyers.TestFixtures.clockAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gestionlocative.loyers.client.BiensClient;
import com.gestionlocative.loyers.client.DisponibiliteBien;
import com.gestionlocative.loyers.client.LocataireDto;
import com.gestionlocative.loyers.client.LocatairesClient;
import com.gestionlocative.loyers.dto.BailResponse;
import com.gestionlocative.loyers.dto.CreateBailRequest;
import com.gestionlocative.loyers.exception.BusinessRuleException;
import com.gestionlocative.loyers.exception.ResourceNotFoundException;
import com.gestionlocative.loyers.model.Bail;
import com.gestionlocative.loyers.model.StatutBail;
import com.gestionlocative.loyers.repository.BailRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BailServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 3);

    @Mock
    BailRepository bailRepository;
    @Mock
    EcheanceService echeanceService;
    @Mock
    BiensClient biensClient;
    @Mock
    LocatairesClient locatairesClient;

    private final UUID bienId = UUID.randomUUID();
    private final UUID locataireId = UUID.randomUUID();

    private BailService service() {
        return new BailService(bailRepository, echeanceService, biensClient, locatairesClient, clockAt(AUJOURDHUI));
    }

    private CreateBailRequest requete(LocalDate debut, LocalDate fin) {
        return new CreateBailRequest(bienId, locataireId, debut, fin, new BigDecimal("1500.00"), new BigDecimal("3000.00"));
    }

    private void locataireExistant() {
        when(locatairesClient.trouver(locataireId))
                .thenReturn(Optional.of(new LocataireDto(locataireId, "Benani", "Youssef", "youssef@exemple.ma")));
    }

    private void bienDisponible(boolean disponible) {
        when(biensClient.disponibilite(bienId))
                .thenReturn(Optional.of(new DisponibiliteBien(bienId, disponible, disponible ? "LIBRE" : "OCCUPE")));
    }

    @Test
    @DisplayName("UT-LOY-38 Créer un bail : bail ACTIF, bien passé à OCCUPE, échéance du mois demandée")
    void ut_loy_38_creer_bail_nominal() {
        locataireExistant();
        bienDisponible(true);
        when(bailRepository.existsByBienIdAndStatut(bienId, StatutBail.ACTIF)).thenReturn(false);
        when(bailRepository.saveAndFlush(any(Bail.class))).thenAnswer(inv -> inv.getArgument(0));
        when(biensClient.changerStatut(bienId, "OCCUPE")).thenReturn(true);

        BailResponse r = service().creer(requete(LocalDate.of(2026, 11, 1), null));

        assertThat(r.statut()).isEqualTo(StatutBail.ACTIF);
        assertThat(r.montantLoyer()).isEqualByComparingTo("1500.00");
        verify(biensClient).changerStatut(bienId, "OCCUPE");
        verify(echeanceService).genererSiConcerne(any(Bail.class), eq(11), eq(2026));
    }

    @Test
    @DisplayName("UT-LOY-39 Bien déjà loué (indisponible) -> création refusée")
    void ut_loy_39_bien_indisponible_est_refuse() {
        locataireExistant();
        bienDisponible(false);

        assertThatThrownBy(() -> service().creer(requete(LocalDate.of(2026, 11, 1), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("déjà loué");
        verify(bailRepository, never()).saveAndFlush(any());
        verify(biensClient, never()).changerStatut(any(), any());
    }

    @Test
    @DisplayName("UT-LOY-40 Locataire introuvable -> ResourceNotFoundException")
    void ut_loy_40_locataire_introuvable() {
        when(locatairesClient.trouver(locataireId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().creer(requete(LocalDate.of(2026, 11, 1), null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Locataire");
        verifyNoInteractions(biensClient);
    }

    @Test
    @DisplayName("UT-LOY-41 Bien introuvable -> ResourceNotFoundException")
    void ut_loy_41_bien_introuvable() {
        locataireExistant();
        when(biensClient.disponibilite(bienId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().creer(requete(LocalDate.of(2026, 11, 1), null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bien");
        verify(bailRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("UT-LOY-42 Date de fin avant la date de début -> création refusée")
    void ut_loy_42_date_fin_avant_debut() {
        assertThatThrownBy(() -> service().creer(requete(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 1))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("date de fin");
        verifyNoInteractions(locatairesClient, biensClient);
    }

    @Test
    @DisplayName("UT-LOY-43 Service Biens répond 409 au passage à OCCUPE -> création refusée")
    void ut_loy_43_conflit_de_statut_sur_biens() {
        locataireExistant();
        bienDisponible(true);
        when(bailRepository.existsByBienIdAndStatut(bienId, StatutBail.ACTIF)).thenReturn(false);
        when(bailRepository.saveAndFlush(any(Bail.class))).thenAnswer(inv -> inv.getArgument(0));
        when(biensClient.changerStatut(bienId, "OCCUPE")).thenReturn(false);

        assertThatThrownBy(() -> service().creer(requete(LocalDate.of(2026, 11, 1), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("déjà occupé");
        verifyNoInteractions(echeanceService);
    }

    @Test
    @DisplayName("UT-LOY-44 Terminer un bail : TERMINE avec date de fin, bien repassé à LIBRE")
    void ut_loy_44_terminer_bail_nominal() {
        Bail b = bail("ACTIF", LocalDate.of(2026, 1, 1), null, "1500.00");
        when(bailRepository.findById(b.getId())).thenReturn(Optional.of(b));
        when(bailRepository.save(any(Bail.class))).thenAnswer(inv -> inv.getArgument(0));
        when(biensClient.changerStatut(b.getBienId(), "LIBRE")).thenReturn(true);

        BailResponse r = service().terminer(b.getId(), LocalDate.of(2026, 11, 30));

        assertThat(r.statut()).isEqualTo(StatutBail.TERMINE);
        assertThat(r.dateFin()).isEqualTo(LocalDate.of(2026, 11, 30));
        verify(biensClient).changerStatut(b.getBienId(), "LIBRE");
    }

    @Test
    @DisplayName("UT-LOY-45 Terminer un bail déjà terminé -> refusé")
    void ut_loy_45_terminer_bail_deja_termine() {
        Bail b = bail("TERMINE", LocalDate.of(2025, 1, 1), LocalDate.of(2026, 6, 30), "1500.00");
        when(bailRepository.findById(b.getId())).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service().terminer(b.getId(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("déjà terminé");
        verify(biensClient, never()).changerStatut(any(), any());
    }

    @Test
    @DisplayName("UT-LOY-46 Terminer un bail alors que le bien est déjà LIBRE (409) -> bail quand même terminé")
    void ut_loy_46_terminer_bail_bien_deja_libre() {
        Bail b = bail("ACTIF", LocalDate.of(2026, 1, 1), null, "1500.00");
        when(bailRepository.findById(b.getId())).thenReturn(Optional.of(b));
        when(bailRepository.save(any(Bail.class))).thenAnswer(inv -> inv.getArgument(0));
        when(biensClient.changerStatut(b.getBienId(), "LIBRE")).thenReturn(false);

        BailResponse r = service().terminer(b.getId(), null);

        assertThat(r.statut()).isEqualTo(StatutBail.TERMINE);
        assertThat(r.dateFin()).isEqualTo(AUJOURDHUI);
    }

    @Test
    @DisplayName("UT-LOY-47 Terminer un bail avec une date de fin avant le début -> refusé")
    void ut_loy_47_terminer_date_avant_debut() {
        Bail b = bail("ACTIF", LocalDate.of(2026, 5, 1), null, "1500.00");
        when(bailRepository.findById(b.getId())).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service().terminer(b.getId(), LocalDate.of(2026, 4, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("date de fin");
        verify(biensClient, never()).changerStatut(any(), any());
    }
}
