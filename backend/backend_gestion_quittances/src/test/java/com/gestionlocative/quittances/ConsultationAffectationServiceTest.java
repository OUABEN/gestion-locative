package com.gestionlocative.quittances;

import com.gestionlocative.quittances.entity.PaiementAffectation;
import com.gestionlocative.quittances.repository.PaiementAffectationRepository;
import com.gestionlocative.quittances.service.ConsultationAffectationService;
import com.gestionlocative.quittances.service.ConsultationQuittanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultationAffectationServiceTest {

    private ConsultationQuittanceService consultationQuittanceService;
    private PaiementAffectationRepository repository;
    private ConsultationAffectationService service;
    private Authentication authentication;

    @BeforeEach
    void preparer() {
        consultationQuittanceService =
                mock(ConsultationQuittanceService.class);

        repository = mock(PaiementAffectationRepository.class);
        authentication = mock(Authentication.class);

        service = new ConsultationAffectationService(
                consultationQuittanceService,
                repository
        );
    }

    @Test
    void doitVerifierAccesAvantLectureDesAffectations() {
        UUID quittanceId = UUID.randomUUID();

        when(repository
                .findByQuittance_IdOrderByAffectationExterneIdAsc(quittanceId))
                .thenReturn(List.of());

        var resultat = service.consulter(authentication, quittanceId);

        assertEquals(0, resultat.size());

        InOrder ordre = inOrder(
                consultationQuittanceService,
                repository
        );

        ordre.verify(consultationQuittanceService)
                .consulter(authentication, quittanceId);

        ordre.verify(repository)
                .findByQuittance_IdOrderByAffectationExterneIdAsc(quittanceId);
    }

    @Test
    void doitRetournerInformationsFigees() {
        UUID quittanceId = UUID.randomUUID();
        PaiementAffectation affectation = mock(PaiementAffectation.class);

        when(affectation.getAffectationExterneId())
                .thenReturn("AFF-TEST-001");

        when(affectation.getPaiementExterneId())
                .thenReturn("PAY-TEST-001");

        when(affectation.getPeriodeDebut())
                .thenReturn(LocalDate.of(2026, 10, 1));

        when(affectation.getPeriodeFin())
                .thenReturn(LocalDate.of(2026, 10, 31));

        when(affectation.getNature()).thenReturn("LOYER");
        when(affectation.getMontant()).thenReturn(new BigDecimal("1500.00"));
        when(affectation.getDevise()).thenReturn("MAD");
        when(affectation.getStatutSource()).thenReturn("CONFIRME");

        when(repository
                .findByQuittance_IdOrderByAffectationExterneIdAsc(quittanceId))
                .thenReturn(List.of(affectation));

        var resultat = service.consulter(authentication, quittanceId);

        assertEquals(1, resultat.size());

        var premiere = resultat.getFirst();

        assertEquals("AFF-TEST-001", premiere.affectationId());
        assertEquals("PAY-TEST-001", premiere.paiementId());
        assertEquals(LocalDate.of(2026, 10, 1), premiere.periodeDebut());
        assertEquals(LocalDate.of(2026, 10, 31), premiere.periodeFin());
        assertEquals("LOYER", premiere.nature());
        assertEquals(new BigDecimal("1500.00"), premiere.montant());
        assertEquals("MAD", premiere.devise());
        assertEquals("CONFIRME", premiere.statutSourceALaPreparation());
    }

    @Test
    void neDoitPasLireAffectationsSiDocumentInaccessible() {
        UUID quittanceId = UUID.randomUUID();

        ResponseStatusException refus = new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Document introuvable."
        );

        when(consultationQuittanceService.consulter(
                authentication,
                quittanceId
        )).thenThrow(refus);

        ResponseStatusException resultat = assertThrows(
                ResponseStatusException.class,
                () -> service.consulter(authentication, quittanceId)
        );

        assertSame(refus, resultat);
        verifyNoInteractions(repository);
    }
}