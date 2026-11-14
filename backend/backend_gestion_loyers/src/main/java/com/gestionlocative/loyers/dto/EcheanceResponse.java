package com.gestionlocative.loyers.dto;

import com.gestionlocative.loyers.model.Echeance;
import com.gestionlocative.loyers.model.StatutEcheance;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EcheanceResponse(
        UUID id,
        UUID bailId,
        Integer mois,
        Integer annee,
        BigDecimal montant,
        StatutEcheance statut,
        LocalDate dateLimite,
        BigDecimal totalPaye,
        BigDecimal resteAPayer) {

    public static EcheanceResponse from(Echeance e, LocalDate dateLimite, BigDecimal totalPaye) {
        BigDecimal reste = e.getMontant().subtract(totalPaye).max(BigDecimal.ZERO);
        return new EcheanceResponse(e.getId(), e.getBailId(), e.getMois(), e.getAnnee(),
                e.getMontant(), e.getStatut(), dateLimite, totalPaye, reste);
    }
}
