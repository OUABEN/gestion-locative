package com.gestionlocative.loyers.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenererEcheancesRequest(
        @NotNull @Min(1) @Max(12) Integer mois,
        @NotNull @Min(2000) Integer annee) {
}
