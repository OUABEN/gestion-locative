package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.document.StockagePdfPrive;
import com.gestionlocative.quittances.dto.ArchiveBrouillonResponse;
import com.gestionlocative.quittances.pdf.ApercuPdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

@Service
public class ArchivageBrouillonService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ArchivageBrouillonService.class);

    private final ArchiveBrouillonTransactions transactions;
    private final ApercuPdfService pdfService;
    private final StockagePdfPrive stockage;

    public ArchivageBrouillonService(
            ArchiveBrouillonTransactions transactions,
            ApercuPdfService pdfService,
            StockagePdfPrive stockage
    ) {
        this.transactions = transactions;
        this.pdfService = pdfService;
        this.stockage = stockage;
    }

    @Transactional(propagation = Propagation.NEVER)
    public ArchiveBrouillonResponse archiver(
            Authentication authentication,
            UUID quittanceId
    ) throws IOException {
        var source = transactions.charger(authentication, quittanceId);

        byte[] contenu = pdfService.generer(source.donnees());
        var fichier = stockage.enregistrer(contenu);

        try {
            return transactions.enregistrer(
                    authentication,
                    quittanceId,
                    source.version(),
                    fichier
            );

        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Rattachement du fichier à vérifier après un échec : {}",
                    fichier.reference()
            );

            throw exception;
        }
    }

    @Transactional(propagation = Propagation.NEVER)
    public byte[] telecharger(
            Authentication authentication,
            UUID quittanceId,
            UUID archiveId
    ) {
        var fichier = transactions.consulterFichier(
                authentication,
                quittanceId,
                archiveId
        );

        return stockage.lire(
                fichier.reference(),
                fichier.sha256()
        );
    }
}