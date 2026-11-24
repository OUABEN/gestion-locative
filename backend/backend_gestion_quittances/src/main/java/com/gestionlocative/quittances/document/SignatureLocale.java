package com.gestionlocative.quittances.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.HexFormat;
import java.time.Instant;

/** Signature détachée de démonstration, sans certification d'identité. */
public final class SignatureLocale {
    private SignatureLocale() {}
    public record Preuve(byte[] signature, byte[] certificat, String sha256) {
        @Override
        public boolean equals(Object autre) {
            if (this == autre) return true;
            if (!(autre instanceof Preuve preuve)) return false;
            return Arrays.equals(signature, preuve.signature)
                    && Arrays.equals(certificat, preuve.certificat)
                    && java.util.Objects.equals(sha256, preuve.sha256);
        }

        @Override
        public int hashCode() {
            int resultat = Arrays.hashCode(signature);
            resultat = 31 * resultat + Arrays.hashCode(certificat);
            resultat = 31 * resultat + java.util.Objects.hashCode(sha256);
            return resultat;
        }

        @Override
        public String toString() {
            // Résumé du contenu des tableaux : jamais de signature/certificat brut dans les logs.
            return "Preuve[tailleSignature=" + (signature == null ? 0 : signature.length)
                    + ", hashSignatureJava=" + Arrays.hashCode(signature)
                    + ", tailleCertificat=" + (certificat == null ? 0 : certificat.length)
                    + ", hashCertificatJava=" + Arrays.hashCode(certificat)
                    + ", sha256=" + sha256 + "]";
        }
    }
    public static Preuve signer(byte[] pdf, Path chemin, char[] motDePasse, Instant date)
            throws GeneralSecurityException, IOException {
        try {
            KeyStore store = KeyStore.getInstance("PKCS12");
            try (var flux = Files.newInputStream(chemin)) { store.load(flux, motDePasse); }
            var cle = store.getKey("darloc-demo", motDePasse);
            if (!(cle instanceof PrivateKey privee) || !(store.getCertificate("darloc-demo") instanceof X509Certificate certificat))
                throw new GeneralSecurityException("Clé de démonstration absente.");
            certificat.checkValidity(Date.from(date));
            if (!(certificat.getPublicKey() instanceof RSAPublicKey rsa) || rsa.getModulus().bitLength() < 2048)
                throw new GeneralSecurityException("Clé RSA insuffisante.");
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privee); signature.update(pdf);
            var preuve = new Preuve(signature.sign(), certificat.getEncoded(), empreinte(pdf));
            if (!verifier(pdf, preuve, date)) throw new GeneralSecurityException("Signature non vérifiable.");
            return preuve;
        } finally { Arrays.fill(motDePasse, '\0'); }
    }
    public static String empreinte(byte[] contenu) throws GeneralSecurityException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
    }
    public static boolean verifier(byte[] pdf, Preuve preuve, Instant date) throws GeneralSecurityException {
        if (!empreinte(pdf).equals(preuve.sha256())) return false;
        var certificat = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(preuve.certificat()));
        certificat.checkValidity(Date.from(date));
        Signature verification = Signature.getInstance("SHA256withRSA");
        verification.initVerify(certificat.getPublicKey()); verification.update(pdf);
        return verification.verify(preuve.signature());
    }
}
