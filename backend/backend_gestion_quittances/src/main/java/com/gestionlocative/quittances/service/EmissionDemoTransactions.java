package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.document.SignatureLocale;
import com.gestionlocative.quittances.dto.DonneesApercu;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class EmissionDemoTransactions {
    private final PreparationApercuService preparation;
    private final ConsultationQuittanceService consultation;
    private final QuittanceRepository documents;
    private final JdbcTemplate jdbc;
    public EmissionDemoTransactions(PreparationApercuService preparation,
            ConsultationQuittanceService consultation, QuittanceRepository documents, JdbcTemplate jdbc) {
        this.preparation = preparation; this.consultation = consultation;
        this.documents = documents; this.jdbc = jdbc;
    }
    public record Source(DonneesApercu donnees, long version, String numero, Instant date) {}
    public record Resultat(UUID id, String numero, String statut, String statutSignature,
            String modeSignature, String sha256, Instant emisLe) {}
    public record Fichier(byte[] pdf, SignatureLocale.Preuve preuve, Instant date, String numero) {
        @Override
        public boolean equals(Object autre) {
            if (this == autre) return true;
            if (!(autre instanceof Fichier fichier)) return false;
            return java.util.Arrays.equals(pdf, fichier.pdf)
                    && Objects.equals(preuve, fichier.preuve)
                    && Objects.equals(date, fichier.date)
                    && Objects.equals(numero, fichier.numero);
        }

        @Override
        public int hashCode() {
            int resultat = java.util.Arrays.hashCode(pdf);
            resultat = 31 * resultat + Objects.hashCode(preuve);
            resultat = 31 * resultat + Objects.hashCode(date);
            resultat = 31 * resultat + Objects.hashCode(numero);
            return resultat;
        }

        @Override
        public String toString() {
            // N'affiche pas le PDF brut (données documentaires potentiellement privées).
            return "Fichier[taillePdf=" + (pdf == null ? 0 : pdf.length)
                    + ", hashPdfJava=" + java.util.Arrays.hashCode(pdf)
                    + ", preuve=" + preuve + ", date=" + date + ", numero=" + numero + "]";
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 10)
    public Source charger(Authentication auth, UUID id) {
        String bailleur = bailleur(auth);
        var donnees = preparation.charger(auth, id);
        var q = documents.findByIdAndBailleurId(id, bailleur).orElseThrow(EmissionDemoTransactions::absent);
        verifierEtat(q);
        Instant date = Instant.now();
        Long compteur = jdbc.queryForObject("SELECT nextval('numero_document_seq')", Long.class);
        String numero = "DEMO-QT-" + date.atZone(java.time.ZoneId.of("Africa/Casablanca")).getYear()
                + "-" + String.format(java.util.Locale.ROOT, "%06d", compteur);
        return new Source(donnees, q.getVersion(), numero, date);
    }
    @Transactional(timeout = 10, rollbackFor = Exception.class)
    public Resultat enregistrer(Authentication auth, UUID id, Source source, byte[] pdf,
            SignatureLocale.Preuve preuve) throws java.security.GeneralSecurityException {
        String bailleur = bailleur(auth);
        var q = documents.trouverPourModification(id, bailleur).orElseThrow(EmissionDemoTransactions::absent);
        verifierEtat(q);
        if (!id.equals(source.donnees().document().id()) || !Objects.equals(q.getVersion(), source.version()))
            throw conflit("Le document a changé. Rechargez ses détails.");
        if (pdf.length > 10485760 || !SignatureLocale.verifier(pdf, preuve, source.date()))
            throw new java.security.GeneralSecurityException("Preuve invalide.");
        jdbc.update("""
            INSERT INTO emission_demo(quittance_id,pdf,signature,certificat,sha256,cree_le)
            VALUES (?,?,?,?,?,?)
            """, id, pdf, preuve.signature(), preuve.certificat(), preuve.sha256(), Timestamp.from(source.date()));
        q.emettreDemonstration(source.numero(), source.date(), preuve.sha256());
        documents.saveAndFlush(q);
        audit(auth, id, "EMISSION_DEMO", "EN_ATTENTE_SIGNATURE", "EMIS");
        return new Resultat(id, q.getNumero(), "EMIS", "SIGNE_VERIFIE", "DEMONSTRATION", preuve.sha256(), source.date());
    }
    @Transactional(readOnly = true)
    public Fichier consulter(Authentication auth, UUID id) {
        var q = consultation.consulter(auth, id); // périmètre bailleur/locataire contrôlé
        var lignes = jdbc.query("SELECT pdf, signature, certificat, sha256, cree_le FROM emission_demo WHERE quittance_id=?",
            (rs, n) -> new Fichier(rs.getBytes("pdf"), new SignatureLocale.Preuve(rs.getBytes("signature"),
                rs.getBytes("certificat"), rs.getString("sha256")), rs.getTimestamp("cree_le").toInstant(), q.numero()), id);
        if (lignes.isEmpty()) throw absent();
        return lignes.getFirst();
    }
    @Transactional(timeout = 10)
    public void tracerLecture(Authentication auth, UUID id, String operation) {
        var q = consultation.consulter(auth, id);
        audit(auth, id, operation, q.statut(), q.statut());
    }
    private void audit(Authentication auth, UUID id, String operation, String avant, String apres) {
        var jwt = (JwtAuthenticationToken) auth;
        String sujet = jwt.getToken().getSubject(), issuer = jwt.getToken().getClaimAsString("iss");
        if (sujet == null || sujet.isBlank() || sujet.length() > 200 || issuer == null || issuer.isBlank() || issuer.length() > 500)
            throw new AccessDeniedException("Identité insuffisante pour l'audit.");
        jdbc.update("""
            INSERT INTO quittance_audit(id,quittance_id,acteur,emetteur_identite,operation,resultat,horodatage,statut_avant,statut_apres)
            VALUES (?,?,?,?,?,'SUCCES',?,?,?)
            """, UUID.randomUUID(), id, sujet, issuer, operation, Timestamp.from(Instant.now()), avant, apres);
    }
    private static String bailleur(Authentication auth) {
        var lecteur = LecteurQuittances.depuis(auth);
        if (!lecteur.bailleur()) throw new AccessDeniedException("Émission réservée au bailleur.");
        return lecteur.identifiant();
    }
    private static void verifierEtat(Quittance q) {
        if (!"EN_ATTENTE_SIGNATURE".equals(q.getStatut().name()) || !"EN_ATTENTE".equals(q.getStatutSignature().name()) || q.getEmisLe() != null)
            throw conflit("Le document doit être validé et en attente de signature.");
    }
    private static ResponseStatusException absent() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Document émis introuvable."); }
    private static ResponseStatusException conflit(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
