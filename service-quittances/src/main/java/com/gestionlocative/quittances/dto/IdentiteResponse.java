package com.gestionlocative.quittances.dto;

import java.util.List;

public record IdentiteResponse(
        String sujet,
        List<String> autorites
) {

    public IdentiteResponse {
        autorites = List.copyOf(autorites);
    }
}