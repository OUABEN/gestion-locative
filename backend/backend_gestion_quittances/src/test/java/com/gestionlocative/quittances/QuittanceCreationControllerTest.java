package com.gestionlocative.quittances;

import com.gestionlocative.quittances.controller.QuittanceCreationController;
import com.gestionlocative.quittances.exception.QuittanceCreationExceptionHandler;
import com.gestionlocative.quittances.service.PreparationQuittanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuittanceCreationControllerTest {

    private PreparationQuittanceService preparationService;
    private MockMvc mvc;

    @BeforeEach
    void preparer() {
        preparationService = mock(PreparationQuittanceService.class);

        mvc = MockMvcBuilders.standaloneSetup(
                        new QuittanceCreationController(preparationService)
                )
                .setControllerAdvice(
                        new QuittanceCreationExceptionHandler()
                )
                .build();
    }

    @Test
    void doitCreerBrouillonAvecIdentiteDuJeton() throws Exception {
        UUID id = UUID.randomUUID();

        when(preparationService.preparer(
                "BAILLEUR-TEST",
                "BAIL-DEMO-001",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)
        )).thenReturn(id);

        mvc.perform(post("/api/v1/quittances")
                        .principal(identite("BAILLEUR", "BAILLEUR-TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandeValide()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.statut").value("BROUILLON"))
                .andExpect(jsonPath("$.statutSignature").value("NON_SIGNE"));

        verify(preparationService).preparer(
                "BAILLEUR-TEST",
                "BAIL-DEMO-001",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)
        );
    }

    @Test
    void doitRefuserCreationParLocataire() throws Exception {
        mvc.perform(post("/api/v1/quittances")
                        .principal(identite("LOCATAIRE", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandeValide()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(preparationService);
    }

    @Test
    void doitRefuserBailleurSansIdentifiant() throws Exception {
        mvc.perform(post("/api/v1/quittances")
                        .principal(identite("BAILLEUR", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandeValide()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(preparationService);
    }

    @Test
    void doitRefuserPeriodeInversee() throws Exception {
        mvc.perform(post("/api/v1/quittances")
                        .principal(identite("BAILLEUR", "BAILLEUR-TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bailId": "BAIL-DEMO-001",
                                  "periodeDebut": "2026-10-31",
                                  "periodeFin": "2026-10-01"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(preparationService);
    }

    @Test
    void doitRefuserDateIllisible() throws Exception {
        mvc.perform(post("/api/v1/quittances")
                        .principal(identite("BAILLEUR", "BAILLEUR-TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bailId": "BAIL-DEMO-001",
                                  "periodeDebut": "date-invalide",
                                  "periodeFin": "2026-10-31"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(preparationService);
    }

    private JwtAuthenticationToken identite(
            String role,
            String bailleurId
    ) {
        Jwt.Builder constructeur = Jwt.withTokenValue("jeton-de-test")
                .header("alg", "RS256")
                .subject("utilisateur-test");

        if (bailleurId != null) {
            constructeur.claim("bailleur_id", bailleurId);
        }

        return new JwtAuthenticationToken(
                constructeur.build(),
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    private String demandeValide() {
        return """
                {
                  "bailId": "BAIL-DEMO-001",
                  "periodeDebut": "2026-10-01",
                  "periodeFin": "2026-10-31"
                }
                """;
    }


    @Test
    void doitAutoriserCreationParGestionnaire() throws Exception {
        UUID id = UUID.randomUUID();
        var gestionnaire = identite("GESTIONNAIRE", null);

        when(preparationService.preparerPourGestionnaire(
                gestionnaire,
                "BAIL-DEMO-001",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)
        )).thenReturn(id);

        mvc.perform(post("/api/v1/quittances")
                        .principal(gestionnaire)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandeValide()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.statut").value("BROUILLON"));

        verify(preparationService).preparerPourGestionnaire(
                gestionnaire,
                "BAIL-DEMO-001",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31));
    }

    @Test
    void doitRefuserCreationParLocataireAvecIdentiteConfirmee()
            throws Exception {

        var jwt = Jwt.withTokenValue("jeton-locataire-test")
                .header("alg", "HS256")
                .subject("COMPTE-LOCATAIRE")
                .claim("locataire_id", "LOC-TEST")
                .build();

        var locataire = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_LOCATAIRE")));

        mvc.perform(post("/api/v1/quittances")
                        .principal(locataire)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandeValide()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(preparationService);
    }

}