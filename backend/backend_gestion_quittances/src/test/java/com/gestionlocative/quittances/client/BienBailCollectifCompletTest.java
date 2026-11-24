package com.gestionlocative.quittances.client;

import com.gestionlocative.quittances.dto.ReferentielsExternes.Bien;
import com.gestionlocative.quittances.dto.ReferentielsExternes.BailPeriode;
import com.gestionlocative.quittances.exception.RegleMetierException;
import com.gestionlocative.quittances.exception.ServiceExterneException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BienBailCollectifCompletTest {

    private static final String PROPRIETAIRE =
            "11111111-1111-1111-1111-111111111111";

    private static final LocalDate DEBUT =
            LocalDate.of(2026, 10, 1);

    private static final LocalDate FIN =
            LocalDate.of(2026, 10, 31);

    private MockRestServiceServer serveur;
    private BienBailCollectifClient client;

    @BeforeEach
    void preparer() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:8081");

        serveur = MockRestServiceServer.bindTo(builder).build();

        RestClient http = builder.build();

        client = new BienBailCollectifClient(
                http, "/api/biens/{id}",
                http, "/api/baux/{id}");
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        serveur.verify();
    }

    private String bien() {
        return """
                {
                  "id": "bien-123",
                  "adresse": "10 rue Test",
                  "ville": "Agadir",
                  "bailleurId": "%s",
                  "charges": 200.00
                }
                """.formatted(PROPRIETAIRE);
    }

    private String bail() {
        return """
                {
                  "id": "bail-123",
                  "bienId": "bien-123",
                  "locataireId": "loc-123",
                  "dateDebut": "2026-01-01",
                  "dateFin": "2026-12-31",
                  "montantLoyer": 3000.00,
                  "statut": "ACTIF"
                }
                """;
    }

    private void reponseBien(String json) {
        serveur.expect(requestTo(
                "http://localhost:8081/api/biens/bien-123"))
                .andRespond(withSuccess(
                        json, MediaType.APPLICATION_JSON));
    }

    private void reponseBail(String json) {
        serveur.expect(requestTo(
                "http://localhost:8081/api/baux/bail-123"))
                .andRespond(withSuccess(
                        json, MediaType.APPLICATION_JSON));
    }

    private void authentifier(
            String sujet, String role, boolean avecNom) {

        Jwt.Builder jeton = Jwt.withTokenValue("jeton-test")
                .header("alg", "RS256")
                .subject(sujet);

        if ("ROLE_BAILLEUR".equals(role)) {
            jeton.claim("bailleur_id", sujet);
        }

        if (avecNom) {
            jeton.claim("nom", "Alami");
            jeton.claim("prenom", "Mehdi");
        }

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(
                        jeton.build(),
                        List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    void consulterBienValide() {
        reponseBien(bien());

        assertEquals(
                new Bien("1", "bien-123",
                        "10 rue Test, Agadir", PROPRIETAIRE),
                client.consulterBien("bien-123"));
    }

    @Test
    void consulterBienSansVille() {
        reponseBien(bien().replace(
                "\"Agadir\"", "null"));

        assertEquals(
                new Bien("1", "bien-123",
                        "10 rue Test", PROPRIETAIRE),
                client.consulterBien("bien-123"));
    }

    @Test
    void refuseIdentifiantBienIncoherent() {
        reponseBien(bien().replace(
                "bien-123", "autre-bien"));

        assertThrows(ServiceExterneException.class,
                () -> client.consulterBien("bien-123"));
    }

    @Test
    void refuseAdresseAbsente() {
        reponseBien(bien().replace(
                "\"10 rue Test\"", "null"));

        assertThrows(ServiceExterneException.class,
                () -> client.consulterBien("bien-123"));
    }

    @Test
    void refuseDateDebutAbsente() {
        assertThrows(RegleMetierException.class,
                () -> client.consulterBail(
                        "bail-123", null, FIN));
    }

    @Test
    void refuseDateFinAbsente() {
        assertThrows(RegleMetierException.class,
                () -> client.consulterBail(
                        "bail-123", DEBUT, null));
    }

    @Test
    void refusePeriodeInversee() {
        assertThrows(RegleMetierException.class,
                () -> client.consulterBail(
                        "bail-123", FIN, DEBUT));
    }

    @Test
    void consulteBailMensuelDuProprietaire() {
        authentifier(PROPRIETAIRE, "ROLE_BAILLEUR", true);

        reponseBail(bail());
        reponseBien(bien());

        assertEquals(new BailPeriode(
                "1", "bail-123", "loc-123", "bien-123",
                PROPRIETAIRE, "Mehdi Alami",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                DEBUT, FIN,
                new BigDecimal("3000.00"),
                new BigDecimal("200.00"), "MAD"),
                client.consulterBail("bail-123", DEBUT, FIN));
    }

    @Test
    void accepteBailTermineCouvrantLaPeriode() {
        authentifier(PROPRIETAIRE, "ROLE_BAILLEUR", true);

        reponseBail(bail().replace("ACTIF", "TERMINE"));
        reponseBien(bien());

        assertEquals(new BailPeriode(
                "1", "bail-123", "loc-123", "bien-123",
                PROPRIETAIRE, "Mehdi Alami",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                DEBUT, FIN,
                new BigDecimal("3000.00"),
                new BigDecimal("200.00"), "MAD"),
                client.consulterBail("bail-123", DEBUT, FIN));
    }

    @Test
    void refuseBailInactif() {
        reponseBail(bail().replace("ACTIF", "ANNULE"));

        assertThrows(ServiceExterneException.class,
                () -> client.consulterBail(
                        "bail-123", DEBUT, FIN));
    }

    @Test
    void refuseLoyerNegatif() {
        reponseBail(bail().replace("3000.00", "-1.00"));

        assertThrows(ServiceExterneException.class,
                () -> client.consulterBail(
                        "bail-123", DEBUT, FIN));
    }

    @Test
    void refuseBailDebutantApresLaPeriode() {
        reponseBail(bail().replace(
                "2026-01-01", "2026-11-01"));

        assertThrows(ServiceExterneException.class,
                () -> client.consulterBail(
                        "bail-123", DEBUT, FIN));
    }

    @Test
    void refuseBailFinissantAvantLaPeriode() {
        reponseBail(bail().replace(
                "2026-12-31", "2026-09-30"));

        assertThrows(ServiceExterneException.class,
                () -> client.consulterBail(
                        "bail-123", DEBUT, FIN));
    }

    @Test
    void refuseChargesNegatives() {
        reponseBail(bail());
        reponseBien(bien().replace("200.00", "-1.00"));

        assertThrows(ServiceExterneException.class,
                () -> client.consulterBail(
                        "bail-123", DEBUT, FIN));
    }

    @Test
    void refuseChargesAbsentes() {
        reponseBail(bail());
        reponseBien(bien().replace("200.00", "null"));

        assertThrows(ServiceExterneException.class,
                () -> client.consulterBail(
                        "bail-123", DEBUT, FIN));
    }

    @Test
    void refuseUtilisateurNonAuthentifie() {
        SecurityContextHolder.clearContext();

        reponseBail(bail());
        reponseBien(bien());

        assertThrows(AccessDeniedException.class,
                () -> client.consulterBail(
                        "bail-123", DEBUT, FIN));
    }

    @Test
    void refuseRoleLocataire() {
        authentifier(PROPRIETAIRE, "ROLE_LOCATAIRE", true);

        reponseBail(bail());
        reponseBien(bien());

        assertThrows(AccessDeniedException.class,
                () -> client.consulterBail(
                        "bail-123", DEBUT, FIN));
    }

    @Test
    void refuseAutreProprietaire() {
        authentifier("22222222-2222-2222-2222-222222222222",
                "ROLE_BAILLEUR", true);

        reponseBail(bail());
        reponseBien(bien());

        assertThrows(AccessDeniedException.class,
                () -> client.consulterBail(
                        "bail-123", DEBUT, FIN));
    }

    @Test
    void refuseIdentiteBailleurIncomplete() {
        authentifier(PROPRIETAIRE, "ROLE_BAILLEUR", false);

        reponseBail(bail());
        reponseBien(bien());

        assertThrows(AccessDeniedException.class,
                () -> client.consulterBail(
                        "bail-123", DEBUT, FIN));
    }


    private void authentifierGestionnaire() {
        Jwt jwt = Jwt.withTokenValue("jeton-gestionnaire-test")
                .header("alg", "HS256")
                .subject("22222222-2222-2222-2222-222222222222")
                .claim("nom", "Benjelloun")
                .claim("prenom", "Sara")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(
                        new SimpleGrantedAuthority("ROLE_GESTIONNAIRE"))));
    }

    private String identiteBailleur() {
        return """
                {
                  "versionContrat": "1",
                  "id": "%s",
                  "nom": "Alami",
                  "prenom": "Mehdi",
                  "nomComplet": "Mehdi Alami",
                  "role": "BAILLEUR"
                }
                """.formatted(PROPRIETAIRE);
    }

    @Test
    void gestionnaireRecupereIdentiteReelle() {
        authentifierGestionnaire();

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:8081")
                .requestInterceptor(
                        new com.gestionlocative.quittances.config
                                .JwtUtilisateurInterceptor());

        serveur = MockRestServiceServer.bindTo(builder).build();

        RestClient http = builder.build();

        client = new BienBailCollectifClient(
                http, "/api/biens/{id}",
                http, "/api/baux/{id}");

        reponseBail(bail());
        reponseBien(bien());

        serveur.expect(requestTo(
                "http://localhost:8081/api/users/" + PROPRIETAIRE))
                .andExpect(
                        org.springframework.test.web.client.match
                                .MockRestRequestMatchers.header(
                                        "Authorization",
                                        "Bearer jeton-gestionnaire-test"))
                .andRespond(withSuccess(
                        identiteBailleur(), MediaType.APPLICATION_JSON));

        BailPeriode resultat = client.consulterBail(
                "bail-123", DEBUT, FIN);

        assertEquals(PROPRIETAIRE, resultat.bailleurId());
        assertEquals("Mehdi Alami", resultat.nomBailleur());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "id", "role", "version", "nom",
            "prenom", "nomComplet", "incoherent"
    })
    void gestionnaireRefuseIdentiteInvalide(String champ) {
        authentifierGestionnaire();

        reponseBail(bail());
        reponseBien(bien());

        String json = identiteBailleur();

        json = switch (champ) {
            case "id" -> json.replace(PROPRIETAIRE,
                    "22222222-2222-2222-2222-222222222222");
            case "role" -> json.replace(
                    "\"BAILLEUR\"", "\"GESTIONNAIRE\"");
            case "version" -> json.replace(
                    "\"1\"", "\"2\"");
            case "nom" -> json.replace(
                    "\"Alami\"", "null");
            case "prenom" -> json.replace(
                    "\"Mehdi\"", "null");
            case "nomComplet" -> json.replace(
                    "\"Mehdi Alami\"", "null");
            case "incoherent" -> json.replace(
                    "\"Mehdi Alami\"", "\"Sara Benjelloun\"");
            default -> throw new IllegalArgumentException(champ);
        };

        serveur.expect(requestTo(
                "http://localhost:8081/api/users/" + PROPRIETAIRE))
                .andRespond(withSuccess(
                        json, MediaType.APPLICATION_JSON));

        assertThrows(ServiceExterneException.class,
                () -> client.consulterBail(
                        "bail-123", DEBUT, FIN));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {
            403, 404, 503
    })
    void gestionnaireRefuseSiIdentiteInaccessible(int statut) {
        authentifierGestionnaire();

        reponseBail(bail());
        reponseBien(bien());

        serveur.expect(requestTo(
                "http://localhost:8081/api/users/" + PROPRIETAIRE))
                .andRespond(
                        org.springframework.test.web.client.response
                                .MockRestResponseCreators.withStatus(
                                        org.springframework.http.HttpStatus
                                                .valueOf(statut)));

        assertThrows(ServiceExterneException.class,
                () -> client.consulterBail(
                        "bail-123", DEBUT, FIN));
    }

}