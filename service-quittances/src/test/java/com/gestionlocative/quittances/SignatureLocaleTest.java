package com.gestionlocative.quittances;

import com.gestionlocative.quittances.document.SignatureLocale;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SignatureLocaleTest {
    @TempDir Path dossier;
    @Test void preuveVerifiableEtAlterationsDetectees() throws Exception {
        Path magasin=dossier.resolve("test.p12");
        String executable=System.getProperty("os.name").toLowerCase().contains("win") ? "keytool.exe" : "keytool";
        var processus=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin",executable).toString(),
            "-genkeypair","-alias","darloc-demo","-keyalg","RSA","-keysize","2048","-sigalg","SHA256withRSA",
            "-validity","1","-storetype","PKCS12","-keystore",magasin.toString(),"-storepass","secret-test-ephemere",
            "-dname","CN=Test temporaire").redirectErrorStream(true).redirectOutput(dossier.resolve("keytool.log").toFile()).start();
        assertTrue(processus.waitFor(30,java.util.concurrent.TimeUnit.SECONDS)); assertEquals(0,processus.exitValue());
        byte[] pdf="%PDF-1.4 exemple".getBytes(java.nio.charset.StandardCharsets.UTF_8);var date=Instant.now();
        var preuve=SignatureLocale.signer(pdf,magasin,"secret-test-ephemere".toCharArray(),date);
        assertTrue(SignatureLocale.verifier(pdf,preuve,date));
        byte[] altere=pdf.clone();altere[0]^=1;
        assertFalse(SignatureLocale.verifier(altere,preuve,date));
        byte[] sig=preuve.signature().clone();sig[0]^=1;
        assertFalse(SignatureLocale.verifier(pdf,new SignatureLocale.Preuve(sig,preuve.certificat(),preuve.sha256()),date));
        assertThrows(Exception.class,()->SignatureLocale.signer(pdf,magasin,"mauvais".toCharArray(),date));
    }
}
