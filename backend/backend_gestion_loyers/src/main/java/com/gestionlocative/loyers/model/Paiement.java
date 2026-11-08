package com.gestionlocative.loyers.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "paiements")
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "echeance_id", nullable = false)
    private UUID echeanceId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_paiement", nullable = false)
    private LocalDate datePaiement;

    @Column(name = "mode_paiement", nullable = false, length = 30)
    private String modePaiement;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (datePaiement == null) datePaiement = LocalDate.now();
    }

    public Paiement() {
    }

    public Paiement(UUID echeanceId, BigDecimal montant, LocalDate datePaiement, String modePaiement) {
        this.echeanceId = echeanceId;
        this.montant = montant;
        this.datePaiement = datePaiement;
        this.modePaiement = modePaiement;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getEcheanceId() { return echeanceId; }
    public void setEcheanceId(UUID echeanceId) { this.echeanceId = echeanceId; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public LocalDate getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDate datePaiement) { this.datePaiement = datePaiement; }
    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
