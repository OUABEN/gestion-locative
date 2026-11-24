package com.gestionlocative.quittances;

import com.gestionlocative.quittances.client.PaiementClient;
import com.gestionlocative.quittances.pdf.EmissionDemoPdfService;
import com.gestionlocative.quittances.service.EmissionDemoService;
import com.gestionlocative.quittances.service.EmissionDemoTransactions;
import com.gestionlocative.quittances.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmissionDemoServiceTest {
    private final EmissionDemoTransactions transactions = mock(EmissionDemoTransactions.class);
    private final EmissionDemoPdfService pdf = mock(EmissionDemoPdfService.class);
    private final PaiementClient paiements = mock(PaiementClient.class);
    private final UUID id = UUID.randomUUID();
    private final TestingAuthenticationToken auth = new TestingAuthenticationToken("acteur", "");
    private EmissionDemoService service(boolean actif, String adresse, String mode) {
        return new EmissionDemoService(transactions,pdf,paiements,actif,adresse,mode,"absent.p12","secret-test");
    }
    @Test void confirmationObligatoire() {
        var e=assertThrows(ResponseStatusException.class,()->service(true,"127.0.0.1","LOCAL").emettre(auth,id,false));
        assertEquals(400,e.getStatusCode().value()); verifyNoInteractions(transactions,paiements,pdf);
    }
    @Test void configurationInactiveRefusee() {
        var e=assertThrows(ResponseStatusException.class,()->service(false,"127.0.0.1","LOCAL").emettre(auth,id,true));
        assertEquals(503,e.getStatusCode().value()); verifyNoInteractions(transactions,paiements,pdf);
    }
    @Test void modeIntegrationRefuse() {
        assertThrows(ResponseStatusException.class,()->service(true,"127.0.0.1","INTEGRATION").emettre(auth,id,true));
        verifyNoInteractions(transactions,paiements,pdf);
    }
    @Test void modeCommunRefuse() {
        assertThrows(ResponseStatusException.class,()->service(true,"127.0.0.1","COMMUN").emettre(auth,id,true));
        verifyNoInteractions(transactions,paiements,pdf);
    }
    @Test void adressePubliqueRefusee() {
        assertThrows(ResponseStatusException.class,()->service(true,"0.0.0.0","LOCAL").emettre(auth,id,true));
        verifyNoInteractions(transactions,paiements,pdf);
    }
    @Test void paiementDisparuBloqueEmissionAvantPdf() {
        var debut=LocalDate.of(2026,10,1);var fin=LocalDate.of(2026,10,31);
        var doc=new QuittanceConsultationResponse(id,null,"BAIL","LOC","BIEN","Bailleur","Nom","Prenom","Adresse",
            debut,fin,new BigDecimal("100.00"),BigDecimal.ZERO,new BigDecimal("100.00"),BigDecimal.ZERO,new BigDecimal("100.00"),
            "MAD","QUITTANCE_INTEGRALE","EN_ATTENTE_SIGNATURE","EN_ATTENTE",Instant.now(),null);
        var affectation=new AffectationQuittanceResponse("AFF","PAY",debut,fin,"LOYER",new BigDecimal("100.00"),"MAD","CONFIRME");
        when(transactions.charger(auth,id)).thenReturn(new EmissionDemoTransactions.Source(new DonneesApercu(doc,List.of(affectation)),1,"DEMO-QT-2026-000001",Instant.now()));
        when(paiements.consulterAffectations("BAIL",debut,fin)).thenReturn(List.of());
        var e=assertThrows(ResponseStatusException.class,()->service(true,"127.0.0.1","LOCAL").emettre(auth,id,true));
        assertEquals(409,e.getStatusCode().value()); verifyNoInteractions(pdf);
        verify(transactions).charger(auth,id);verifyNoMoreInteractions(transactions);
    }
}
