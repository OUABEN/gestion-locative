package com.gestionlocative.quittances.pdf;

import com.gestionlocative.quittances.dto.DonneesApercu;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
public class EmissionReellePdfService {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generer(DonneesApercu donnees, String numero, java.time.Instant dateEmission) throws IOException {
        var document = donnees.document();

        try (PDDocument pdf = new PDDocument();
             InputStream police = ouvrirPolice();
             ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {

            PDType0Font font = PDType0Font.load(pdf, police, true);

            pdf.getDocumentInformation().setTitle(
                    "Quittance locative électronique"
            );
            pdf.getDocumentInformation().setCreator("service-quittances");

            try (MiseEnPage page = new MiseEnPage(
                    pdf,
                    font,
                    numero
            )) {
                String titre = switch (document.typeDocument()) {
                    case "QUITTANCE_INTEGRALE" -> "QUITTANCE";
                    case "RECU_PARTIEL" -> "REÇU PARTIEL";
                    default -> throw new IOException(
                            "Type documentaire non pris en charge."
                    );
                };

                page.paragraphe(titre, 19);
                page.paragraphe(
                        "Émission électronique avec preuve détachée.",
                        10
                );
                page.espace(10);

                page.paragraphe(
                        "Référence : " + numero,
                        10
                );
                page.paragraphe("Émis le : " + dateEmission.toString() + " (UTC)", 10);
                page.paragraphe("Bail : " + requis(document.bailId()), 10);

                page.paragraphe(
                        "Période : du " + DATE.format(document.periodeDebut())
                                + " au " + DATE.format(document.periodeFin()),
                        11
                );

                page.section("PARTIES ET BIEN");

                page.paragraphe(
                        "Bailleur : " + requis(document.nomBailleur()),
                        11
                );

                String prenom = document.prenomLocataire() == null
                        ? ""
                        : document.prenomLocataire().trim();

                page.paragraphe(
                        "Locataire : "
                                + (prenom.isEmpty() ? "" : prenom + " ")
                                + requis(document.nomLocataire()),
                        11
                );

                page.paragraphe(
                        "Bien : " + requis(document.adresseBien()),
                        11
                );

                page.section("MONTANTS EN MAD");
                page.enteteMontants();

                page.montants(
                        "Loyer",
                        document.loyerDu(),
                        document.loyerPaye()
                );

                page.montants(
                        "Charges",
                        document.chargesDues(),
                        document.chargesPayees()
                );

                BigDecimal totalDu =
                        document.loyerDu().add(document.chargesDues());

                page.montants(
                        "Total",
                        totalDu,
                        document.totalPaye()
                );

                page.espace(8);
                page.paragraphe(
                        "Reste à régler sur la période : "
                                + montant(totalDu.subtract(document.totalPaye()))
                                + " MAD",
                        11
                );

                if ("RECU_PARTIEL".equals(document.typeDocument())) {
                    page.paragraphe(
                            "Paiement partiel : ce reçu ne constate pas "
                                    + "le règlement intégral de la période.",
                            10
                    );
                }

                page.section("AFFECTATIONS CONSERVÉES");

                if (donnees.affectations().isEmpty()) {
                    throw new IOException(
                            "Aucune affectation conservée pour ce document."
                    );
                }

                for (var affectation : donnees.affectations()) {
                    page.paragraphe(
                            "Paiement " + requis(affectation.paiementId())
                                    + " | " + requis(affectation.nature())
                                    + " | " + montant(affectation.montant())
                                    + " " + requis(affectation.devise()),
                            10
                    );

                    page.paragraphe(
                            "Affectation : "
                                    + requis(affectation.affectationId()),
                            9
                    );

                    page.espace(4);
                }

                page.paragraphe(
                        "Ces références décrivent les affectations "
                                + "enregistrées lors de la préparation.",
                        9
                );

                page.section("VALIDATION ET SIGNATURE");

                page.paragraphe(
                        "Signature détachée SHA256withRSA : preuve disponible séparément.",
                        11
                );

                page.paragraphe(
                        "Certificat configuré et contrôlé par empreinte. "
                                + "La valeur juridique nécessite une validation indépendante du certificat.",
                        10
                );
            }

            pdf.save(sortie);
            return sortie.toByteArray();

        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    "Une donnée ou un caractère ne peut pas être rendu.",
                    exception
            );
        }
    }

    private InputStream ouvrirPolice() throws IOException {
        InputStream police = EmissionReellePdfService.class.getResourceAsStream(
                "/fonts/NotoSans-Regular.ttf"
        );

        if (police == null) {
            throw new IOException("La police Noto Sans est absente.");
        }

        return police;
    }

    private static String requis(String valeur) throws IOException {
        if (valeur == null || valeur.isBlank()) {
            throw new IOException("Une donnée documentaire est absente.");
        }

        return valeur;
    }

    private static String montant(BigDecimal valeur) {
        return valeur.setScale(2, RoundingMode.UNNECESSARY)
                .toPlainString()
                .replace('.', ',');
    }

    private static final class MiseEnPage implements AutoCloseable {

        private static final float MARGE = 48;
        private static final float LARGEUR =
                PDRectangle.A4.getWidth() - 2 * MARGE;

        private static final Color BLEU = new Color(28, 49, 75);
        private static final Color GRIS = new Color(90, 98, 108);

        private final PDDocument document;
        private final PDType0Font police;
        private final String reference;

        private PDPageContentStream flux;
        private float y;
        private int numeroPage;

        private MiseEnPage(
                PDDocument document,
                PDType0Font police,
                String reference
        ) throws IOException {
            this.document = document;
            this.police = police;
            this.reference = reference;
            nouvellePage();
        }

        private void nouvellePage() throws IOException {
            if (flux != null) {
                flux.close();
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            flux = new PDPageContentStream(document, page);
            numeroPage++;

            float haut = PDRectangle.A4.getHeight();

            flux.setNonStrokingColor(BLEU);
            flux.addRect(MARGE, haut - 83, LARGEUR, 35);
            flux.fill();

            texte(
                    "ÉMISSION ÉLECTRONIQUE",
                    MARGE + 12,
                    haut - 70,
                    13,
                    Color.WHITE
            );

            texte(
                    "Document " + reference + " — Page " + numeroPage,
                    MARGE,
                    30,
                    8,
                    GRIS
            );

            y = haut - 110;
        }

        private void assurerPlace(float hauteur) throws IOException {
            if (y - hauteur < 60) {
                nouvellePage();
            }
        }

        private void espace(float hauteur) throws IOException {
            assurerPlace(hauteur);
            y -= hauteur;
        }

        private void section(String titre) throws IOException {
            assurerPlace(65);
            y -= 12;
            texte(titre, MARGE, y, 11, BLEU);
            y -= 9;

            flux.setStrokingColor(new Color(210, 216, 223));
            flux.setLineWidth(0.6f);
            flux.moveTo(MARGE, y);
            flux.lineTo(MARGE + LARGEUR, y);
            flux.stroke();

            y -= 20;
        }

        private void paragraphe(String valeur, float taille)
                throws IOException {
            String propre = valeur.replaceAll("\\s+", " ").trim();
            StringBuilder ligne = new StringBuilder();

            for (int position = 0; position < propre.length();) {
                int codePoint = propre.codePointAt(position);
                String caractere = new String(Character.toChars(codePoint));
                String candidate = ligne.toString() + caractere;

                if (largeur(candidate, taille) > LARGEUR
                        && !ligne.isEmpty()) {
                    ligne(ligne.toString().stripTrailing(), taille);
                    ligne.setLength(0);

                    if (!caractere.isBlank()) {
                        ligne.append(caractere);
                    }
                } else {
                    ligne.append(caractere);
                }

                position += Character.charCount(codePoint);
            }

            if (!ligne.isEmpty()) {
                ligne(ligne.toString(), taille);
            }

            y -= 4;
        }

        private void ligne(String contenu, float taille)
                throws IOException {
            assurerPlace(taille + 6);
            texte(contenu, MARGE, y, taille, BLEU);
            y -= taille + 6;
        }

        private void enteteMontants() throws IOException {
            assurerPlace(110);
            texte("Nature", MARGE, y, 10, GRIS);
            texteDroite("Dû", 365, y, 10);
            texteDroite("Payé", 547, y, 10);
            y -= 23;
        }

        private void montants(
                String libelle,
                BigDecimal du,
                BigDecimal paye
        ) throws IOException {
            assurerPlace(24);
            texte(libelle, MARGE, y, 11, BLEU);
            texteDroite(montant(du), 365, y, 11);
            texteDroite(montant(paye), 547, y, 11);
            y -= 24;
        }

        private float largeur(String valeur, float taille)
                throws IOException {
            return police.getStringWidth(valeur) / 1000 * taille;
        }

        private void texteDroite(
                String valeur,
                float droite,
                float positionY,
                float taille
        ) throws IOException {
            texte(
                    valeur,
                    droite - largeur(valeur, taille),
                    positionY,
                    taille,
                    BLEU
            );
        }

        private void texte(
                String valeur,
                float x,
                float positionY,
                float taille,
                Color couleur
        ) throws IOException {
            flux.beginText();
            flux.setFont(police, taille);
            flux.setNonStrokingColor(couleur);
            flux.newLineAtOffset(x, positionY);
            flux.showText(valeur);
            flux.endText();
        }

        @Override
        public void close() throws IOException {
            if (flux != null) {
                flux.close();
            }
        }
    }
}