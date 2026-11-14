package com.gestionlocative.quittances;

import com.gestionlocative.quittances.dto.AffectationQuittanceResponse;
import com.gestionlocative.quittances.dto.DonneesApercu;
import com.gestionlocative.quittances.dto.QuittanceConsultationResponse;
import com.gestionlocative.quittances.pdf.ApercuPdfService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApercuPdfServiceTest {

    private final ApercuPdfService service = new ApercuPdfService();

    @Test
    void doitGenererPdfA4AvecAccentsEtMontants() throws Exception {
        byte[] contenu = service.generer(new DonneesApercu(
                document(false),
                List.of(
                        affectation("A1", "LOYER", "3000.00"),
                        affectation("A2", "CHARGES", "200.00")
                )
        ));

        try (PDDocument pdf = Loader.loadPDF(contenu)) {
            String texte = new PDFTextStripper().getText(pdf);

            assertTrue(texte.contains("PROJET DE QUITTANCE"));
            assertTrue(texte.contains("BROUILLON — NON SIGNÉ"));
            assertTrue(texte.contains("Éléonore"));
            assertTrue(texte.contains("3200,00"));
            assertTrue(texte.contains("01/10/2026"));
            assertTrue(pdf.getSignatureDictionaries().isEmpty());

            assertEquals(
                    PDRectangle.A4.getWidth(),
                    pdf.getPage(0).getMediaBox().getWidth(),
                    0.1f
            );

            assertEquals(
                    PDRectangle.A4.getHeight(),
                    pdf.getPage(0).getMediaBox().getHeight(),
                    0.1f
            );
        }
    }

    @Test
    void doitIdentifierUnRecuPartiel() throws Exception {
        byte[] contenu = service.generer(new DonneesApercu(
                document(true),
                List.of(affectation("A1", "LOYER", "1500.00"))
        ));

        try (PDDocument pdf = Loader.loadPDF(contenu)) {
            String texte = new PDFTextStripper().getText(pdf);

            assertTrue(texte.contains("PROJET DE REÇU PARTIEL"));
            assertTrue(texte.contains("Paiement partiel"));
            assertTrue(texte.contains("1500,00"));
            assertTrue(texte.contains("1700,00"));
        }
    }

    @Test
    void doitContinuerSurPlusieursPagesSansPerdreLaDerniereAffectation()
            throws Exception {
        List<AffectationQuittanceResponse> affectations = new ArrayList<>();

        for (int index = 1; index <= 100; index++) {
            affectations.add(
                    affectation("LOYER-" + index, "LOYER", "30.00")
            );
        }

        affectations.add(
                affectation("DERNIERE-AFFECTATION", "CHARGES", "200.00")
        );

        byte[] contenu = service.generer(
                new DonneesApercu(document(false), affectations)
        );

        try (PDDocument pdf = Loader.loadPDF(contenu)) {
            assertTrue(pdf.getNumberOfPages() > 1);

            String texte = new PDFTextStripper().getText(pdf);
            assertTrue(texte.contains("DERNIERE-AFFECTATION"));

            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                PDFTextStripper lecteur = new PDFTextStripper();
                lecteur.setStartPage(page);
                lecteur.setEndPage(page);

                assertTrue(
                        lecteur.getText(pdf)
                                .contains("BROUILLON — NON SIGNÉ")
                );
            }
        }
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