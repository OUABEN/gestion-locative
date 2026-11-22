package com.gestionlocative.quittances.dto;

import java.time.Instant;
import java.util.UUID;

public record EvenementHistoriqueResponse(
        UUID id,
        String operation,
        String resultat,
        Instant horodatage,
        String statutAvant,
        String statutApres,
        String motif
) {
}