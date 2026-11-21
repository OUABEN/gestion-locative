package com.gestionlocative.quittances.suivi;

import com.gestionlocative.quittances.client.PaiementClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReconciliationPaiementsService {
    private final ReferencePaiementsService references;
    private final PaiementClient paiements;
    public ReconciliationPaiementsService(ReferencePaiementsService references, PaiementClient paiements) {
        this.references = references; this.paiements = paiements;
    }
    public record Rapport(UUID documentId, String resultat, Instant verifieLe, List<ComparateurPaiements.Ecart> ecarts) {}
    @Transactional(propagation = Propagation.NEVER)
    public Rapport controler(Authentication auth, UUID id) {
        // Vérification des droits avant l'appel externe, sans transaction ouverte pendant cet appel.
        var reference = references.lire(auth, id);
        var d = reference.document();
        var actuelles = paiements.consulterAffectations(d.bailId(), d.periodeDebut(), d.periodeFin());
        var ecarts = ComparateurPaiements.comparer(d.bailId(), d.periodeDebut(), d.periodeFin(), reference.affectations(), actuelles);
        return new Rapport(id, ecarts.isEmpty() ? "CONFORME" : "ECARTS_DETECTES", Instant.now(), ecarts);
    }
}
