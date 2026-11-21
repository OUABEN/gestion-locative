package com.gestionlocative.quittances.dto;

import java.util.UUID;

public record CreationQuittanceResponse(
        UUID id,
        String statut,
        String statutSignature
) {
}