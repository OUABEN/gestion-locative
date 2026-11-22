package com.gestionlocative.quittances.client;

import java.time.LocalDate;
import java.util.List;

import com.gestionlocative.quittances.dto.SituationPeriode.Affectation;

public interface PaiementClient {

    List<Affectation> consulterAffectations(
            String bailId,
            LocalDate debut,
            LocalDate fin
    );
}