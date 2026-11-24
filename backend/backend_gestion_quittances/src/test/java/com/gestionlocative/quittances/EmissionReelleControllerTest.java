package com.gestionlocative.quittances;

import com.gestionlocative.quittances.controller.EmissionReelleController;
import com.gestionlocative.quittances.service.EmissionReelleService;
import com.gestionlocative.quittances.service.EmissionReelleTransactions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmissionReelleControllerTest {

    private final EmissionReelleService service =
            mock(EmissionReelleService.class);

    private final Authentication auth =
            mock(Authentication.class);

    private final EmissionReelleController controleur =
            new EmissionReelleController(service);

    private final UUID id = UUID.randomUUID();

    @Test
    void transmetLaConfirmationEmission() {
        var resultat = new EmissionReelleTransactions.Resultat(
                id, "QT-2026-000001", "EMIS",
                "SIGNE_VERIFIE", "SIGNATURE_DETACHEE",
                "a".repeat(64), Instant.now());

        when(service.emettre(auth, id, true))
                .thenReturn(resultat);

        assertSame(resultat, controleur.emettre(
                auth, id,
                new EmissionReelleController.Demande(true)));

        verify(service).emettre(auth, id, true);
    }

    @Test
    void transmetUneDemandeSansConfirmation() {
        controleur.emettre(auth, id, null);

        verify(service).emettre(auth, id, null);
    }

    @Test
    void transmetUnRefusDeConfirmation() {
        controleur.emettre(auth, id,
                new EmissionReelleController.Demande(false));

        verify(service).emettre(auth, id, false);
    }

    @Test
    void retourneLaVerificationDuService() {
        var resultat = new EmissionReelleTransactions.Verification(
                "QT-2026-000001", "a".repeat(64),
                true, false);

        when(service.verifier(auth, id))
                .thenReturn(resultat);

        assertSame(resultat, controleur.verifier(auth, id));

        verify(service).verifier(auth, id);
    }

    @Test
    void retourneLePdfAvecLesEntetesDeProtection() {
        byte[] contenu = new byte[]{1, 2, 3};

        when(service.telechargerPdf(auth, id))
                .thenReturn(contenu);

        var reponse = controleur.telechargerPdf(auth, id);

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF,
                reponse.getHeaders().getContentType());

        assertEquals(contenu.length,
                reponse.getHeaders().getContentLength());

        assertEquals("no-store",
                reponse.getHeaders().getCacheControl());

        assertEquals("attachment",
                reponse.getHeaders().getContentDisposition().getType());

        assertEquals("quittance-" + id + ".pdf",
                reponse.getHeaders().getContentDisposition().getFilename());

        assertArrayEquals(contenu, reponse.getBody());

        verify(service).telechargerPdf(auth, id);
    }

    @Test
    void retourneLaPreuveZipAvecLesEntetesDeProtection() {
        byte[] contenu = new byte[]{4, 5, 6};

        when(service.telechargerPreuve(auth, id))
                .thenReturn(contenu);

        var reponse = controleur.telechargerPreuve(auth, id);

        assertEquals(HttpStatus.OK, reponse.getStatusCode());

        assertEquals(MediaType.parseMediaType("application/zip"),
                reponse.getHeaders().getContentType());

        assertEquals(contenu.length,
                reponse.getHeaders().getContentLength());

        assertEquals("no-store",
                reponse.getHeaders().getCacheControl());

        assertEquals("attachment",
                reponse.getHeaders().getContentDisposition().getType());

        assertEquals("preuve-" + id + ".zip",
                reponse.getHeaders().getContentDisposition().getFilename());

        assertArrayEquals(contenu, reponse.getBody());

        verify(service).telechargerPreuve(auth, id);
    }
}