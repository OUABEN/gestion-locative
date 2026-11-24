package com.gestionlocative.loyers.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Bail : table baux (04_baux.sql). Un seul bail ACTIF par bien (index unique partiel). */
@Entity
@Table(name = "baux")
public class Bail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bien_id", nullable = false)
    private UUID bienId;

    @Column(name = "locataire_id", nullable = false)
    private UUID locataireId;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "montant_loyer", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantLoyer;

    @Column(name = "depot_garantie", nullable = false, precision = 12, scale = 2)
    private BigDecimal depotGarantie = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatutBail statut = StatutBail.ACTIF;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Bail() {
    }

    public Bail(UUID bienId, UUID locataireId, LocalDate dateDebut, LocalDate dateFin,
                BigDecimal montantLoyer, BigDecimal depotGarantie) {
        this.bienId = bienId;
        this.locataireId = locataireId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.montantLoyer = montantLoyer;
        this.depotGarantie = depotGarantie;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBienId() { return bienId; }
    public void setBienId(UUID bienId) { this.bienId = bienId; }
    public UUID getLocataireId() { return locataireId; }
    public void setLocataireId(UUID locataireId) { this.locataireId = locataireId; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public BigDecimal getMontantLoyer() { return montantLoyer; }
    public void setMontantLoyer(BigDecimal montantLoyer) { this.montantLoyer = montantLoyer; }
    public BigDecimal getDepotGarantie() { return depotGarantie; }
    public void setDepotGarantie(BigDecimal depotGarantie) { this.depotGarantie = depotGarantie; }
    public StatutBail getStatut() { return statut; }
    public void setStatut(StatutBail statut) { this.statut = statut; }
}
