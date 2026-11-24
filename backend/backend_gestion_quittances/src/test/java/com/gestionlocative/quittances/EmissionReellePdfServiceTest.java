package com.gestionlocative.quittances;

import com.gestionlocative.quittances.dto.AffectationQuittanceResponse;
import com.gestionlocative.quittances.dto.DonneesApercu;
import com.gestionlocative.quittances.dto.QuittanceConsultationResponse;
import com.gestionlocative.quittances.pdf.EmissionReellePdfService;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmissionReellePdfServiceTest {

    private final EmissionReellePdfService service =
            new EmissionReellePdfService();

    private static final String NUMERO = "QT-2026-000001";

    private static final Instant DATE =
            Instant.parse("2026-11-24T14:00:00Z");

    private byte[] genererQuittance() throws IOException {
        return service.generer(new DonneesApercu(
                document(false),
                List.of(
                        affectation("A1", "LOYER", "3000.00"),
                        affectation("A2", "CHARGES", "200.00")
                )
        ), NUMERO, DATE);
    }

    @Test
    void doitGenererUneQuittanceReelleAvecSesReferences()
            throws Exception {
        try (PDDocument pdf = Loader.loadPDF(genererQuittance())) {
            String texte = new PDFTextStripper().getText(pdf);

            assertTrue(texte.contains("QUITTANCE"));
            assertTrue(texte.contains("ÉMISSION ÉLECTRONIQUE"));
            assertTrue(texte.contains(NUMERO));
            assertTrue(texte.contains("2026-11-24T14:00:00Z"));
            assertTrue(texte.contains("BAIL-TEST"));
            assertTrue(texte.contains("01/10/2026"));
            assertTrue(texte.contains("31/10/2026"));
        }
    }

    @Test
    void doitConserverLesPartiesEtLesMontants()
            throws Exception {
        try (PDDocument pdf = Loader.loadPDF(genererQuittance())) {
            String texte = new PDFTextStripper().getText(pdf);

            assertTrue(texte.contains("Bailleur fictif"));
            assertTrue(texte.contains("Éléonore"));
            assertTrue(texte.contains("Résidence fictive, Agadir"));
            assertTrue(texte.contains("3000,00"));
            assertTrue(texte.contains("200,00"));
            assertTrue(texte.contains("3200,00"));
            assertTrue(texte.contains("0,00 MAD"));
            assertTrue(texte.contains("PAY-A1"));
            assertTrue(texte.contains("PAY-A2"));
        }
    }

    @Test
    void doitIdentifierUnRecuPartiel()
            throws Exception {
        byte[] contenu = service.generer(new DonneesApercu(
                document(true),
                List.of(affectation("A1", "LOYER", "1500.00"))
        ), NUMERO, DATE);

        try (PDDocument pdf = Loader.loadPDF(contenu)) {
            String texte = new PDFTextStripper().getText(pdf);

            assertTrue(texte.contains("REÇU PARTIEL"));
            assertTrue(texte.contains("Paiement partiel"));
            assertTrue(texte.contains("1500,00"));
            assertTrue(texte.contains("1700,00"));
        }
    }

    @Test
    void doitConserverLeFormatA4()
            throws Exception {
        try (PDDocument pdf = Loader.loadPDF(genererQuittance())) {
            assertEquals(PDRectangle.A4.getWidth(),
                    pdf.getPage(0).getMediaBox().getWidth(), 0.1f);

            assertEquals(PDRectangle.A4.getHeight(),
                    pdf.getPage(0).getMediaBox().getHeight(), 0.1f);
        }
    }

    @Test
    void doitIndiquerLaSignatureDetacheeSansConfondreAvecUnBrouillon()
            throws Exception {
        try (PDDocument pdf = Loader.loadPDF(genererQuittance())) {
            String texte = new PDFTextStripper().getText(pdf);

            assertTrue(texte.contains("SHA256withRSA"));
            assertFalse(texte.contains("BROUILLON — NON SIGNÉ"));
            assertFalse(texte.contains("PROJET DE QUITTANCE"));

            // La preuve est détachée et non incorporée au PDF.
            assertTrue(pdf.getSignatureDictionaries().isEmpty());
        }
    }

    @Test
    void doitConserverLesAffectationsSurPlusieursPages()
            throws Exception {
        List<AffectationQuittanceResponse> affectations =
                new ArrayList<>();

        for (int index = 1; index <= 100; index++) {
            affectations.add(
                    affectation("LOYER-" + index, "LOYER", "30.00"));
        }

        affectations.add(
                affectation("DERNIERE-AFFECTATION", "CHARGES", "200.00"));

        byte[] contenu = service.generer(
                new DonneesApercu(document(false), affectations),
                NUMERO, DATE);

        try (PDDocument pdf = Loader.loadPDF(contenu)) {
            assertTrue(pdf.getNumberOfPages() > 1);

            String texte = new PDFTextStripper().getText(pdf);
            assertTrue(texte.contains("DERNIERE-AFFECTATION"));

            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                PDFTextStripper lecteur = new PDFTextStripper();
                lecteur.setStartPage(page);
                lecteur.setEndPage(page);

                assertTrue(lecteur.getText(pdf)
                        .contains("ÉMISSION ÉLECTRONIQUE"));
            }
        }
    }

    @Test
    void doitRefuserUnDocumentSansAffectation() {
        assertThrows(IOException.class,
                () -> service.generer(
                        new DonneesApercu(document(false), List.of()),
                        NUMERO, DATE));
    }

    private QuittanceConsultationResponse document(boolean partiel) {
        return new QuittanceConsultationResponse(
                UUID.randomUUID(),
                null,
                "BAIL-TEST",
                "LOC-TEST",
                "BIEN-TEST",
                "Bailleur fictif",
                "Démonstration",
                "Éléonore",
                "Résidence fictive, Agadir",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                new BigDecimal("3000.00"),
                new BigDecimal("200.00"),
                new BigDecimal(partiel ? "1500.00" : "3000.00"),
                new BigDecimal(partiel ? "0.00" : "200.00"),
                new BigDecimal(partiel ? "1500.00" : "3200.00"),
                "MAD",
                partiel ? "RECU_PARTIEL" : "QUITTANCE_INTEGRALE",
                "BROUILLON",
                "NON_SIGNE",
                Instant.parse("2026-10-01T10:00:00Z"),
                null
        );
    }

    private AffectationQuittanceResponse affectation(
            String id,
            String nature,
            String montant
    ) {
        return new AffectationQuittanceResponse(
                id,
                "PAY-" + id,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                nature,
                new BigDecimal(montant),
                "MAD",
                "CONFIRME"
        );
    }
}