package com.gestionlocative.quittances;

import com.gestionlocative.quittances.document.StockagePdfPrive;
import com.gestionlocative.quittances.exception.StockagePdfException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockagePdfPriveTest {

    @TempDir
    Path repertoire;

    private StockagePdfPrive stockage;

    @BeforeEach
    void preparer() throws Exception {
        stockage = new StockagePdfPrive(
                repertoire.toString(),
                1048576
        );
    }

    @Test
    void doitConserverEtRelireExactementLePdf() throws Exception {
        byte[] original = creerPdf();

        var resultat = stockage.enregistrer(original);

        String empreinteAttendue = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(original)
        );

        assertEquals(empreinteAttendue, resultat.sha256());
        assertEquals(original.length, resultat.tailleOctets());

        assertArrayEquals(
                original,
                stockage.lire(resultat.reference(), resultat.sha256())
        );

        Path dossier = repertoire.resolve(resultat.reference());

        assertTrue(Files.isRegularFile(dossier.resolve("document.pdf")));
        assertFalse(Files.exists(dossier.resolve("document.part")));
    }

    @Test
    void doitConserverLesFichiersApresNouvelleInstance() throws Exception {
        byte[] original = creerPdf();
        var resultat = stockage.enregistrer(original);

        StockagePdfPrive nouvelleInstance = new StockagePdfPrive(
                repertoire.toString(),
                1048576
        );

        assertArrayEquals(
                original,
                nouvelleInstance.lire(
                        resultat.reference(),
                        resultat.sha256()
                )
        );
    }

    @Test
    void doitAttribuerDeuxReferencesSansEcrasement() throws Exception {
        byte[] original = creerPdf();

        var premier = stockage.enregistrer(original);
        var second = stockage.enregistrer(original);

        assertNotEquals(premier.reference(), second.reference());
        assertEquals(premier.sha256(), second.sha256());

        assertArrayEquals(
                original,
                stockage.lire(premier.reference(), premier.sha256())
        );

        assertArrayEquals(
                original,
                stockage.lire(second.reference(), second.sha256())
        );
    }

    @Test
    void doitDetecterUnFichierModifie() throws Exception {
        var resultat = stockage.enregistrer(creerPdf());

        Path fichier = repertoire
                .resolve(resultat.reference())
                .resolve("document.pdf");

        Files.writeString(
                fichier,
                "%PDF-contenu-modifie",
                StandardCharsets.UTF_8
        );

        StockagePdfException exception = assertThrows(
                StockagePdfException.class,
                () -> stockage.lire(
                        resultat.reference(),
                        resultat.sha256()
                )
        );

        assertEquals("INTEGRITE_PDF_INVALIDE", exception.getCode());
    }

    @Test
    void doitSignalerUnFichierManquant() throws Exception {
        var resultat = stockage.enregistrer(creerPdf());

        Files.delete(
                repertoire.resolve(resultat.reference())
                        .resolve("document.pdf")
        );

        StockagePdfException exception = assertThrows(
                StockagePdfException.class,
                () -> stockage.lire(
                        resultat.reference(),
                        resultat.sha256()
                )
        );

        assertEquals("PDF_ABSENT", exception.getCode());
    }

    @Test
    void doitRefuserLesReferencesRepresentantDesChemins() {
        List<String> references = List.of(
                "../secret.txt",
                "..\\secret.txt",
                "/etc/passwd",
                "C:\\Windows\\win.ini",
                UUID.randomUUID() + "/document.pdf"
        );

        for (String reference : references) {
            StockagePdfException exception = assertThrows(
                    StockagePdfException.class,
                    () -> stockage.lire(reference, "0".repeat(64))
            );

            assertEquals("REFERENCE_PDF_INVALIDE", exception.getCode());
        }
    }

    @Test
    void doitRefuserUnContenuNonPdf() {
        StockagePdfException exception = assertThrows(
                StockagePdfException.class,
                () -> stockage.enregistrer(
                        "Ceci est un texte".getBytes(StandardCharsets.UTF_8)
                )
        );

        assertEquals("CONTENU_PDF_INVALIDE", exception.getCode());
    }

    @Test
    void doitRefuserUnFichierTropVolumineux() throws Exception {
        StockagePdfPrive stockageLimite = new StockagePdfPrive(
                repertoire.toString(),
                1024
        );

        byte[] tropGrand = new byte[1025];

        StockagePdfException exception = assertThrows(
                StockagePdfException.class,
                () -> stockageLimite.enregistrer(tropGrand)
        );

        assertEquals("PDF_TROP_VOLUMINEUX", exception.getCode());
    }

    private byte[] creerPdf() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {

            document.addPage(new PDPage());
            document.save(sortie);

            return sortie.toByteArray();
        }
    }
}