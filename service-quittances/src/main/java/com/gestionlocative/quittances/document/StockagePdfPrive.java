package com.gestionlocative.quittances.document;

import com.gestionlocative.quittances.exception.StockagePdfException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class StockagePdfPrive {

    private final Path racine;
    private final long tailleMax;

    public StockagePdfPrive(
            @Value("${quittances.stockage.repertoire}") String repertoire,
            @Value("${quittances.stockage.taille-max-octets:20971520}")
            long tailleMax
    ) throws IOException {
        if (repertoire == null || repertoire.isBlank()) {
            throw new IllegalArgumentException(
                    "Le répertoire de stockage est obligatoire."
            );
        }

        if (tailleMax < 1024 || tailleMax > 104857600) {
            throw new IllegalArgumentException(
                    "La limite doit être comprise entre 1 Kio et 100 Mio."
            );
        }

        this.tailleMax = tailleMax;

        Path chemin = Path.of(repertoire)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(chemin);

        if (Files.isSymbolicLink(chemin)
                || !Files.isDirectory(chemin, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "Le répertoire de stockage doit être un dossier réel."
            );
        }

        this.racine = chemin.toRealPath();
        restreindrePermissionsPosix(racine);
    }

    public PdfStocke enregistrer(byte[] contenu) {
        verifierContenu(contenu);

        byte[] copie = contenu.clone();
        String empreinte = calculerSha256(copie);

        Path dossier = null;

        try {
            String reference = UUID.randomUUID().toString();
            Path candidat = racine.resolve(reference);

            Files.createDirectory(candidat);
            dossier = candidat;

            restreindrePermissionsPosix(dossier);

            Path temporaire = dossier.resolve("document.part");
            Path destination = dossier.resolve("document.pdf");

            try (FileChannel canal = FileChannel.open(
                    temporaire,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS
            )) {
                ByteBuffer tampon = ByteBuffer.wrap(copie);

                while (tampon.hasRemaining()) {
                    canal.write(tampon);
                }

                canal.force(true);
            }

            Files.move(
                    temporaire,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE
            );

            return new PdfStocke(
                    reference,
                    empreinte,
                    copie.length
            );

        } catch (IOException exception) {
            nettoyerEcritureIncomplete(dossier, exception);

            throw new StockagePdfException(
                    "ECRITURE_PDF_IMPOSSIBLE",
                    "Le PDF n'a pas pu être conservé.",
                    exception
            );
        }
    }

    public byte[] lire(
            String reference,
            String empreinteAttendue
    ) {
        Path dossier = resoudreReference(reference);
        verifierEmpreinteAttendue(empreinteAttendue);

        try {
            BasicFileAttributes attributsDossier = Files.readAttributes(
                    dossier,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS
            );

            if (!attributsDossier.isDirectory()
                    || attributsDossier.isSymbolicLink()) {
                throw new StockagePdfException(
                        "REFERENCE_PDF_INVALIDE",
                        "La référence ne désigne pas un dossier autorisé."
                );
            }

            Path fichier = dossier.resolve("document.pdf");

            BasicFileAttributes attributsFichier = Files.readAttributes(
                    fichier,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS
            );

            if (!attributsFichier.isRegularFile()
                    || attributsFichier.isSymbolicLink()) {
                throw new StockagePdfException(
                        "REFERENCE_PDF_INVALIDE",
                        "La référence ne désigne pas un fichier autorisé."
                );
            }

            byte[] contenu = lireAvecLimite(fichier);

            byte[] attendue = HexFormat.of().parseHex(empreinteAttendue);
            byte[] obtenue = HexFormat.of().parseHex(
                    calculerSha256(contenu)
            );

            if (!MessageDigest.isEqual(attendue, obtenue)) {
                throw new StockagePdfException(
                        "INTEGRITE_PDF_INVALIDE",
                        "Le contenu du PDF ne correspond plus à son empreinte."
                );
            }

            return contenu;

        } catch (NoSuchFileException exception) {
            throw new StockagePdfException(
                    "PDF_ABSENT",
                    "Le fichier PDF est absent du stockage.",
                    exception
            );

        } catch (IOException exception) {
            throw new StockagePdfException(
                    "LECTURE_PDF_IMPOSSIBLE",
                    "Le fichier PDF ne peut pas être lu.",
                    exception
            );
        }
    }

    private byte[] lireAvecLimite(Path fichier) throws IOException {
        try (FileChannel canal = FileChannel.open(
                fichier,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS
        );
             ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {

            if (canal.size() > tailleMax) {
                throw tailleDepassee();
            }

            ByteBuffer tampon = ByteBuffer.allocate(8192);
            long total = 0;
            int lus;

            while ((lus = canal.read(tampon)) != -1) {
                if (lus == 0) {
                    continue;
                }

                total += lus;

                if (total > tailleMax) {
                    throw tailleDepassee();
                }

                sortie.write(tampon.array(), 0, lus);
                tampon.clear();
            }

            return sortie.toByteArray();
        }
    }

    private Path resoudreReference(String reference) {
        if (reference == null) {
            throw referenceInvalide();
        }

        try {
            UUID identifiant = UUID.fromString(reference);

            if (!identifiant.toString().equals(reference)) {
                throw referenceInvalide();
            }

        } catch (IllegalArgumentException exception) {
            throw referenceInvalide();
        }

        Path dossier = racine.resolve(reference).normalize();

        if (!racine.equals(dossier.getParent())) {
            throw referenceInvalide();
        }

        return dossier;
    }

    private void verifierContenu(byte[] contenu) {
        if (contenu == null || contenu.length < 5) {
            throw new StockagePdfException(
                    "CONTENU_PDF_INVALIDE",
                    "Le contenu PDF est absent ou invalide."
            );
        }

        if (contenu.length > tailleMax) {
            throw tailleDepassee();
        }

        if (contenu[0] != '%'
                || contenu[1] != 'P'
                || contenu[2] != 'D'
                || contenu[3] != 'F'
                || contenu[4] != '-') {
            throw new StockagePdfException(
                    "CONTENU_PDF_INVALIDE",
                    "Le contenu ne possède pas l'en-tête PDF attendu."
            );
        }
    }

    private void verifierEmpreinteAttendue(String empreinte) {
        if (empreinte == null
                || !empreinte.matches("[0-9a-f]{64}")) {
            throw new StockagePdfException(
                    "EMPREINTE_PDF_INVALIDE",
                    "L'empreinte attendue doit être un SHA-256 hexadécimal."
            );
        }
    }

    private static String calculerSha256(byte[] contenu) {
        try {
            MessageDigest calculateur =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    calculateur.digest(contenu)
            );

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 est indisponible dans cet environnement Java.",
                    exception
            );
        }
    }

    private void restreindrePermissionsPosix(Path dossier)
            throws IOException {
        if (Files.getFileStore(dossier)
                .supportsFileAttributeView(PosixFileAttributeView.class)) {
            Files.setPosixFilePermissions(
                    dossier,
                    PosixFilePermissions.fromString("rwx------")
            );
        }
    }

    private void nettoyerEcritureIncomplete(
            Path dossier,
            IOException erreurInitiale
    ) {
        if (dossier == null) {
            return;
        }

        try {
            Files.deleteIfExists(dossier.resolve("document.part"));
            Files.deleteIfExists(dossier.resolve("document.pdf"));
            Files.deleteIfExists(dossier);

        } catch (IOException erreurNettoyage) {
            erreurInitiale.addSuppressed(erreurNettoyage);
        }
    }

    private StockagePdfException referenceInvalide() {
        return new StockagePdfException(
                "REFERENCE_PDF_INVALIDE",
                "La référence documentaire est invalide."
        );
    }

    private StockagePdfException tailleDepassee() {
        return new StockagePdfException(
                "PDF_TROP_VOLUMINEUX",
                "Le PDF dépasse la taille maximale autorisée."
        );
    }
}