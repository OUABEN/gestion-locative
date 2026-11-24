package com.gestionlocative.quittances.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Signature détachée avec certificat explicitement épinglé : ne prouve pas à elle seule la qualification juridique. */
@Component
public class SignataireReel {
    @Value("${quittances.emission-reelle.pkcs12-path:}") private String chemin;
    @Value("${quittances.emission-reelle.pkcs12-alias:}") private String alias;
    @Value("${quittances.emission-reelle.pkcs12-password:}") private String motDePasse;
    @Value("${quittances.emission-reelle.certificat-sha256:}") private String empreinteCertificat;

    public boolean estConfigure() {
        return present(chemin) && present(alias) && present(motDePasse)
                && empreinteCertificat != null && empreinteCertificat.matches("[0-9a-f]{64}");
    }

    public SignatureLocale.Preuve signer(byte[] pdf, Instant date)
            throws IOException, GeneralSecurityException {
        if (!estConfigure() || pdf == null || pdf.length == 0 || date == null) {
            throw new GeneralSecurityException("Configuration de signature incomplète.");
        }
        char[] secret = motDePasse.toCharArray();
        try {
            KeyStore magasin = KeyStore.getInstance("PKCS12");
            try (var entree = Files.newInputStream(Path.of(chemin))) {
                magasin.load(entree, secret);
            }
            if (!(magasin.getCertificate(alias) instanceof X509Certificate certificat)
                    || !(magasin.getKey(alias, secret) instanceof PrivateKey privee)) {
                throw new GeneralSecurityException("Certificat ou clé privée absent.");
            }
            verifierCertificat(certificat, date);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privee);
            signature.update(pdf);
            SignatureLocale.Preuve preuve = new SignatureLocale.Preuve(
                    signature.sign(), certificat.getEncoded(), SignatureLocale.empreinte(pdf));
            if (!verifier(pdf, preuve, date)) {
                throw new GeneralSecurityException("Signature non vérifiable.");
            }
            return preuve;
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    public boolean verifier(byte[] pdf, SignatureLocale.Preuve preuve, Instant date)
            throws GeneralSecurityException {
        if (!estConfigure() || pdf == null || preuve == null || date == null) return false;
        X509Certificate certificat = (X509Certificate) java.security.cert.CertificateFactory
                .getInstance("X.509").generateCertificate(
                        new java.io.ByteArrayInputStream(preuve.certificat()));
        verifierCertificat(certificat, date);
        return SignatureLocale.verifier(pdf, preuve, date);
    }

    private void verifierCertificat(X509Certificate certificat, Instant date)
            throws GeneralSecurityException {
        certificat.checkValidity(Date.from(date));
        if (!(certificat.getPublicKey() instanceof RSAPublicKey rsa)
                || rsa.getModulus().bitLength() < 2048) {
            throw new GeneralSecurityException("Clé publique insuffisante.");
        }
        boolean[] usages = certificat.getKeyUsage();
        if (usages != null && (usages.length == 0 || !usages[0])) {
            throw new GeneralSecurityException("Certificat non autorisé à la signature numérique.");
        }
        String empreinte = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(certificat.getEncoded()));
        if (!MessageDigest.isEqual(
                empreinte.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                empreinteCertificat.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
            throw new GeneralSecurityException("Empreinte du certificat non autorisée.");
        }
    }

    private static boolean present(String valeur) {
        return valeur != null && !valeur.isBlank();
    }
}
