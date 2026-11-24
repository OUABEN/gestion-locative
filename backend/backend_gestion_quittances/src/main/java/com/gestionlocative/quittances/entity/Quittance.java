package com.gestionlocative.quittances.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "quittance")
public class Quittance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "numero", length = 40, unique = true)
    private String numero;

    @Column(name = "bail_id", nullable = false, length = 100)
    private String bailId;

    @Column(name = "locataire_id", nullable = false, length = 100)
    private String locataireId;

    @Column(name = "bien_id", nullable = false, length = 100)
    private String bienId;

    @Column(name = "bailleur_id", nullable = false, length = 100)
    private String bailleurId;

    @Column(name = "periode_debut", nullable = false)
    private LocalDate periodeDebut;

    @Column(name = "periode_fin", nullable = false)
    private LocalDate periodeFin;

    @Column(name = "loyer_du", nullable = false, precision = 12, scale = 2)
    private BigDecimal loyerDu;

    @Column(name = "charges_dues", nullable = false, precision = 12, scale = 2)
    private BigDecimal chargesDues;

    @Column(name = "loyer_paye", nullable = false, precision = 12, scale = 2)
    private BigDecimal loyerPaye;

    @Column(name = "charges_payees", nullable = false, precision = 12, scale = 2)
    private BigDecimal chargesPayees;

    @Column(name = "total_paye", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPaye;

    @Column(name = "devise", nullable = false, length = 3)
    private String devise;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type_document", nullable = false, length = 30)
    private TypeDocument typeDocument;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutQuittance statut;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "statut_signature", nullable = false, length = 30)
    private StatutSignature statutSignature;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe;

    @Column(name = "emis_le")
    private Instant emisLe;

    @Column(name = "pdf_reference", length = 255)
    private String pdfReference;

    @Column(name = "pdf_sha256", length = 64)
    private String pdfSha256;

    @Column(name = "motif_annulation", length = 2000)
    private String motifAnnulation;

    @Column(name = "document_precedent_id")
    private UUID documentPrecedentId;

    @Column(name = "motif_remplacement", length = 2000)
    private String motifRemplacement;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "donnees_figees", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> donneesFigees;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Quittance() {
    }

    public static Quittance creerBrouillon(
            String bailId,
            String locataireId,
            String bienId,
            String bailleurId,
            LocalDate periodeDebut,
            LocalDate periodeFin,
            BigDecimal loyerDu,
            BigDecimal chargesDues,
            BigDecimal loyerPaye,
            BigDecimal chargesPayees,
            TypeDocument typeDocument,
            Map<String, String> donneesFigees) {

        Quittance quittance = new Quittance();

        quittance.bailId = Objects.requireNonNull(bailId);
        quittance.locataireId = Objects.requireNonNull(locataireId);
        quittance.bienId = Objects.requireNonNull(bienId);
        quittance.bailleurId = Objects.requireNonNull(bailleurId);

        quittance.periodeDebut = Objects.requireNonNull(periodeDebut);
        quittance.periodeFin = Objects.requireNonNull(periodeFin);

        quittance.loyerDu = normaliserMontant(loyerDu);
        quittance.chargesDues = normaliserMontant(chargesDues);
        quittance.loyerPaye = normaliserMontant(loyerPaye);
        quittance.chargesPayees = normaliserMontant(chargesPayees);

        quittance.totalPaye = quittance.loyerPaye.add(quittance.chargesPayees);
        quittance.devise = "MAD";

        quittance.typeDocument = Objects.requireNonNull(typeDocument);
        quittance.statut = StatutQuittance.BROUILLON;
        quittance.statutSignature = StatutSignature.NON_SIGNE;
        quittance.creeLe = Instant.now();

        quittance.donneesFigees = new HashMap<>(
                Map.copyOf(Objects.requireNonNull(donneesFigees))
        );

        return quittance;
    }

    private static BigDecimal normaliserMontant(BigDecimal montant) {
        return Objects.requireNonNull(montant)
                .setScale(2, RoundingMode.UNNECESSARY);
    }

    public UUID getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public String getBailId() {
        return bailId;
    }

    public String getLocataireId() {
        return locataireId;
    }

    public String getBienId() {
        return bienId;
    }

    public String getBailleurId() {
        return bailleurId;
    }

    public LocalDate getPeriodeDebut() {
        return periodeDebut;
    }

    public LocalDate getPeriodeFin() {
        return periodeFin;
    }

    public BigDecimal getLoyerDu() {
        return loyerDu;
    }

    public BigDecimal getChargesDues() {
        return chargesDues;
    }

    public BigDecimal getLoyerPaye() {
        return loyerPaye;
    }

    public BigDecimal getChargesPayees() {
        return chargesPayees;
    }

    public BigDecimal getTotalPaye() {
        return totalPaye;
    }
    public void validerPourSignature() {
        boolean statutAutorise =
                statut == StatutQuittance.BROUILLON
                        || statut == StatutQuittance.EN_ATTENTE_VALIDATION;

        if (!statutAutorise
                || statutSignature != StatutSignature.NON_SIGNE
                || emisLe != null) {
            throw new IllegalStateException(
                    "Ce document ne peut pas être validé pour signature."
            );
        }

        statut = StatutQuittance.EN_ATTENTE_SIGNATURE;
        statutSignature = StatutSignature.EN_ATTENTE;
    }
    public String getDevise() {
        return devise;
    }

    public TypeDocument getTypeDocument() {
        return typeDocument;
    }

    public StatutQuittance getStatut() {
        return statut;
    }

    public StatutSignature getStatutSignature() {
        return statutSignature;
    }

    public Instant getCreeLe() {
        return creeLe;
    }

    public Instant getEmisLe() {
        return emisLe;
    }

    public String getPdfReference() {
        return pdfReference;
    }

    public String getPdfSha256() {
        return pdfSha256;
    }

    public String getMotifAnnulation() {
        return motifAnnulation;
    }

    public UUID getDocumentPrecedentId() {
        return documentPrecedentId;
    }

    public String getMotifRemplacement() {
        return motifRemplacement;
    }

    public Map<String, String> getDonneesFigees() {
        return Map.copyOf(donneesFigees);
    }

    public Long getVersion() {
        return version;
    }
    public void annuler(String motif) {
        if (statut == StatutQuittance.ANNULE
                || statut == StatutQuittance.REMPLACE) {
            throw new IllegalStateException(
                    "Ce document ne peut plus être annulé."
            );
        }

        if (motif == null
                || motif.isBlank()
                || motif.strip().length() > 2000) {
            throw new IllegalArgumentException(
                    "Un motif de 1 à 2000 caractères est obligatoire."
            );
        }

        motifAnnulation = motif.strip();
        statut = StatutQuittance.ANNULE;

        if (statutSignature == StatutSignature.EN_ATTENTE) {
            statutSignature = StatutSignature.NON_SIGNE;
        }
    }
    public void rattacherDocumentPrecedent(
            java.util.UUID precedentId,
            String motif
    ) {
        if (statut != StatutQuittance.BROUILLON
                || emisLe != null
                || documentPrecedentId != null) {
            throw new IllegalStateException(
                    "Le lien de remplacement ne peut pas être modifié."
            );
        }

        if (precedentId == null || precedentId.equals(id)) {
            throw new IllegalArgumentException(
                    "Le document précédent est invalide."
            );
        }

        if (motif == null
                || motif.isBlank()
                || motif.strip().length() > 2000) {
            throw new IllegalArgumentException(
                    "Un motif de remplacement est obligatoire."
            );
        }

        documentPrecedentId = precedentId;
        motifRemplacement = motif.strip();
    }
    public void emettreDemonstration(String reference, Instant date, String empreinte) {
        if (statut != StatutQuittance.EN_ATTENTE_SIGNATURE
                || statutSignature != StatutSignature.EN_ATTENTE || emisLe != null) {
            throw new IllegalStateException("Le document doit être validé avant émission.");
        }
        if (reference == null || !reference.matches("DEMO-QT-[0-9]{4}-[0-9]+")
                || date == null || empreinte == null || !empreinte.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Preuve d'émission incorrecte.");
        }
        numero = reference;
        emisLe = date;
        pdfReference = "db-demo:" + id;
        pdfSha256 = empreinte;
        statutSignature = StatutSignature.SIGNE_VERIFIE;
        statut = StatutQuittance.EMIS;
    }
}