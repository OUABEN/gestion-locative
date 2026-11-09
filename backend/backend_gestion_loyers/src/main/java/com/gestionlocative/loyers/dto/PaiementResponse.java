package com.gestionlocative.loyers.dto;

import com.gestionlocative.loyers.model.Paiement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaiementResponse(
        UUID id,
        UUID echeanceId,
        BigDecimal montant,
        LocalDate datePaiement,
        String modePaiement,
        LocalDateTime createdAt) {

    public static PaiementResponse from(Paiement p) {
        return new PaiementResponse(p.getId(), p.getEcheanceId(), p.getMontant(),
                p.getDatePaiement(), p.getModePaiement(), p.getCreatedAt());
    }
}
