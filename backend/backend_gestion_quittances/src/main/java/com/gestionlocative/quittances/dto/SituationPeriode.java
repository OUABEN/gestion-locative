package com.gestionlocative.quittances.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record SituationPeriode(
        String bailId,
        LocalDate debutBail,
        LocalDate finBail,
        LocalDate periodeDebut,
        LocalDate periodeFin,
        BigDecimal loyerDu,
        BigDecimal chargesDues,
        String devise,
        List<Affectation> affectations
) {

    public SituationPeriode {
        affectations = List.copyOf(Objects.requireNonNull(affectations));
    }

    public record Affectation(
            String affectationId,
            String paiementId,
            String bailId,
            LocalDate periodeDebut,
            LocalDate periodeFin,
            Nature nature,
            BigDecimal montant,
            String devise,
            StatutPaiement statut
    ) {
    }

    public enum Nature {
        LOYER,
        CHARGES,
        DEPOT_GARANTIE,
        EXCEDENT
    }

    public enum StatutPaiement {
        EN_ATTENTE,
        CONFIRME,
        ANNULE,
        REMBOURSE
    }
}