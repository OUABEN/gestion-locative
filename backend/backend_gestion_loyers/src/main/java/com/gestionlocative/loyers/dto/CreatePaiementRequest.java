package com.gestionlocative.loyers.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePaiementRequest(
        @NotNull UUID echeanceId,
        @NotNull @DecimalMin("0.01") BigDecimal montant,
        LocalDate datePaiement,
        @NotBlank @Size(max = 30) String modePaiement) {
}
