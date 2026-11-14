package com.gestionlocative.loyers.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Bail tel que renvoyé par le service Locataires (camelCase ou snake_case acceptés). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BailDto(
        UUID id,
        @JsonAlias("bien_id") UUID bienId,
        @JsonAlias("locataire_id") UUID locataireId,
        @JsonAlias("date_debut") LocalDate dateDebut,
        @JsonAlias("date_fin") LocalDate dateFin,
        @JsonAlias("montant_loyer") BigDecimal montantLoyer,
        @JsonAlias("depot_garantie") BigDecimal depotGarantie,
        String statut) {
}
