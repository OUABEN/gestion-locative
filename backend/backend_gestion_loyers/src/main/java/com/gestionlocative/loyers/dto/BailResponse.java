package com.gestionlocative.loyers.dto;

import com.gestionlocative.loyers.model.Bail;
import com.gestionlocative.loyers.model.StatutBail;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BailResponse(
        UUID id,
        UUID bienId,
        UUID locataireId,
        LocalDate dateDebut,
        LocalDate dateFin,
        BigDecimal montantLoyer,
        BigDecimal depotGarantie,
        StatutBail statut) {

    public static BailResponse from(Bail b) {
        return new BailResponse(b.getId(), b.getBienId(), b.getLocataireId(), b.getDateDebut(),
                b.getDateFin(), b.getMontantLoyer(), b.getDepotGarantie(), b.getStatut());
    }
}
