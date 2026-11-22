package com.gestionlocative.quittances.service;

import java.util.List;
import java.util.UUID;

import com.gestionlocative.quittances.dto.SituationPeriode.Affectation;
import com.gestionlocative.quittances.entity.PaiementAffectation;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.repository.PaiementAffectationRepository;
import com.gestionlocative.quittances.repository.QuittanceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnregistrementBrouillonService {

    private final QuittanceRepository quittances;
    private final PaiementAffectationRepository affectations;

    public EnregistrementBrouillonService(
            QuittanceRepository quittances,
            PaiementAffectationRepository affectations) {

        this.quittances = quittances;
        this.affectations = affectations;
    }

    @Transactional
    public UUID enregistrer(
            Quittance brouillon,
            List<Affectation> affectationsRetenues) {

        if (affectationsRetenues == null || affectationsRetenues.isEmpty()) {
            throw new IllegalArgumentException(
                    "Un brouillon doit comporter au moins une affectation."
            );
        }

        Quittance enregistree = quittances.saveAndFlush(brouillon);

        List<PaiementAffectation> copies = affectationsRetenues.stream()
                .map(source -> PaiementAffectation.copier(enregistree, source))
                .toList();

        affectations.saveAllAndFlush(copies);

        return enregistree.getId();
    }
}