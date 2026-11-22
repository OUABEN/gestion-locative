package com.gestionlocative.quittances.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record PaiementsPeriodeResponse(
        String versionContrat,
        Boolean complet,
        String bailId,
        LocalDate periodeDebut,
        LocalDate periodeFin,
        List<SituationPeriode.Affectation> affectations
) {

    public PaiementsPeriodeResponse {
        affectations = List.copyOf(Objects.requireNonNull(affectations));
    }
}