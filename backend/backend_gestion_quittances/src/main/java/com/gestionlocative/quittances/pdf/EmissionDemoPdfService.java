package com.gestionlocative.quittances.pdf;

import com.gestionlocative.quittances.dto.DonneesApercu;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;

@Service
public class EmissionDemoPdfService {

    private final GenerateurPdfCommun generateur =
            new GenerateurPdfCommun();

    public byte[] generer(
            DonneesApercu donnees,
            String numero,
            Instant dateEmission
    ) throws IOException {
        return generateur.generer(
                donnees,
                numero,
                dateEmission,
                GenerateurPdfCommun.Mode.DEMONSTRATION);
    }
}