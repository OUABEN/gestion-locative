package com.gestionlocative.loyers.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateBailRequest(
        @NotNull UUID bienId,
        @NotNull UUID locataireId,
        @NotNull LocalDate dateDebut,
        LocalDate dateFin,
        @NotNull @DecimalMin("0.00") BigDecimal montantLoyer,
        @DecimalMin("0.00") BigDecimal depotGarantie) {
}
