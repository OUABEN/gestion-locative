package com.gestionlocative.quittances.suivi;

import com.gestionlocative.quittances.dto.QuittanceConsultationResponse;
import com.gestionlocative.quittances.service.ConsultationQuittanceService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class ExportCsvService {
    private final ConsultationQuittanceService consultation;
    public ExportCsvService(ConsultationQuittanceService consultation) { this.consultation = consultation; }
    // Toutes les pages voient le même état de la base pendant cet export.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, timeout = 30)
    public byte[] exporter(Authentication auth, String tri, String sens, String bailId, String numero,
                           String statut, LocalDate debut, LocalDate fin) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(ligne("Identifiant", "Numero", "Bail", "Locataire", "Bien", "Debut", "Fin", "Type", "Statut",
                "Loyer du", "Charges dues", "Loyer paye", "Charges payees", "Total paye", "Devise", "Cree le", "Emis le", "Document precedent"));
        int page = 0;
        while (true) {
            var resultat = consultation.lister(auth, page, 100, tri, sens, bailId, numero, statut, debut, fin);
            if (resultat.totalElements() > 5000) throw new ResponseStatusException(HttpStatus.valueOf(422),
                    "L'export est limité à 5 000 documents. Précisez les filtres.");
            for (var d : resultat.contenu()) csv.append(document(d));
            if (++page >= resultat.totalPages()) break;
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    private String document(QuittanceConsultationResponse d) {
        return ligne(d.id(), d.numero(), d.bailId(), d.locataireId(), d.bienId(), d.periodeDebut(), d.periodeFin(),
                d.typeDocument(), d.statut(), d.loyerDu(), d.chargesDues(), d.loyerPaye(), d.chargesPayees(),
                d.totalPaye(), d.devise(), d.creeLe(), d.emisLe(), d.documentPrecedentId());
    }
    public static String ligne(Object... valeurs) {
        return Arrays.stream(valeurs).map(ExportCsvService::cellule).collect(Collectors.joining(";")) + "\r\n";
    }
    public static String cellule(Object valeur) {
        String texte = valeur == null ? "" : valeur.toString();
        String debut = texte.stripLeading();
        // Neutralisation des formules dans les champs provenant des sources externes.
        if ((!debut.isEmpty() && "=+-@".indexOf(debut.charAt(0)) >= 0)
                || texte.startsWith("\t") || texte.startsWith("\r") || texte.startsWith("\n")) texte = "'" + texte;
        return "\"" + texte.replace("\"", "\"\"") + "\"";
    }
}
