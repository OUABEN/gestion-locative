package com.gestionlocative.quittances.dto;

import java.util.List;

public record PageHistoriqueResponse(
        List<EvenementHistoriqueResponse> contenu,
        int page,
        int taille,
        long totalElements,
        long totalPages
) {
    public PageHistoriqueResponse {
        contenu = List.copyOf(contenu);
    }
}