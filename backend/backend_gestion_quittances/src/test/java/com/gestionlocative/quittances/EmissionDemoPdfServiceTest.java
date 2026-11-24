package com.gestionlocative.quittances;

import com.gestionlocative.quittances.dto.AffectationQuittanceResponse;
import com.gestionlocative.quittances.dto.DonneesApercu;
import com.gestionlocative.quittances.dto.QuittanceConsultationResponse;
import com.gestionlocative.quittances.pdf.EmissionDemoPdfService;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmissionDemoPdfServiceTest {

    private final EmissionDemoPdfService service =
            new EmissionDemoPdfService();

    private static final String NUMERO = "DEMO-QT-2026-000001";

    private static final Instant DATE =
            Instant.parse("2026-11-24T14:00:00Z");

    @Test
    void doitConserverLesMentionsDeDemonstration() throws Exception {
        byte[] contenu = service.generer(
                new DonneesApercu(document(false), List.of(
                        affectation("A1", "LOYER", "3000.00"),
                        affectation("A2", "CHARGES", "200.00"))),
                NUMERO, DATE);

        try (var pdf = Loader.loadPDF(contenu)) {
            String texte = new PDFTextStripper().getText(pdf)
                    .replaceAll("\\s+", " ");

            assertTrue(texte.contains("QUITTANCE — DÉMONSTRATION"));
            assertTrue(texte.contains("DÉMONSTRATION — CERTIFICAT DE TEST"));
            assertTrue(texte.contains(NUMERO));
            assertTrue(texte.contains("3200,00"));
            assertTrue(texte.contains("SHA256withRSA"));
            assertTrue(texte.contains(
                    "ne constitue pas une signature qualifiée"));

            assertFalse(texte.contains("BROUILLON — NON SIGNÉ"));
            assertFalse(texte.contains("ÉMISSION ÉLECTRONIQUE"));
        }
    }

    @Test
    void doitIdentifierUnRecuPartielDeDemonstration()
            throws Exception {
        byte[] contenu = service.generer(
                new DonneesApercu(document(true), List.of(
                        affectation("A1", "LOYER", "1500.00"))),
                NUMERO, DATE);

        try (var pdf = Loader.loadPDF(contenu)) {
            String texte = new PDFTextStripper().getText(pdf);

            assertTrue(texte.contains("REÇU PARTIEL — DÉMONSTRATION"));
            assertTrue(texte.contains("Paiement partiel"));
            assertTrue(texte.contains("1500,00"));
            assertTrue(texte.contains("1700,00"));
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