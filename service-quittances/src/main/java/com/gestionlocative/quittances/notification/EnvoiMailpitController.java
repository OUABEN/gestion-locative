package com.gestionlocative.quittances.notification;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quittances/{id}/envois-mailpit")
public class EnvoiMailpitController {
    private final EnvoiMailpitService service;
    private final EnvoiMailpitTransactions transactions;
    public EnvoiMailpitController(EnvoiMailpitService service,EnvoiMailpitTransactions transactions){this.service=service;this.transactions=transactions;}
    public record Demande(UUID requeteId,Boolean confirmation){}
    @PostMapping
    public org.springframework.http.ResponseEntity<EnvoiMailpitTransactions.Envoi> envoyer(Authentication auth,@PathVariable UUID id,@RequestBody Demande demande){
        var resultat=service.envoyer(auth,id,demande.requeteId(),demande.confirmation());
        int statut=switch(resultat.statut()){case "ENVOYE" -> 200;case "EN_ATTENTE_ENVOI" -> 202;default -> 503;};
        return org.springframework.http.ResponseEntity.status(statut).body(resultat);
    }
    @GetMapping
    public EnvoiMailpitTransactions.Page lister(Authentication auth,@PathVariable UUID id,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int taille){return transactions.lister(auth,id,page,taille);}
}
