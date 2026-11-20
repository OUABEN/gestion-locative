package com.gestionlocative.quittances.dto;

import java.time.Instant;
import java.util.UUID;

public record ArchiveBrouillonResponse(
        UUID id,
        UUID quittanceId,
        long versionDocument,
        String nature,
        String sha256,
        long tailleOctets,
        Instant creeLe
) {
}