package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.document.SignataireReel;
import com.gestionlocative.quittances.document.SignatureLocale;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.StatutQuittance;
import com.gestionlocative.quittances.entity.StatutSignature;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.security.IdentiteAudit;
import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class EmissionReelleTransactions {
    private final QuittanceRepository documents;
    private final JdbcTemplate jdbc;
    private final IdentiteAudit audit;
    private final SignataireReel signataire;

    public EmissionReelleTransactions(QuittanceRepository documents, JdbcTemplate jdbc,
            IdentiteAudit audit, SignataireReel signataire) {
        this.documents = documents;
        this.jdbc = jdbc;
        this.audit = audit;
        this.signataire = signataire;
    }

    public record Resultat(UUID id, String numero, String statut, String statutSignature,
            String modeSignature, String sha256, Instant emisLe) {}
    public record Verification(String numero, String sha256,
            boolean signatureCryptographiqueValide, boolean signatureQualifiee) {}
    public record Fichier(byte[] pdf, SignatureLocale.Preuve preuve,
            Instant date, String numero) {
        @Override public String toString() { return "Fichier[contenu protégé]"; }
    }

    @Transactional(timeout = 10, rollbackFor = Exception.class)
    public Resultat enregistrer(Authentication authentification, UUID id, Long version,
            String numero, Instant date, byte[] pdf, SignatureLocale.Preuve preuve)
            throws GeneralSecurityException {
        var lecteur = LecteurQuittances.depuis(authentification);
        if (!lecteur.bailleur()) throw new org.springframework.security.access.AccessDeniedException(
                "Émission réservée au bailleur.");
        Quittance document = documents.trouverPourModification(id, lecteur.identifiant())
                .orElseThrow(EmissionReelleTransactions::absent);
        if (!Objects.equals(document.getVersion(), version)
                || document.getStatut() != StatutQuittance.EN_ATTENTE_SIGNATURE
                || document.getStatutSignature() != StatutSignature.EN_ATTENTE
                || document.getTypeDocument() != TypeDocument.QUITTANCE_INTEGRALE
                || document.getEmisLe() != null
                || document.getLoyerDu().compareTo(document.getLoyerPaye()) != 0
                || document.getChargesDues().compareTo(document.getChargesPayees()) != 0) {
            throw conflit("Document changé, non validé ou paiement incomplet.");
        }
        if (pdf == null || pdf.length > 10485760 || pdf.length == 0
                || !signataire.verifier(pdf, preuve, date)) {
            throw conflit("La signature du PDF ne peut pas être vérifiée.");
        }
        jdbc.update("""
                INSERT INTO emission_reelle(quittance_id,numero,pdf,signature,certificat,sha256,cree_le)
                VALUES (?,?,?,?,?,?,?)
                """, id, numero, pdf, preuve.signature(), preuve.certificat(), preuve.sha256(),
                Timestamp.from(date));
        document.emettreReelle(numero, date, preuve.sha256());
        documents.saveAndFlush(document);
        var acteur = audit.depuis(authentification);
        jdbc.update("""
                INSERT INTO quittance_audit(id,quittance_id,acteur,emetteur_identite,operation,
                    resultat,horodatage,statut_avant,statut_apres)
                VALUES (?,?,?,?,?,'SUCCES',?,?,?)
                """, UUID.randomUUID(), id, acteur.sujet(), acteur.emetteurIdentite(),
                "EMISSION_REELLE", Timestamp.from(Instant.now()), "EN_ATTENTE_SIGNATURE", "EMIS");
        return new Resultat(id, numero, "EMIS", "SIGNE_VERIFIE",
                "SIGNATURE_DETACHEE", preuve.sha256(), date);
    }

    @Transactional(readOnly = true)
    public Fichier consulter(Authentication auth, UUID id,
            ConsultationQuittanceService consultation) throws GeneralSecurityException {
        var document = consultation.consulter(auth, id);
        if (!"EMIS".equals(document.statut())) throw absent();
        List<Fichier> fichiers = jdbc.query("""
                SELECT pdf,signature,certificat,sha256,cree_le,numero
                FROM emission_reelle WHERE quittance_id=?
                """, (rs, i) -> new Fichier(rs.getBytes("pdf"),
                new SignatureLocale.Preuve(rs.getBytes("signature"),
                        rs.getBytes("certificat"), rs.getString("sha256")),
                rs.getTimestamp("cree_le").toInstant(), rs.getString("numero")), id);
        if (fichiers.isEmpty()) throw absent();
        var fichier = fichiers.getFirst();
        var quittance = documents.findById(id).orElseThrow(EmissionReelleTransactions::absent);
        if (!Objects.equals(document.numero(), fichier.numero())
                || !Objects.equals(quittance.getNumero(), fichier.numero())
                || !Objects.equals(quittance.getPdfSha256(), fichier.preuve().sha256())
                || !Objects.equals(quittance.getPdfReference(), "db-reelle:" + id)
                || !signataire.verifier(fichier.pdf(), fichier.preuve(), fichier.date())) {
            throw conflit("Preuve cryptographique invalide.");
        }
        return fichier;
    }

    private static ResponseStatusException absent() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Émission introuvable.");
    }
    private static ResponseStatusException conflit(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
