package com.gestionlocative.quittances.client;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import com.gestionlocative.quittances.dto.ReferentielsExternes.Bien;
import com.gestionlocative.quittances.dto.ReferentielsExternes.BailPeriode;
import org.springframework.web.client.RestClient;

/** Adaptateur de démonstration : propriété configurée, jamais fournie par l'API Biens. */
public final class BiensLocalClient implements BienBailClient {
    private final BienBailClient delegate;
    private final RestClient client;
    private final String bienId;
    public BiensLocalClient(BienBailClient delegate, RestClient client, String bienId) {
        UUID.fromString(bienId);
        this.delegate = delegate;
        this.client = client;
        this.bienId = bienId;
    }
    public record FicheBien(String id, String adresse, String ville) {}

    @Override public Bien consulterBien(String id) {
        LectureApiExterne.verifierIdentifiant(id);
        if (!bienId.equals(id)) throw LectureApiExterne.incoherence("BIENS");
        FicheBien fiche = LectureApiExterne.lire(client, "BIENS", "/api/biens/{id}",
                Map.of("id", id), FicheBien.class);
        if (!id.equals(fiche.id()) || !LectureApiExterne.textePresent(fiche.adresse())
                || !LectureApiExterne.textePresent(fiche.ville())) {
            throw LectureApiExterne.incoherence("BIENS");
        }
        // Version interne et association fictive explicite limitée au bien configuré.
        return new Bien("1", id, fiche.adresse() + ", " + fiche.ville(), "BAILLEUR-TEST");
    }
    @Override public BailPeriode consulterBail(String id, LocalDate debut, LocalDate fin) {
        return delegate.consulterBail(id, debut, fin);
    }
}
