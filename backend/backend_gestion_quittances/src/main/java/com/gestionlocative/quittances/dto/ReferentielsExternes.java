package com.gestionlocative.quittances.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ReferentielsExternes {

    private ReferentielsExternes() {
    }

    public record Locataire(
            String versionContrat,
            String id,
            String nom,
            String prenom,
            String email,
            Boolean emailVerifie
    ) {
    }

    public record Bien(
            String versionContrat,
            String id,
            String adresse,
            String bailleurId
    ) {
    }

    public record BailPeriode(
            String versionContrat,
            String bailId,
            String locataireId,
            String bienId,
            String bailleurId,
            String nomBailleur,
            LocalDate debutBail,
            LocalDate finBail,
            LocalDate periodeDebut,
            LocalDate periodeFin,
            BigDecimal loyerDu,
            BigDecimal chargesDues,
            String devise
    ) {
    }
}