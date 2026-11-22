package com.gestionlocative.quittances.dto;

import java.util.List;

public record DonneesApercu(
        QuittanceConsultationResponse document,
        List<AffectationQuittanceResponse> affectations
) {
    public DonneesApercu {
        affectations = List.copyOf(affectations);
    }
}