package com.gestionlocative.quittances;

import com.gestionlocative.quittances.document.PdfStocke;
import com.gestionlocative.quittances.document.StockagePdfPrive;
import com.gestionlocative.quittances.dto.ArchiveBrouillonResponse;
import com.gestionlocative.quittances.dto.DonneesApercu;
import com.gestionlocative.quittances.pdf.ApercuPdfService;
import com.gestionlocative.quittances.service.ArchivageBrouillonService;
import com.gestionlocative.quittances.service.ArchiveBrouillonTransactions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ArchivageBrouillonServiceTest {

    private ArchiveBrouillonTransactions transactions;
    private ApercuPdfService pdfService;
    private StockagePdfPrive stockage;
    private ArchivageBrouillonService service;
    private Authentication authentication;

    @BeforeEach
    void preparer() {
        transactions = mock(ArchiveBrouillonTransactions.class);
        pdfService = mock(ApercuPdfService.class);
        stockage = mock(StockagePdfPrive.class);
        authentication = mock(Authentication.class);

        service = new ArchivageBrouillonService(
                transactions,
                pdfService,
                stockage
        );
    }

    @Test
    void doitControlerPuisGenererStockerEtRattacher() throws Exception {
        UUID id = UUID.randomUUID();
        DonneesApercu donnees = mock(DonneesApercu.class);
        byte[] pdf = new byte[]{1, 2, 3};

        var fichier = new PdfStocke(
                UUID.randomUUID().toString(),
                "a".repeat(64),
                pdf.length
        );

        var reponse = new ArchiveBrouillonResponse(
                UUID.randomUUID(),
                id,
                2L,
                "BROUILLON",
                fichier.sha256(),
                fichier.tailleOctets(),
                Instant.now()
        );

        when(transactions.charger(authentication, id))
                .thenReturn(new ArchiveBrouillonTransactions.Source(donnees, 2L));

        when(pdfService.generer(donnees)).thenReturn(pdf);
        when(stockage.enregistrer(pdf)).thenReturn(fichier);

        when(transactions.enregistrer(authentication, id, 2L, fichier))
                .thenReturn(reponse);

        assertSame(reponse, service.archiver(authentication, id));

        var ordre = inOrder(transactions, pdfService, stockage);

        ordre.verify(transactions).charger(authentication, id);
        ordre.verify(pdfService).generer(donnees);
        ordre.verify(stockage).enregistrer(pdf);
        ordre.verify(transactions).enregistrer(
                authentication,
                id,
                2L,
                fichier
        );
    }

    @Test
    void accesRefuseNeDoitPasGenererNiStocker() {
        UUID id = UUID.randomUUID();

        when(transactions.charger(authentication, id))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThrows(
                ResponseStatusException.class,
                () -> service.archiver(authentication, id)
        );

        verifyNoInteractions(pdfService, stockage);
    }

    @Test
    void telechargementDoitUtiliserEmpreinteIssueDeLaBase() {
        UUID id = UUID.randomUUID();
        UUID archiveId = UUID.randomUUID();
        String reference = UUID.randomUUID().toString();
        String empreinte = "b".repeat(64);
        byte[] attendu = new byte[]{4, 5, 6};

        when(transactions.consulterFichier(authentication, id, archiveId))
                .thenReturn(new ArchiveBrouillonTransactions.Fichier(
                        reference,
                        empreinte
                ));

        when(stockage.lire(reference, empreinte)).thenReturn(attendu);

        assertArrayEquals(
                attendu,
                service.telecharger(authentication, id, archiveId)
        );

        var ordre = inOrder(transactions, stockage);

        ordre.verify(transactions).consulterFichier(
                authentication,
                id,
                archiveId
        );
        ordre.verify(stockage).lire(reference, empreinte);

        verifyNoInteractions(pdfService);
    }

    @Test
    void rattachementRefuseNeDoitPasRetournerUnSucces() throws Exception {
        UUID id = UUID.randomUUID();
        DonneesApercu donnees = mock(DonneesApercu.class);
        byte[] pdf = new byte[]{1};

        var fichier = new PdfStocke(
                UUID.randomUUID().toString(),
                "c".repeat(64),
                1
        );

        when(transactions.charger(authentication, id))
                .thenReturn(new ArchiveBrouillonTransactions.Source(donnees, 0L));

        when(pdfService.generer(donnees)).thenReturn(pdf);
        when(stockage.enregistrer(pdf)).thenReturn(fichier);

        when(transactions.enregistrer(authentication, id, 0L, fichier))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT));

        assertThrows(
                ResponseStatusException.class,
                () -> service.archiver(authentication, id)
        );
    }
}