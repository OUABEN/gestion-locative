package com.gestionlocative.loyers.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Réponse de GET /api/biens/{id}/disponibilite. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DisponibiliteBien(UUID id, boolean disponible, String statut) {
}
