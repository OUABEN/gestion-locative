package com.gestionlocative.quittances.dto;

import java.util.List;

public record PageArchivesBrouillonResponse(
        List<ArchiveBrouillonResponse> contenu,
        int page,
        int taille,
        long totalElements,
        long totalPages
) {
    public PageArchivesBrouillonResponse {
        contenu = List.copyOf(contenu);
    }
}
