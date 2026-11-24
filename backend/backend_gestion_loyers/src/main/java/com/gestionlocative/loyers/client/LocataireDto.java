package com.gestionlocative.loyers.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Fiche locataire renvoyée par le service Locataires. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocataireDto(UUID id, String nom, String prenom, String email) {
}
