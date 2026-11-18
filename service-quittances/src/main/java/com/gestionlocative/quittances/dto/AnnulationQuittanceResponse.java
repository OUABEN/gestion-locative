package com.gestionlocative.quittances.dto;

import java.time.Instant;
import java.util.UUID;

public record AnnulationQuittanceResponse(
        UUID id,
        String statut,
        String statutSignature,
        String motif,
        Instant annuleLe
) {
}