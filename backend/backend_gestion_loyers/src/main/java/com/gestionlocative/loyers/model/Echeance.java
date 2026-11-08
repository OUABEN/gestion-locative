package com.gestionlocative.loyers.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "echeances")
public class Echeance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bail_id", nullable = false)
    private UUID bailId;

    @Column(nullable = false)
    private Integer mois;

    @Column(nullable = false)
    private Integer annee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatutEcheance statut = StatutEcheance.A_PAYER;

    public Echeance() {
    }

    public Echeance(UUID bailId, Integer mois, Integer annee, BigDecimal montant) {
        this.bailId = bailId;
        this.mois = mois;
        this.annee = annee;
        this.montant = montant;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBailId() { return bailId; }
    public void setBailId(UUID bailId) { this.bailId = bailId; }
    public Integer getMois() { return mois; }
    public void setMois(Integer mois) { this.mois = mois; }
    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public StatutEcheance getStatut() { return statut; }
    public void setStatut(StatutEcheance statut) { this.statut = statut; }
}
