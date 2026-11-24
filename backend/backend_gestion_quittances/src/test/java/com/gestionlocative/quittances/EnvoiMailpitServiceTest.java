package com.gestionlocative.quittances;
import com.gestionlocative.quittances.notification.*;
import com.gestionlocative.quittances.service.EmissionDemoService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnvoiMailpitServiceTest {
    private final EnvoiMailpitTransactions transactions=mock(EnvoiMailpitTransactions.class);
    private final EmissionDemoService documents=mock(EmissionDemoService.class);
    private final TestingAuthenticationToken auth=new TestingAuthenticationToken("test","");
    private final UUID doc=UUID.randomUUID(),requete=UUID.randomUUID(),envoi=UUID.randomUUID();
    private EnvoiMailpitService service(boolean actif,String mode) {return new EnvoiMailpitService(transactions,documents,actif,"127.0.0.1",mode,1025);}
    @Test void refuseSansConfirmation(){
        var e=assertThrows(ResponseStatusException.class,()->service(true,"LOCAL").envoyer(auth,doc,requete,false));
        assertEquals(400,e.getStatusCode().value());verifyNoInteractions(transactions,documents);
    }
    @Test void refuseSiDesactive(){
        var e=assertThrows(ResponseStatusException.class,()->service(false,"LOCAL").envoyer(auth,doc,requete,true));
        assertEquals(503,e.getStatusCode().value());verifyNoInteractions(transactions,documents);
    }
    @Test void refuseHorsModeLocal(){
        assertThrows(ResponseStatusException.class,()->service(true,"INTEGRATION").envoyer(auth,doc,requete,true));
        verifyNoInteractions(transactions,documents);
    }
    @Test void refuseEnModeCommun(){
        assertThrows(ResponseStatusException.class,()->service(true,"COMMUN").envoyer(auth,doc,requete,true));
        verifyNoInteractions(transactions,documents);
    }
    @Test void repetitionNeRelitPasLePdfEtNeRenvoiePas(){
        var existant=new EnvoiMailpitTransactions.Envoi(envoi,"test@example.test","ENVOYE",Instant.now(),Instant.now(),null);
        when(transactions.reserver(auth,doc,requete)).thenReturn(new EnvoiMailpitTransactions.Reservation(existant,false));
        assertSame(existant,service(true,"LOCAL").envoyer(auth,doc,requete,true));
        verifyNoInteractions(documents);verify(transactions).reserver(auth,doc,requete);verifyNoMoreInteractions(transactions);
    }
    @Test void echecAvantSmtpEstHistoriseEtNonAnnonceCommeSucces(){
        var nouveau=new EnvoiMailpitTransactions.Envoi(envoi,"test@example.test","EN_ATTENTE_ENVOI",Instant.now(),null,null);
        when(transactions.reserver(auth,doc,requete)).thenReturn(new EnvoiMailpitTransactions.Reservation(nouveau,true));
        when(documents.telecharger(auth,doc,false)).thenThrow(new IllegalStateException("PDF indisponible"));
        var e=assertThrows(ResponseStatusException.class,()->service(true,"LOCAL").envoyer(auth,doc,requete,true));
        assertEquals(503,e.getStatusCode().value());
        verify(transactions).terminer(auth,doc,envoi,"ECHEC_ENVOI","PREPARATION_OU_CONNEXION");
    }
}
