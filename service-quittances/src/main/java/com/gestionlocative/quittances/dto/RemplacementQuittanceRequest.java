package com.gestionlocative.quittances.dto;

public record RemplacementQuittanceRequest(
        String motif,
        Boolean confirmation
) {
}