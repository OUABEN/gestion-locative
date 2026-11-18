package com.gestionlocative.quittances.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AffectationQuittanceResponse(
        String affectationId,
        String paiementId,
        LocalDate periodeDebut,
        LocalDate periodeFin,
        String nature,
        BigDecimal montant,
        String devise,
        String statutSourceALaPreparation
) {
}