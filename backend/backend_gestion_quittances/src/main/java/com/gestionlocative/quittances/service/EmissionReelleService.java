package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.config.ReferentielsProperties;
import com.gestionlocative.quittances.document.SignataireReel;
import com.gestionlocative.quittances.document.SignatureLocale;
import com.gestionlocative.quittances.pdf.EmissionReellePdfService;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class EmissionReelleService {
    private final ControlePrealableEmissionService controle;
    private final PreparationApercuService preparation;
    private final QuittanceRepository documents;
    private final JdbcTemplate jdbc;
    private final EmissionReellePdfService pdf;
    private final SignataireReel signataire;
    private final EmissionReelleTransactions transactions;
    private final ConsultationQuittanceService consultation;
    private final ReferentielsProperties sources;

    @Value("${quittances.emission-reelle.active:false}") private boolean actif;
    @Value("${quittances.securite.mode:LOCAL}") private String modeJwt;
    @Value("${quittances.integration.paiements.mode:SIMULATION}") private String modePaiements;

    public EmissionReelleService(ControlePrealableEmissionService controle,
            PreparationApercuService preparation, QuittanceRepository documents,
            JdbcTemplate jdbc, EmissionReellePdfService pdf, SignataireReel signataire,
            EmissionReelleTransactions transactions, ConsultationQuittanceService consultation,
            ReferentielsProperties sources) {
        this.controle = controle; this.preparation = preparation; this.documents = documents;
        this.jdbc = jdbc; this.pdf = pdf; this.signataire = signataire;
        this.transactions = transactions; this.consultation = consultation; this.sources = sources;
    }

    @Transactional(propagation = Propagation.NEVER)
    public EmissionReelleTransactions.Resultat emettre(Authentication auth, UUID id,
            Boolean confirmation) {
        if (!Boolean.TRUE.equals(confirmation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmation obligatoire.");
        }
        if (!actif || !"COMMUN".equals(modeJwt)
                || !"UTILISATEUR".equals(modePaiements)
                || sources == null || !modeUtilisateur(sources.locataires())
                || !modeUtilisateur(sources.biens()) || !modeUtilisateur(sources.baux())
                || !signataire.estConfigure()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Émission réelle indisponible : JWT commun, sources réelles et certificat requis.");
        }
        // Contrôle externe préalable : preuves réelles, bailleur, paiements et instantané.
        controle.verifier(auth, id);
        String bailleur = LecteurQuittances.depuis(auth).identifiant();
        var document = documents.findByIdAndBailleurId(id, bailleur)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var donnees = preparation.charger(auth, id);
        if (!id.equals(donnees.document().id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Identifiant incohérent.");
        }
        Instant date = Instant.now();
        Long sequence = jdbc.queryForObject("SELECT nextval('numero_document_seq')", Long.class);
        if (sequence == null) throw new IllegalStateException("Séquence de numérotation absente.");
        String numero = "QT-" + date.atZone(ZoneOffset.UTC).getYear()
                + "-" + String.format(java.util.Locale.ROOT, "%06d", sequence);
        try {
            byte[] contenu = pdf.generer(donnees, numero, date);
            SignatureLocale.Preuve preuve = signataire.signer(contenu, date);
            return transactions.enregistrer(auth, id, document.getVersion(), numero,
                    date, contenu, preuve);
        } catch (IOException | GeneralSecurityException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Émission indisponible : vérifiez le PDF et la configuration du certificat.");
        }
    }

    private static boolean modeUtilisateur(ReferentielsProperties.Endpoint endpoint) {
        return endpoint != null && "UTILISATEUR".equals(endpoint.mode());
    }

    public EmissionReelleTransactions.Verification verifier(Authentication auth, UUID id) {
        var fichier = charger(auth, id);
        return new EmissionReelleTransactions.Verification(
                fichier.numero(), fichier.preuve().sha256(), true, false);
    }

    public byte[] telechargerPdf(Authentication auth, UUID id) {
        return charger(auth, id).pdf();
    }

    public byte[] telechargerPreuve(Authentication auth, UUID id) {
        var fichier = charger(auth, id);
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(sortie)) {
            ajouter(zip, "document.pdf", fichier.pdf());
            ajouter(zip, "document.sig", fichier.preuve().signature());
            ajouter(zip, "certificat.cer", fichier.preuve().certificat());
            ajouter(zip, "LIRE-MOI.txt", ("Signature détachée RSA-SHA256.\n"
                    + "Empreinte SHA-256 : " + fichier.preuve().sha256() + "\n"
                    + "Validité cryptographique vérifiée.\n"
                    + "La chaîne de certification et la valeur juridique doivent être vérifiées séparément.\n")
                    .getBytes(StandardCharsets.UTF_8));
            zip.finish();
            return sortie.toByteArray();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Preuve d'émission indisponible.");
        }
    }

    private EmissionReelleTransactions.Fichier charger(Authentication auth, UUID id) {
        try {
            return transactions.consulter(auth, id, consultation);
        } catch (GeneralSecurityException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Preuve d'émission invalide.");
        }
    }
    private static void ajouter(ZipOutputStream zip, String nom, byte[] contenu)
            throws IOException {
        zip.putNextEntry(new ZipEntry(nom));
        zip.write(contenu);
        zip.closeEntry();
    }
}
