package com.gestionlocative.quittances.notification;

import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;

@Service
public class EnvoiMailpitTransactions {
    private final QuittanceRepository documents;
    private final JdbcTemplate jdbc;
    public EnvoiMailpitTransactions(QuittanceRepository documents, JdbcTemplate jdbc) {
        this.documents=documents;this.jdbc=jdbc;
    }
    public record Envoi(UUID id, String destinataire, String statut, Instant creeLe, Instant termineLe, String erreurCode) {}
    public record Reservation(Envoi envoi, boolean nouveau) {}
    public record Page(List<Envoi> contenu, int page, int taille, long totalElements, long totalPages) {}
    private final org.springframework.jdbc.core.RowMapper<Envoi> mapper=(rs,n)->new Envoi(
        rs.getObject("id",UUID.class),rs.getString("destinataire"),rs.getString("statut"),
        rs.getTimestamp("cree_le").toInstant(),rs.getTimestamp("termine_le")==null?null:rs.getTimestamp("termine_le").toInstant(),rs.getString("erreur_code"));
    @Transactional(timeout=10)
    public Reservation reserver(Authentication auth, UUID document, UUID requete) {
        var q=proprietaire(auth,document);
        var existant=jdbc.query("SELECT * FROM quittance_envoi WHERE quittance_id=? AND requete_id=?",mapper,document,requete);
        if(!existant.isEmpty()) return new Reservation(existant.getFirst(),false);
        if (!"EMIS".equals(q.getStatut().name()) || q.getNumero()==null || !q.getNumero().startsWith("DEMO-QT-"))
            throw erreur(409,"Seul un document de démonstration émis et actif peut être envoyé.");
        Integer bloque=jdbc.queryForObject("SELECT count(*) FROM quittance_envoi WHERE quittance_id=? AND statut<>'ECHEC_ENVOI'",Integer.class,document);
        if(bloque!=null && bloque>0) throw erreur(409,"Un envoi existe déjà. Consultez son état avant toute nouvelle tentative.");
        Integer recent=jdbc.queryForObject("SELECT count(*) FROM quittance_envoi WHERE quittance_id=? AND cree_le > CURRENT_TIMESTAMP - INTERVAL '30 seconds'",Integer.class,document);
        if(recent!=null && recent>0) throw erreur(429,"Attendez 30 secondes avant une nouvelle tentative.");
        UUID id=UUID.randomUUID();Instant date=Instant.now();
        String adresse="locataire-"+document+"@example.test";
        jdbc.update("INSERT INTO quittance_envoi(id,quittance_id,requete_id,destinataire,mode,statut,cree_le) VALUES (?,?,?,?,'MAILPIT','EN_ATTENTE_ENVOI',?)",id,document,requete,adresse,Timestamp.from(date));
        audit(auth,document,"ENVOI_MAILPIT_DEMANDE","SUCCES");
        return new Reservation(new Envoi(id,adresse,"EN_ATTENTE_ENVOI",date,null,null),true);
    }
    @Transactional(timeout=10)
    public Envoi terminer(Authentication auth, UUID document, UUID envoi, String statut, String code) {
        proprietaire(auth,document);
        if(!Set.of("ENVOYE","ECHEC_ENVOI","INDETERMINE").contains(statut)) throw new IllegalArgumentException("Statut invalide");
        int change=jdbc.update("UPDATE quittance_envoi SET statut=?,erreur_code=?,termine_le=? WHERE id=? AND quittance_id=? AND statut='EN_ATTENTE_ENVOI'",statut,code,Timestamp.from(Instant.now()),envoi,document);
        if(change!=1) throw erreur(409,"La tentative a déjà été clôturée. Consultez Mailpit avant de poursuivre.");
        audit(auth,document,"ENVOI_MAILPIT_"+statut,"ENVOYE".equals(statut)?"SUCCES":"ECHEC");
        return jdbc.queryForObject("SELECT * FROM quittance_envoi WHERE id=? AND quittance_id=?",mapper,envoi,document);
    }
    @Transactional(timeout=10)
    public Page lister(Authentication auth, UUID document, int page, int taille) {
        proprietaire(auth,document);
        if(page<0 || page>100000 || taille<1 || taille>50)throw erreur(400,"Pagination invalide.");
        int reprises=jdbc.update("UPDATE quittance_envoi SET statut='INDETERMINE',erreur_code='EXECUTION_INTERROMPUE',termine_le=CURRENT_TIMESTAMP WHERE quittance_id=? AND statut='EN_ATTENTE_ENVOI' AND cree_le<CURRENT_TIMESTAMP-INTERVAL '10 minutes'",document);
        if(reprises>0)audit(auth,document,"ENVOI_MAILPIT_INTERROMPU","ECHEC");
        long total=jdbc.queryForObject("SELECT count(*) FROM quittance_envoi WHERE quittance_id=?",Long.class,document);
        var lignes=jdbc.query("SELECT * FROM quittance_envoi WHERE quittance_id=? ORDER BY cree_le DESC,id DESC LIMIT ? OFFSET ?",mapper,document,taille,(long)page*taille);
        return new Page(List.copyOf(lignes),page,taille,total,(total+taille-1)/taille);
    }
    private com.gestionlocative.quittances.entity.Quittance proprietaire(Authentication auth,UUID id) {
        var lecteur=LecteurQuittances.depuis(auth);
        if(!lecteur.bailleur())throw new AccessDeniedException("Envois réservés au bailleur.");
        return documents.trouverPourModification(id,lecteur.identifiant()).orElseThrow(()->erreur(404,"Document introuvable."));
    }
    private void audit(Authentication auth,UUID doc,String operation,String resultat) {
        var jwt=(JwtAuthenticationToken)auth;
        String acteur=jwt.getToken().getSubject(),issuer=jwt.getToken().getClaimAsString("iss");
        if(acteur==null || acteur.isBlank() || acteur.length()>200 || issuer==null || issuer.isBlank() || issuer.length()>500)
            throw new AccessDeniedException("Identité insuffisante pour l'audit.");
        jdbc.update("INSERT INTO quittance_audit(id,quittance_id,acteur,emetteur_identite,operation,resultat,horodatage) VALUES (?,?,?,?,?,?,?)",UUID.randomUUID(),doc,acteur,issuer,operation,resultat,Timestamp.from(Instant.now()));
    }
    private static ResponseStatusException erreur(int code,String message) {return new ResponseStatusException(HttpStatus.valueOf(code),message);}
}
