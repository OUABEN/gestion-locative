package com.gestionlocative.quittances.pdf;

import com.gestionlocative.quittances.dto.DonneesApercu;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ApercuPdfService {

    private final GenerateurPdfCommun generateur =
            new GenerateurPdfCommun();

    public byte[] generer(DonneesApercu donnees) throws IOException {
        return generateur.generer(
                donnees,
                donnees.document().id().toString(),
                null,
                GenerateurPdfCommun.Mode.APERCU);
    }
}