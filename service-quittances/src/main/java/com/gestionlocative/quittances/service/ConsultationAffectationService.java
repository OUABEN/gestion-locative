package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.dto.AffectationQuittanceResponse;
import com.gestionlocative.quittances.entity.PaiementAffectation;
import com.gestionlocative.quittances.repository.PaiementAffectationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConsultationAffectationService {

    private final ConsultationQuittanceService consultationQuittanceService;
    private final PaiementAffectationRepository affectationRepository;

    public ConsultationAffectationService(
            ConsultationQuittanceService consultationQuittanceService,
            PaiementAffectationRepository affectationRepository
    ) {
        this.consultationQuittanceService = consultationQuittanceService;
        this.affectationRepository = affectationRepository;
    }

    @Transactional(readOnly = true)
    public List<AffectationQuittanceResponse> consulter(
            Authentication authentication,
            UUID quittanceId
    ) {
        consultationQuittanceService.consulter(
                authentication,
                quittanceId
        );

        return affectationRepository
                .findByQuittance_IdOrderByAffectationExterneIdAsc(quittanceId)
                .stream()
                .map(this::convertir)
                .toList();
    }

    private AffectationQuittanceResponse convertir(
            PaiementAffectation affectation
    ) {
        return new AffectationQuittanceResponse(
                affectation.getAffectationExterneId(),
                affectation.getPaiementExterneId(),
                affectation.getPeriodeDebut(),
                affectation.getPeriodeFin(),
                affectation.getNature(),
                affectation.getMontant(),
                affectation.getDevise(),
                affectation.getStatutSource()
        );
    }
}