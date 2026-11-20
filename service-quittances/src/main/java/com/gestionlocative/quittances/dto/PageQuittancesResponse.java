package com.gestionlocative.quittances.dto;

import java.util.List;

public record PageQuittancesResponse(
        List<QuittanceConsultationResponse> contenu,
        int page,
        int taille,
        long totalElements,
        int totalPages
) {

    public PageQuittancesResponse {
        contenu = List.copyOf(contenu);
    }
}