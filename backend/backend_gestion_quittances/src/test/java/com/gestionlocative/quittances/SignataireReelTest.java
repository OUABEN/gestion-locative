package com.gestionlocative.quittances;

import com.gestionlocative.quittances.document.SignataireReel;
import com.gestionlocative.quittances.document.SignatureLocale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SignataireReelTest {

    @TempDir
    static Path repertoire;

    private static final String ALIAS = "signature-test";
    private static final String SECRET = UUID.randomUUID().toString();

    private static Path magasin;
    private static String empreinte;
    private static Instant date;

    private SignataireReel signataire;

    private final byte[] pdf =
            "%PDF-1.7 document de test"
                    .getBytes(StandardCharsets.UTF_8);

    @BeforeAll
    static void preparerCertificat() throws Exception {
        magasin = repertoire.resolve("signature-test.p12");

        String executable = System.getProperty("os.name")
                .toLowerCase(java.util.Locale.ROOT).contains("win")
                ? "keytool.exe" : "keytool";

        Path keytool = Path.of(
                System.getProperty("java.home"), "bin", executable);

        Path journal = repertoire.resolve("keytool.log");

        Process processus = new ProcessBuilder(
                keytool.toString(),
                "-genkeypair",
                "-alias", ALIAS,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-sigalg", "SHA256withRSA",
                "-validity", "2",
                "-dname", "CN=Certificat temporaire JUnit",
                "-ext", "KU=digitalSignature",
                "-storetype", "PKCS12",
                "-keystore", magasin.toString(),
                "-storepass", SECRET,
                "-keypass", SECRET,
                "-noprompt")
                .redirectErrorStream(true)
                .redirectOutput(journal.toFile())
                .start();

        if (!processus.waitFor(30, TimeUnit.SECONDS)) {
            processus.destroyForcibly();
            fail("Generation du certificat trop longue");
        }

        assertEquals(0, processus.exitValue(),
                "Echec de la generation du certificat de test");

        KeyStore ks = KeyStore.getInstance("PKCS12");

        try (var entree = Files.newInputStream(magasin)) {
            ks.load(entree, SECRET.toCharArray());
        }

        empreinte = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(ks.getCertificate(ALIAS).getEncoded()));

        date = Instant.now();
    }

    @BeforeEach
    void configurer() {
        signataire = new SignataireReel();

        ReflectionTestUtils.setField(
                signataire, "chemin", magasin.toString());
        ReflectionTestUtils.setField(
                signataire, "alias", ALIAS);
        ReflectionTestUtils.setField(
                signataire, "motDePasse", SECRET);
        ReflectionTestUtils.setField(
                signataire, "empreinteCertificat", empreinte);
    }

    @Test
    void configurationValide() {
        assertTrue(signataire.estConfigure());
    }

    @Test
    void configurationIncompleteRefusee() {
        ReflectionTestUtils.setField(signataire, "chemin", "");

        assertFalse(signataire.estConfigure());

        assertThrows(GeneralSecurityException.class,
                () -> signataire.signer(pdf, date));
    }

    @Test
    void empreinteMalFormeeRefusee() {
        ReflectionTestUtils.setField(
                signataire, "empreinteCertificat", "incorrecte");

        assertFalse(signataire.estConfigure());
    }

    @Test
    void signatureReelleValide() throws Exception {
        var preuve = signataire.signer(pdf, date);

        assertNotNull(preuve);
        assertTrue(preuve.signature().length > 0);
        assertTrue(preuve.certificat().length > 0);

        assertEquals(SignatureLocale.empreinte(pdf), preuve.sha256());
        assertTrue(signataire.verifier(pdf, preuve, date));
    }

    @Test
    void modificationDuPdfDetectee() throws Exception {
        var preuve = signataire.signer(pdf, date);

        byte[] modifie = pdf.clone();
        modifie[0] ^= 1;

        assertFalse(signataire.verifier(modifie, preuve, date));
    }

    @Test
    void certificatNonAutoriseRefuse() {
        ReflectionTestUtils.setField(
                signataire, "empreinteCertificat", "0".repeat(64));

        assertThrows(GeneralSecurityException.class,
                () -> signataire.signer(pdf, date));
    }

    @Test
    void certificatExpireRefuse() throws Exception {
        var preuve = signataire.signer(pdf, date);

        assertThrows(GeneralSecurityException.class,
                () -> signataire.verifier(
                        pdf, preuve, date.plus(10, ChronoUnit.DAYS)));
    }

    @Test
    void aliasInexistantRefuse() {
        ReflectionTestUtils.setField(
                signataire, "alias", "alias-inexistant");

        assertThrows(GeneralSecurityException.class,
                () -> signataire.signer(pdf, date));
    }

    @Test
    void pdfVideRefuse() {
        assertThrows(GeneralSecurityException.class,
                () -> signataire.signer(new byte[0], date));
    }

    @Test
    void pdfNullRefuse() {
        assertThrows(GeneralSecurityException.class,
                () -> signataire.signer(null, date));
    }

    @Test
    void dateAbsenteRefusee() {
        assertThrows(GeneralSecurityException.class,
                () -> signataire.signer(pdf, null));
    }

    @Test
    void verificationSansPreuveRefusee() throws Exception {
        assertFalse(signataire.verifier(pdf, null, date));
    }

    @Test
    void verificationSansPdfRefusee() throws Exception {
        var preuve = signataire.signer(pdf, date);

        assertFalse(signataire.verifier(null, preuve, date));
    }

    @Test
    void verificationSansDateRefusee() throws Exception {
        var preuve = signataire.signer(pdf, date);

        assertFalse(signataire.verifier(pdf, preuve, null));
    }
}