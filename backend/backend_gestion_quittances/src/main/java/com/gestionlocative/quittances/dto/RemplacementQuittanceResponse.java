package com.gestionlocative.quittances.dto;

import java.util.UUID;

public record RemplacementQuittanceResponse(
        UUID id,
        UUID documentPrecedentId,
        String statut,
        String statutSignature
) {
}