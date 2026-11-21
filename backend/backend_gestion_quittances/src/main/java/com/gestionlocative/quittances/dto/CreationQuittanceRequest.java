package com.gestionlocative.quittances.dto;

import java.time.LocalDate;

public record CreationQuittanceRequest(
        String bailId,
        LocalDate periodeDebut,
        LocalDate periodeFin
) {
}