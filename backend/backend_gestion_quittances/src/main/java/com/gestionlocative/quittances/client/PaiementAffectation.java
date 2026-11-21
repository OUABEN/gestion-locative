package com.gestionlocative.quittances.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.gestionlocative.quittances.dto.SituationPeriode.Affectation;
import com.gestionlocative.quittances.dto.SituationPeriode.Nature;
import com.gestionlocative.quittances.dto.SituationPeriode.StatutPaiement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "paiement_affectation")
public class PaiementAffectation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quittance_id", nullable = false, updatable = false)
    private Quittance quittance;

    @Column(name = "affectation_externe_id", nullable = false, length = 100)
    private String affectationExterneId;

    @Column(name = "paiement_externe_id", nullable = false, length = 100)
    private String paiementExterneId;

    @Column(name = "periode_debut", nullable = false)
    private LocalDate periodeDebut;

    @Column(name = "periode_fin", nullable = false)
    private LocalDate periodeFin;

    @Column(name = "nature", nullable = false, length = 20)
    private String nature;

    @Column(name = "montant", nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(name = "devise", nullable = false, length = 3)
    private String devise;

    @Column(name = "statut_source", nullable = false, length = 20)
    private String statutSource;

    protected PaiementAffectation() {
    }

    public static PaiementAffectation copier(
            Quittance quittance,
            Affectation source) {

        Objects.requireNonNull(quittance);
        Objects.requireNonNull(source);

        if (!quittance.getBailId().equals(source.bailId())
                || !quittance.getPeriodeDebut().equals(source.periodeDebut())
                || !quittance.getPeriodeFin().equals(source.periodeFin())
                || source.statut() != StatutPaiement.CONFIRME
                || (source.nature() != Nature.LOYER
                && source.nature() != Nature.CHARGES)
                || !"MAD".equals(source.devise())
                || source.montant() == null
                || source.montant().signum() <= 0) {
            throw new IllegalArgumentException(
                    "L'affectation ne correspond pas au document."
            );
        }

        PaiementAffectation copie = new PaiementAffectation();

        copie.quittance = quittance;
        copie.affectationExterneId = source.affectationId();
        copie.paiementExterneId = source.paiementId();
        copie.periodeDebut = source.periodeDebut();
        copie.periodeFin = source.periodeFin();
        copie.nature = source.nature().name();
        copie.montant = source.montant().setScale(2, RoundingMode.UNNECESSARY);
        copie.devise = source.devise();
        copie.statutSource = source.statut().name();

        return copie;
    }

    public UUID getId() {
        return id;
    }

    public String getAffectationExterneId() {
        return affectationExterneId;
    }

    public String getPaiementExterneId() {
        return paiementExterneId;
    }

    public LocalDate getPeriodeDebut() {
        return periodeDebut;
    }

    public LocalDate getPeriodeFin() {
        return periodeFin;
    }

    public String getNature() {
        return nature;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public String getDevise() {
        return devise;
    }

    public String getStatutSource() {
        return statutSource;
    }
}