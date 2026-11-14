package com.gestionlocative.quittances.suivi;

import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class TableauBordService {
    private final JdbcTemplate jdbc;
    public TableauBordService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Groupe(String statut, String typeDocument, long nombre) {}
    public record Tableau(long totalDocuments, List<Groupe> groupes, Instant consulteLe) {}

    @Transactional(readOnly = true)
    public Tableau consulter(Authentication authentication) {
        var lecteur = LecteurQuittances.depuis(authentication);
        // Fragments constants : l'identifiant utilisateur reste un paramètre SQL.
        String acces = lecteur.bailleur() ? "bailleur_id = ?"
                : "locataire_id = ? AND emis_le IS NOT NULL";
        List<Groupe> groupes = jdbc.query("SELECT statut, type_document, COUNT(*) AS nombre "
                + "FROM quittance WHERE " + acces + " GROUP BY statut, type_document ORDER BY statut, type_document",
                (rs, i) -> new Groupe(rs.getString("statut"), rs.getString("type_document"), rs.getLong("nombre")),
                lecteur.identifiant());
        return new Tableau(groupes.stream().mapToLong(Groupe::nombre).sum(), List.copyOf(groupes), Instant.now());
    }
}
