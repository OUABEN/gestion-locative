package com.gestionlocative.quittances.dto;

import java.time.Instant;
import java.util.UUID;

public record ValidationQuittanceResponse(
        UUID id,
        String statut,
        String statutSignature,
        Instant valideLe
) {
}