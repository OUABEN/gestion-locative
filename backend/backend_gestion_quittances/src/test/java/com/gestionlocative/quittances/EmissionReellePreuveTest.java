package com.gestionlocative.quittances;

import com.gestionlocative.quittances.config.ReferentielsProperties;
import com.gestionlocative.quittances.document.SignataireReel;
import com.gestionlocative.quittances.document.SignatureLocale;
import com.gestionlocative.quittances.pdf.EmissionReellePdfService;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.service.ConsultationQuittanceService;
import com.gestionlocative.quittances.service.ControlePrealableEmissionService;
import com.gestionlocative.quittances.service.EmissionReelleService;
import com.gestionlocative.quittances.service.EmissionReelleTransactions;
import com.gestionlocative.quittances.service.PreparationApercuService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmissionReellePreuveTest {

    private final UUID id = UUID.randomUUID();

    private final Authentication auth =
            mock(Authentication.class);

    private final EmissionReelleTransactions transactions =
            mock(EmissionReelleTransactions.class);

    private final ConsultationQuittanceService consultation =
            mock(ConsultationQuittanceService.class);

    private final byte[] pdf =
            "%PDF-1.7 contenu de test"
                    .getBytes(StandardCharsets.UTF_8);

    private final byte[] signature = new byte[]{1, 2, 3};
    private final byte[] certificat = new byte[]{4, 5, 6};

    private String sha256;

    private final String numero = "QT-2026-000001";

    private EmissionReelleService service;

    @BeforeEach
    void preparer() throws GeneralSecurityException {
        sha256 = SignatureLocale.empreinte(pdf);
        service = new EmissionReelleService(
                mock(ControlePrealableEmissionService.class),
                mock(PreparationApercuService.class),
                mock(QuittanceRepository.class),
                mock(JdbcTemplate.class),
                mock(EmissionReellePdfService.class),
                mock(SignataireReel.class),
                transactions,
                consultation,
                mock(ReferentielsProperties.class));
    }

    private void documentDisponible() throws Exception {
        var preuve = new SignatureLocale.Preuve(
                signature, certificat, sha256);

        var fichier = new EmissionReelleTransactions.Fichier(
                pdf, preuve,
                Instant.parse("2026-11-24T14:00:00Z"),
                numero);

        when(transactions.consulter(auth, id, consultation))
                .thenReturn(fichier);
    }

    private void documentInvalide() throws Exception {
        when(transactions.consulter(auth, id, consultation))
                .thenThrow(new GeneralSecurityException(
                        "Preuve invalide"));
    }

    @Test
    void telechargeLePdfConserve() throws Exception {
        documentDisponible();

        assertArrayEquals(pdf,
                service.telechargerPdf(auth, id));

        verify(transactions).consulter(
                auth, id, consultation);
    }

    @Test
    void retourneUneVerificationNonQualifiee() throws Exception {
        documentDisponible();

        var verification = service.verifier(auth, id);

        assertEquals(numero, verification.numero());
        assertEquals(sha256, verification.sha256());
        assertTrue(
                verification.signatureCryptographiqueValide());
        assertFalse(verification.signatureQualifiee());
    }

    @Test
    void archiveContientLePdfEtToutesLesPreuves()
            throws Exception {
        documentDisponible();

        byte[] archive = service.telechargerPreuve(auth, id);

        Map<String, byte[]> fichiers = new HashMap<>();

        try (var zip = new ZipInputStream(
                new ByteArrayInputStream(archive))) {

            java.util.zip.ZipEntry entree;

            while ((entree = zip.getNextEntry()) != null) {
                fichiers.put(
                        entree.getName(), zip.readAllBytes());
                zip.closeEntry();
            }
        }

        assertEquals(Set.of(
                "document.pdf",
                "document.sig",
                "certificat.cer",
                "LIRE-MOI.txt"), fichiers.keySet());

        assertArrayEquals(pdf, fichiers.get("document.pdf"));
        assertArrayEquals(signature, fichiers.get("document.sig"));
        assertArrayEquals(certificat, fichiers.get("certificat.cer"));

        String notice = new String(
                fichiers.get("LIRE-MOI.txt"),
                StandardCharsets.UTF_8);

        assertTrue(notice.contains(sha256));
        assertTrue(notice.contains("RSA-SHA256"));
        assertTrue(notice.contains(
                "Validité cryptographique vérifiée"));
    }

    @Test
    void refuseTelechargementPdfAvecPreuveInvalide()
            throws Exception {
        documentInvalide();

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        () -> service.telechargerPdf(auth, id))
                        .getStatusCode());
    }

    @Test
    void refuseVerificationAvecPreuveInvalide()
            throws Exception {
        documentInvalide();

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        () -> service.verifier(auth, id))
                        .getStatusCode());
    }

    @Test
    void refuseArchiveAvecPreuveInvalide()
            throws Exception {
        documentInvalide();

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        () -> service.telechargerPreuve(auth, id))
                        .getStatusCode());
    }
}