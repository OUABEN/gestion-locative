package com.gestionlocative.quittances.dto;

public record AnnulationQuittanceRequest(
        String motif,
        Boolean confirmation
) {
}