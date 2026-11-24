package com.gestionlocative.quittances.notification;

import com.gestionlocative.quittances.config.ReseauDemo;

import com.gestionlocative.quittances.service.EmissionDemoService;
import com.gestionlocative.quittances.service.EmissionReelleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ByteArrayResource;
import java.util.UUID;

/** Adaptateur SMTP de test uniquement. Aucun destinataire réel accepté. */
@Service
public class EnvoiMailpitService {
    @Value("${DARLOC_DOCKER_DEMO:false}")
    private boolean dockerDemo;

    @Value("${DARLOC_MAILPIT_INTEGRATION_REELLE:false}")
    private boolean integrationReelle;

    @Value("${quittances.emission-reelle.active:false}")
    private boolean emissionReelleActive;

    private final EmissionReelleService documentsReels;

    private final EnvoiMailpitTransactions transactions;
    private final EmissionDemoService documents;
    private final boolean actif;
    private final String adresse,mode;
    private final int port;
    public EnvoiMailpitService(EnvoiMailpitTransactions transactions,EmissionDemoService documents, EmissionReelleService documentsReels,
        @Value("${DARLOC_MAILPIT_ACTIF:false}") boolean actif,
        @Value("${server.address:}") String adresse,
        @Value("${quittances.securite.mode:}") String mode,
        @Value("${DARLOC_MAILPIT_PORT:1025}") int port) {
        this.transactions=transactions;this.documents=documents; this.documentsReels=documentsReels;this.actif=actif;this.adresse=adresse;this.mode=mode;this.port=port;
    }
    public EnvoiMailpitTransactions.Envoi envoyer(Authentication auth, UUID document,UUID requete,Boolean confirmation) {
        if(requete==null || !Boolean.TRUE.equals(confirmation)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirmez l'envoi et fournissez son identifiant de requête.");
        boolean modeReelAutorise = integrationReelle
                && emissionReelleActive
                && !dockerDemo
                && "COMMUN".equals(mode)
                && "0.0.0.0".equals(adresse)
                && port == 1025;

        boolean modeDemoAutorise = !integrationReelle
                && ReseauDemo.ecouteAutorisee(adresse, dockerDemo)
                && ("LOCAL".equals(mode)
                    || (dockerDemo && "COMMUN".equals(mode)));

        if(!actif || !(modeReelAutorise || modeDemoAutorise) || port<1024 || port>65535)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Activez Mailpit dans la configuration locale.");
        var reservation = modeReelAutorise
                ? transactions.reserver(auth, document, requete, true)
                : transactions.reserver(auth, document, requete);
        if(!reservation.nouveau())return reservation.envoi(); // même requête : jamais de deuxième SMTP
        var envoi=reservation.envoi();
        JavaMailSenderImpl smtp=new JavaMailSenderImpl();
        smtp.setHost((dockerDemo || modeReelAutorise) ? "mailpit" : "127.0.0.1");smtp.setPort(port);smtp.setDefaultEncoding("UTF-8");
        var props=smtp.getJavaMailProperties();
        props.setProperty("mail.smtp.connectiontimeout","3000");props.setProperty("mail.smtp.timeout","5000");props.setProperty("mail.smtp.writetimeout","5000");
        jakarta.mail.internet.MimeMessage message;
        try {
            byte[] pdf = modeReelAutorise
                    ? documentsReels.telechargerPdf(auth, document)
                    : documents.telecharger(auth, document, false);
            smtp.testConnection(); // échec ici : aucun message n'a été transmis
            message=smtp.createMimeMessage();
            var helper=new MimeMessageHelper(message,true,"UTF-8");
            helper.setFrom("quittances@example.test");helper.setTo(envoi.destinataire());
            helper.setSubject(modeReelAutorise
                    ? "DarLoc - Quittance signee - Test integration"
                    : "DarLoc — document de démonstration");
            helper.setText(modeReelAutorise
                    ? "Bonjour,\n\nVous trouverez votre quittance en piece jointe.\nCe message est reserve aux tests locaux Mailpit.\n\nDarLoc"
                    : "Bonjour,\n\nVous trouverez votre document de démonstration en pièce jointe.\nCe message est capturé localement par Mailpit.\n\nDarLoc", false);
            helper.addAttachment(
                    (modeReelAutorise ? "quittance-" : "document-demo-")
                        + document + ".pdf",
                    new ByteArrayResource(pdf),
                    "application/pdf");
            message.setHeader("X-DarLoc-Envoi",envoi.id().toString());
        } catch(Exception e) {
            transactions.terminer(auth,document,envoi.id(),"ECHEC_ENVOI","PREPARATION_OU_CONNEXION");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Aucun email transmis. Vérifiez Mailpit et le document ; une nouvelle tentative sera possible.");
        }
        try { smtp.send(message); }
        catch(Exception e) {
            transactions.terminer(auth,document,envoi.id(),"INDETERMINE","RESULTAT_SMTP_INCONNU");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Le résultat SMTP est incertain. Vérifiez Mailpit ; aucun réessai automatique ne sera effectué.");
        }
        // Une panne DB après acceptation laisse la tentative bloquée, jamais automatiquement réexpédiée.
        return transactions.terminer(auth,document,envoi.id(),"ENVOYE",null);
    }
}
