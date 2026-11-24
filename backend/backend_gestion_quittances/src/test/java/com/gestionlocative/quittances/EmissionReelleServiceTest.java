package com.gestionlocative.quittances;

import com.gestionlocative.quittances.config.ReferentielsProperties;
import com.gestionlocative.quittances.document.SignataireReel;
import com.gestionlocative.quittances.pdf.EmissionReellePdfService;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.service.ConsultationQuittanceService;
import com.gestionlocative.quittances.service.ControlePrealableEmissionService;
import com.gestionlocative.quittances.service.EmissionReelleService;
import com.gestionlocative.quittances.service.EmissionReelleTransactions;
import com.gestionlocative.quittances.service.PreparationApercuService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmissionReelleServiceTest {
    private final ControlePrealableEmissionService controle = mock(ControlePrealableEmissionService.class);
    private final SignataireReel signataire = mock(SignataireReel.class);
    private final UUID id = UUID.randomUUID();

    @Test void desactiveeParDefaut() {
        var service = service(sources("UTILISATEUR"));
        ReflectionTestUtils.setField(service, "modeJwt", "COMMUN");
        ReflectionTestUtils.setField(service, "modePaiements", "UTILISATEUR");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class,
                        () -> service.emettre(null, id, true)).getStatusCode());
        verifyNoInteractions(controle);
    }

    @Test void confirmationObligatoire() {
        var service = service(sources("UTILISATEUR"));
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class,
                        () -> service.emettre(null, id, false)).getStatusCode());
        verifyNoInteractions(controle);
    }

    @Test void modeJwtLocalRefuse() {
        var service = service(sources("UTILISATEUR"));
        ReflectionTestUtils.setField(service, "actif", true);
        ReflectionTestUtils.setField(service, "modeJwt", "LOCAL");
        ReflectionTestUtils.setField(service, "modePaiements", "UTILISATEUR");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class,
                        () -> service.emettre(null, id, true)).getStatusCode());
        verifyNoInteractions(controle);
    }

    @Test void sourcesSimuleesRefusees() {
        var service = service(sources("SIMULATION"));
        ReflectionTestUtils.setField(service, "actif", true);
        ReflectionTestUtils.setField(service, "modeJwt", "COMMUN");
        ReflectionTestUtils.setField(service, "modePaiements", "UTILISATEUR");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class,
                        () -> service.emettre(null, id, true)).getStatusCode());
        verifyNoInteractions(controle);
    }

    @Test void certificatAbsentRefuse() {
        var service = service(sources("UTILISATEUR"));
        ReflectionTestUtils.setField(service, "actif", true);
        ReflectionTestUtils.setField(service, "modeJwt", "COMMUN");
        ReflectionTestUtils.setField(service, "modePaiements", "UTILISATEUR");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class,
                        () -> service.emettre(null, id, true)).getStatusCode());
        verifyNoInteractions(controle);
    }

    private EmissionReelleService service(ReferentielsProperties sources) {
        return new EmissionReelleService(controle, mock(PreparationApercuService.class),
                mock(QuittanceRepository.class), mock(JdbcTemplate.class),
                mock(EmissionReellePdfService.class), signataire,
                mock(EmissionReelleTransactions.class),
                mock(ConsultationQuittanceService.class), sources);
    }

    private static ReferentielsProperties sources(String mode) {
        var endpoint = new ReferentielsProperties.Endpoint(mode,
                "http://127.0.0.1:8082", "/api/v1/{id}", "");
        return new ReferentielsProperties(endpoint, endpoint, endpoint);
    }
}
