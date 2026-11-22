package com.gestionlocative.quittances.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record QuittanceConsultationResponse(
        UUID id,
        String numero,
        String bailId,
        String locataireId,
        String bienId,
        String nomBailleur,
        String nomLocataire,
        String prenomLocataire,
        String adresseBien,
        LocalDate periodeDebut,
        LocalDate periodeFin,
        BigDecimal loyerDu,
        BigDecimal chargesDues,
        BigDecimal loyerPaye,
        BigDecimal chargesPayees,
        BigDecimal totalPaye,
        String devise,
        String typeDocument,
        String statut,
        String statutSignature,
        Instant creeLe,
        Instant emisLe,
        UUID documentPrecedentId,
        String motifRemplacement
) {
 public QuittanceConsultationResponse(
        UUID id,
        String numero,
        String bailId,
        String locataireId,
        String bienId,
        String nomBailleur,
        String nomLocataire,
        String prenomLocataire,
        String adresseBien,
        LocalDate periodeDebut,
        LocalDate periodeFin,
        BigDecimal loyerDu,
        BigDecimal chargesDues,
        BigDecimal loyerPaye,
        BigDecimal chargesPayees,
        BigDecimal totalPaye,
        String devise,
        String typeDocument,
        String statut,
        String statutSignature,
        Instant creeLe,
        Instant emisLe
) { this(id, numero, bailId, locataireId, bienId, nomBailleur, nomLocataire, prenomLocataire, adresseBien, periodeDebut, periodeFin, loyerDu, chargesDues, loyerPaye, chargesPayees, totalPaye, devise, typeDocument, statut, statutSignature, creeLe, emisLe, null, null); }
}