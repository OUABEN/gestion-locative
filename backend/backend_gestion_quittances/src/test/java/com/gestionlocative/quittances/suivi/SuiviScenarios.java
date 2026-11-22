package com.gestionlocative.quittances.suivi;

import com.gestionlocative.quittances.client.PaiementClient;
import com.gestionlocative.quittances.dto.*;
import com.gestionlocative.quittances.dto.SituationPeriode.*;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.exception.ServiceExterneException;
import com.gestionlocative.quittances.mapper.QuittanceConsultationMapper;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.service.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SuiviScenarios {
    static final LocalDate DEBUT = LocalDate.of(2026,10,1), FIN = LocalDate.of(2026,10,31);
    static final UUID ID = UUID.fromString("d251f229-598e-4f57-87ad-fb3bae8fa27b");
    static int tests;
    static void verifier(boolean ok, String message) { if (!ok) throw new AssertionError(message); tests++; }
    static <T extends Throwable> void refuse(Class<T> type, Runnable action) {
        try { action.run(); } catch(Throwable e) { verifier(type.isInstance(e), "Exception inattendue: " + e); return; }
        throw new AssertionError("Refus attendu: " + type);
    }
    static Authentication identite(String role, String id) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg","RS256").subject("test")
                .claim("iss","urn:gestionlocative:dev").claim(role.equals("BAILLEUR") ? "bailleur_id" : "locataire_id",id).build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
    static Affectation source(String id, String paiement, String montant, Nature nature, StatutPaiement statut) {
        return new Affectation(id,paiement,"BAIL",DEBUT,FIN,nature,new BigDecimal(montant),"MAD",statut);
    }
    static AffectationQuittanceResponse conservee() {
        return new AffectationQuittanceResponse("AFF","PAY",DEBUT,FIN,"LOYER",new BigDecimal("1500.00"),"MAD","CONFIRME");
    }
    static List<ComparateurPaiements.Ecart> comparer(Affectation... actuelles) {
        return ComparateurPaiements.comparer("BAIL",DEBUT,FIN,List.of(conservee()),Arrays.asList(actuelles));
    }
    public static void comparaison() {
        var identique = source("AFF","PAY","1500",Nature.LOYER,StatutPaiement.CONFIRME);
        verifier(comparer(identique).isEmpty(),"Échelle BigDecimal ignorée");
        verifier(comparer(identique,identique).isEmpty(),"Doublon identique dédupliqué");
        verifier(comparer().getFirst().natureEcart().equals("ABSENTE"),"Suppression détectée");
        for(var statut : List.of(StatutPaiement.ANNULE,StatutPaiement.REMBOURSE,StatutPaiement.EN_ATTENTE))
            verifier(comparer(source("AFF","PAY","1500",Nature.LOYER,statut)).getFirst().natureEcart().equals("MODIFIEE"),"Statut changé");
        verifier(comparer(source("AFF","PAY","1400",Nature.LOYER,StatutPaiement.CONFIRME)).size()==1,"Montant changé");
        verifier(comparer(source("AFF","AUTRE","1500",Nature.LOYER,StatutPaiement.CONFIRME)).size()==1,"Référence paiement changée");
        verifier(comparer(source("AFF","PAY","1500",Nature.CHARGES,StatutPaiement.CONFIRME)).size()==1,"Nature changée");
        verifier(comparer(identique,source("NOUV","PAY2","100",Nature.CHARGES,StatutPaiement.CONFIRME)).getFirst().natureEcart().equals("AJOUTEE"),"Ajout détecté");
        verifier(comparer(identique,source("NOUV","PAY2","100",Nature.DEPOT_GARANTIE,StatutPaiement.CONFIRME)).isEmpty(),"Dépôt exclu");
        verifier(comparer(identique,source("NOUV","PAY2","100",Nature.EXCEDENT,StatutPaiement.CONFIRME)).isEmpty(),"Excédent exclu");
        verifier(comparer(identique,source("NOUV","PAY2","0",Nature.LOYER,StatutPaiement.CONFIRME)).isEmpty(),"Zéro exclu");
        verifier(comparer(identique,source("NOUV","PAY2","100",Nature.LOYER,StatutPaiement.EN_ATTENTE)).isEmpty(),"Nouveau non confirmé exclu");
        refuse(ServiceExterneException.class,()->comparer(identique,source("AFF","PAY","1499",Nature.LOYER,StatutPaiement.CONFIRME)));
        refuse(ServiceExterneException.class,()->comparer(identique,source("AUTRE","PAY","100",Nature.CHARGES,StatutPaiement.ANNULE)));
        refuse(ServiceExterneException.class,()->comparer(source("AFF","PAY","-1",Nature.LOYER,StatutPaiement.CONFIRME)));
        refuse(ServiceExterneException.class,()->comparer(source("AFF","PAY","1.001",Nature.LOYER,StatutPaiement.CONFIRME)));
        refuse(ServiceExterneException.class,()->comparer(new Affectation("AFF","PAY","AUTRE",DEBUT,FIN,Nature.LOYER,BigDecimal.ONE,"MAD",StatutPaiement.CONFIRME)));
        refuse(ServiceExterneException.class,()->comparer(new Affectation("AFF","PAY","BAIL",DEBUT,FIN,Nature.LOYER,BigDecimal.ONE,"EUR",StatutPaiement.CONFIRME)));
        refuse(ServiceExterneException.class,()->comparer(new Affectation("AFF","PAY","BAIL",DEBUT,FIN.plusDays(1),Nature.LOYER,BigDecimal.ONE,"MAD",StatutPaiement.CONFIRME)));
        refuse(ServiceExterneException.class,()->comparer((Affectation)null));
    }
    public static void csv() {
        verifier(ExportCsvService.cellule("=1+1").equals("\"'=1+1\""),"Formule neutralisée");
        for(String s:List.of("+CMD","-CMD","@SUM(1)","   =1","\t=1","\r=1","\n=1"))
            verifier(ExportCsvService.cellule(s).startsWith("\"'"),"Préfixe dangereux neutralisé");
        verifier(ExportCsvService.cellule("a;\"b\"\nC").equals("\"a;\"\"b\"\"\nC\""),"Guillemets et séparateurs échappés");
        verifier(ExportCsvService.cellule(null).equals("\"\""),"Null exportable");
        var auth = identite("BAILLEUR","OWNER");
        var doc = new QuittanceConsultationMapper().convertir(Quittance.creerBrouillon("BAIL","LOC","BIEN","OWNER",DEBUT,FIN,
                new BigDecimal("3000"),new BigDecimal("200"),new BigDecimal("1500"),BigDecimal.ZERO,TypeDocument.RECU_PARTIEL,Map.of()));
        AtomicInteger pages = new AtomicInteger();
        var consultation = new ConsultationQuittanceService(null,null) {
            @Override public PageQuittancesResponse lister(Authentication a,int page,int taille,String tri,String sens,String bail,String numero,String statut,LocalDate debut,LocalDate fin) {
                verifier(a==auth && bail.equals("BAIL") && statut.equals("BROUILLON") && debut.equals(DEBUT) && fin.equals(FIN),"Filtres et identité propagés");
                verifier(page==pages.getAndIncrement(),"Toutes les pages parcourues");
                return new PageQuittancesResponse(List.of(doc),page,100,2,2);
            }
        };
        String csv = new String(new ExportCsvService(consultation).exporter(auth,"creeLe","desc","BAIL",null,"BROUILLON",DEBUT,FIN),StandardCharsets.UTF_8);
        verifier(pages.get()==2 && csv.startsWith("\uFEFF") && csv.lines().count()==3,"BOM et toutes les pages exportées");
        var trop = new ConsultationQuittanceService(null,null) {
            @Override public PageQuittancesResponse lister(Authentication a,int page,int taille,String tri,String sens,String bail,String numero,String statut,LocalDate debut,LocalDate fin) {
                return new PageQuittancesResponse(List.of(),page,100,5001,51);
            }
        };
        refuse(ResponseStatusException.class,()->new ExportCsvService(trop).exporter(auth,"creeLe","desc",null,null,null,null,null));
    }
    public static void acces() {
        AtomicInteger appels = new AtomicInteger();
        QuittanceRepository repo = (QuittanceRepository)Proxy.newProxyInstance(SuiviScenarios.class.getClassLoader(),new Class[]{QuittanceRepository.class},(p,m,args)->{
            verifier(m.getName().equals("findByIdAndBailleurId"),"Recherche limitée au bailleur");
            verifier(args[0].equals(ID) && args[1].equals("OWNER"),"Identité utilisée dans la recherche");
            return Optional.empty();
        });
        var consultation = new ConsultationQuittanceService(repo,new QuittanceConsultationMapper());
        var references = new ReferencePaiementsService(consultation,new ConsultationAffectationService(consultation,null));
        PaiementClient client=(b,d,f)->{appels.incrementAndGet();return List.of();};
        var service = new ReconciliationPaiementsService(references,client);
        refuse(AccessDeniedException.class,()->service.controler(identite("LOCATAIRE","LOC"),ID));
        refuse(AccessDeniedException.class,()->service.controler(null,ID));
        refuse(ResponseStatusException.class,()->service.controler(identite("BAILLEUR","OWNER"),ID));
        verifier(appels.get()==0,"Aucun appel externe sans document autorisé");
        var tableau = new TableauBordService(new JdbcTemplate() {
            @Override @SuppressWarnings("unchecked") public <T> List<T> query(String sql, RowMapper<T> mapper, Object... args) {
                boolean bailleur=args[0].equals("OWNER");
                verifier(sql.contains(bailleur ? "bailleur_id = ?" : "locataire_id = ? AND emis_le IS NOT NULL"),"Portée SQL correcte");
                verifier(!sql.contains(args[0].toString()),"Identité liée comme paramètre");
                return (List<T>)List.of(new TableauBordService.Groupe("BROUILLON","RECU_PARTIEL",2),new TableauBordService.Groupe("EMIS","QUITTANCE_INTEGRALE",1));
            }
        });
        verifier(tableau.consulter(identite("BAILLEUR","OWNER")).totalDocuments()==3,"Compteurs globaux");
        tableau.consulter(identite("LOCATAIRE","LOC"));
        refuse(AccessDeniedException.class,()->tableau.consulter(null));
    }
    public static void main(String[] args) { comparaison(); csv(); acces(); System.out.println("OK : " + tests + " vérifications Java."); }
}
