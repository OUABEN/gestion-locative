package com.gestionlocative.quittances.dto;

import java.math.BigDecimal;
import java.util.List;

import com.gestionlocative.quittances.entity.TypeDocument;

public record ResultatCalcul(
        BigDecimal loyerDu,
        BigDecimal chargesDues,
        BigDecimal loyerPaye,
        BigDecimal chargesPayees,
        BigDecimal totalPaye,
        TypeDocument typeDocument,
        List<SituationPeriode.Affectation> affectationsRetenues
) {

    public ResultatCalcul {
        affectationsRetenues = List.copyOf(affectationsRetenues);
    }
}