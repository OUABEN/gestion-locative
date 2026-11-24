package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.config.ReseauDemo;

import com.gestionlocative.quittances.client.PaiementClient;
import com.gestionlocative.quittances.document.SignatureLocale;
import com.gestionlocative.quittances.pdf.EmissionDemoPdfService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.zip.*;

@Service
public class EmissionDemoService {
    @Value("${DARLOC_DOCKER_DEMO:false}")
    private boolean dockerDemo;
    @Value("${DARLOC_SIGNATURE_DEMO_COMMUN:false}")
    private boolean demoCommun;

    private final EmissionDemoTransactions transactions;
    private final EmissionDemoPdfService pdf;
    private final PaiementClient paiements;
    private final boolean actif;
    private final String adresse, jwtMode, chemin, motDePasse;
    public EmissionDemoService(EmissionDemoTransactions transactions, EmissionDemoPdfService pdf, PaiementClient paiements,
            @Value("${DARLOC_SIGNATURE_DEMO:false}") boolean actif,
            @Value("${server.address:}") String adresse,
            @Value("${quittances.securite.mode:}") String jwtMode,
            @Value("${DARLOC_SIGNATURE_PATH:.local-signature/darloc-demo.p12}") String chemin,
            @Value("${DARLOC_SIGNATURE_PASSWORD:}") String motDePasse) {
        this.transactions = transactions; this.pdf = pdf; this.paiements = paiements;
        this.actif = actif; this.adresse = adresse; this.jwtMode = jwtMode;
        this.chemin = chemin; this.motDePasse = motDePasse;
    }
    public EmissionDemoTransactions.Resultat emettre(Authentication auth, UUID id, Boolean confirmation) {
        if (!Boolean.TRUE.equals(confirmation)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La confirmation est obligatoire.");
        if (!actif || !ReseauDemo.ecouteAutorisee(adresse, dockerDemo) || !("LOCAL".equals(jwtMode) || (dockerDemo && demoCommun && "COMMUN".equals(jwtMode))) || motDePasse.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Signature de démonstration non activée sur ce serveur local.");
        var source = transactions.charger(auth, id);
        // Appel distant hors transaction. Tout changement impose une nouvelle préparation.
        verifierPaiements(source);
        try {
            byte[] contenu = pdf.generer(source.donnees(), source.numero(), source.date());
            var preuve = SignatureLocale.signer(contenu, Path.of(chemin), motDePasse.toCharArray(), source.date());
            return transactions.enregistrer(auth, id, source, contenu, preuve);
        } catch (IOException | GeneralSecurityException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Émission impossible : vérifiez la police PDF, le certificat local et son mot de passe.");
        }
    }
    private void verifierPaiements(EmissionDemoTransactions.Source source) {
        var doc = source.donnees().document();
        var actuels = paiements.consulterAffectations(doc.bailId(), doc.periodeDebut(), doc.periodeFin());
        var confirmes = actuels.stream().filter(a -> a.statut() == com.gestionlocative.quittances.dto.SituationPeriode.StatutPaiement.CONFIRME
            && (a.nature() == com.gestionlocative.quittances.dto.SituationPeriode.Nature.LOYER
             || a.nature() == com.gestionlocative.quittances.dto.SituationPeriode.Nature.CHARGES)).toList();
        var archives = source.donnees().affectations();
        Set<String> ids = new HashSet<>();
        boolean coherent = confirmes.size() == archives.size() && !archives.isEmpty();
        for (var a : confirmes) {
            if (!ids.add(a.affectationId())) coherent = false;
            boolean trouve = archives.stream().anyMatch(b -> Objects.equals(b.affectationId(), a.affectationId())
                && Objects.equals(b.paiementId(), a.paiementId()) && Objects.equals(a.bailId(), doc.bailId())
                && Objects.equals(b.periodeDebut(), a.periodeDebut()) && Objects.equals(b.periodeFin(), a.periodeFin())
                && Objects.equals(b.nature(), a.nature().name()) && a.montant() != null
                && b.montant().compareTo(a.montant()) == 0 && Objects.equals(b.devise(), a.devise()));
            if (!trouve) coherent = false;
        }
        if (!coherent) throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Les paiements ont changé depuis la préparation. Réexaminez le document avant de le remplacer.");
    }
    public record Verification(String modeSignature, boolean integriteValide, boolean signatureValide, String sha256, String numero) {}
    private EmissionDemoTransactions.Fichier verifier(Authentication auth, UUID id) {
        var fichier = transactions.consulter(auth, id);
        try {
            if (!SignatureLocale.verifier(fichier.pdf(), fichier.preuve(), fichier.date()))
                throw new GeneralSecurityException("Preuve invalide");
            return fichier;
        } catch (GeneralSecurityException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Échec du contrôle d'intégrité ou de signature. Téléchargement bloqué.");
        }
    }
    public Verification verifierDocument(Authentication auth, UUID id) {
        var f = verifier(auth, id);
        transactions.tracerLecture(auth, id, "VERIFICATION_DEMO");
        return new Verification("DEMONSTRATION", true, true, f.preuve().sha256(), f.numero());
    }
    public byte[] telecharger(Authentication auth, UUID id, boolean archive) {
        var f = verifier(auth, id);
        byte[] contenu = f.pdf();
        if (archive) {
            try (var sortie = new ByteArrayOutputStream(); var zip = new ZipOutputStream(sortie)) {
                ajouter(zip, "document.pdf", f.pdf());
                ajouter(zip, "document.sig", f.preuve().signature());
                ajouter(zip, "certificat.cer", f.preuve().certificat());
                ajouter(zip, "LIRE-MOI.txt", ("DEMONSTRATION : certificat auto-signe de test, aucune identite certifiee.\n"
                    + "Signature detachee SHA256withRSA du PDF exact, non integree au PDF.\n"
                    + "SHA-256 : " + f.preuve().sha256() + "\n"
                    + "Le certificat et la date sont conserves par DarLoc, sans horodatage de confiance.\n")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.finish(); contenu = sortie.toByteArray();
            } catch (IOException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Archive indisponible."); }
        }
        transactions.tracerLecture(auth, id, archive ? "TELECHARGEMENT_PREUVE_DEMO" : "TELECHARGEMENT_PDF_DEMO");
        return contenu;
    }
    private static void ajouter(ZipOutputStream zip, String nom, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(nom)); zip.write(bytes); zip.closeEntry();
    }
}
