package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.client.BienBailClient;
import com.gestionlocative.quittances.client.LocataireClient;
import com.gestionlocative.quittances.client.PaiementClient;
import com.gestionlocative.quittances.config.ReferentielsProperties;
import com.gestionlocative.quittances.dto.ReferentielsExternes.BailPeriode;
import com.gestionlocative.quittances.dto.ReferentielsExternes.Bien;
import com.gestionlocative.quittances.dto.ReferentielsExternes.Locataire;
import com.gestionlocative.quittances.dto.SituationPeriode.Affectation;
import com.gestionlocative.quittances.dto.SituationPeriode.Nature;
import com.gestionlocative.quittances.dto.SituationPeriode.StatutPaiement;
import com.gestionlocative.quittances.entity.PaiementAffectation;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.exception.ServiceExterneException;
import com.gestionlocative.quittances.repository.PaiementAffectationRepository;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.security.DecodeurJwtCommun;
import com.gestionlocative.quittances.security.IdentiteAudit;
import com.gestionlocative.quittances.security.SecurityConfiguration;
import com.gestionlocative.quittances.security.ValidationJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ControlePrealableEmissionServiceTest {

    private static final String SECRET =
            "secret-fictif-precontrole-emission-32-octets";
    private static final String BAILLEUR_ID =
            "a54ec812-75a1-4ad0-8d16-115a1187c4c0";
    private static final String AUTRE_BAILLEUR_ID =
            "34f715aa-10db-4f66-9e2d-77a4770bcb87";
    private static final String BAIL_ID = "BAIL-TEST";
    private static final String BIEN_ID = "BIEN-TEST";
    private static final String LOCATAIRE_ID = "LOC-TEST";
    private static final LocalDate DEBUT_BAIL =
            LocalDate.of(2026, 1, 1);
    private static final LocalDate PERIODE_DEBUT =
            LocalDate.of(2026, 10, 1);
    private static final LocalDate PERIODE_FIN =
            LocalDate.of(2026, 10, 31);
    private static final String ADRESSE = "12 rue de test, Agadir";

    private final QuittanceRepository quittances =
            mock(QuittanceRepository.class);
    private final PaiementAffectationRepository affectations =
            mock(PaiementAffectationRepository.class);
    private final BienBailClient biensBaux = mock(BienBailClient.class);
    private final LocataireClient locataires = mock(LocataireClient.class);
    private final PaiementClient paiements = mock(PaiementClient.class);

    private final ConstructionBrouillonService construction =
            new ConstructionBrouillonService(
                    biensBaux,
                    locataires,
                    paiements,
                    new CalculQuittanceService()
            );

    private Quittance document;

    @BeforeEach
    void preparerPreuvesDeTest() {
        document = quittance(BAILLEUR_ID, TypeDocument.QUITTANCE_INTEGRALE,
                "3000.00", "200.00");

        when(quittances.findByIdAndBailleurId(
                quittanceId(),
                BAILLEUR_ID
        )).thenReturn(Optional.of(document));
        when(biensBaux.consulterBail(
                BAIL_ID,
                PERIODE_DEBUT,
                PERIODE_FIN
        )).thenReturn(bail(BAILLEUR_ID, "3000.00", "200.00"));
        when(biensBaux.consulterBien(BIEN_ID)).thenReturn(
                new Bien("1", BIEN_ID, ADRESSE, BAILLEUR_ID)
        );
        when(locataires.consulter(LOCATAIRE_ID)).thenReturn(
                new Locataire(
                        "1",
                        LOCATAIRE_ID,
                        "Locataire",
                        "Test",
                        null,
                        false
                )
        );
        when(paiements.consulterAffectations(
                BAIL_ID,
                PERIODE_DEBUT,
                PERIODE_FIN
        )).thenReturn(paiementsComplets());
        when(affectations
                .findByQuittance_IdOrderByAffectationExterneIdAsc(
                        quittanceId()
                )).thenReturn(sauvegarder(document, paiementsComplets()));
    }

    @Test
    void bailleurProprietaireEtJwtCommunSansIssuerSontAcceptes()
            throws Exception {
        var authentication = authentification("BAILLEUR", BAILLEUR_ID);
        assertNull(authentication.getToken().getClaim("iss"));

        service("COMMUN", "INTEGRATION", referentielsIntegres())
                .verifier(authentication, quittanceId());

        assertEquals("EN_ATTENTE_SIGNATURE", document.getStatut().name());
        assertNull(document.getNumero());
        verify(biensBaux).consulterBail(
                BAIL_ID,
                PERIODE_DEBUT,
                PERIODE_FIN
        );
        verify(paiements).consulterAffectations(
                BAIL_ID,
                PERIODE_DEBUT,
                PERIODE_FIN
        );
    }

    @Test
    void modeUtilisateurAvecSourcesReellesPermetLePrecontrole() throws Exception {
        var endpoints = new ReferentielsProperties(
                new ReferentielsProperties.Endpoint(
                        "UTILISATEUR", "https://locataires.example.test",
                        "/api/v1/locataires/{id}/quittance-summary", ""),
                new ReferentielsProperties.Endpoint(
                        "UTILISATEUR", "https://biens.example.test",
                        "/api/biens/{id}", ""),
                new ReferentielsProperties.Endpoint(
                        "UTILISATEUR", "https://baux.example.test",
                        "/api/v1/baux/{id}/situation?debut={debut}&fin={fin}", "")
        );

        service("COMMUN", "UTILISATEUR", endpoints)
                .verifier(authentification("BAILLEUR", BAILLEUR_ID), quittanceId());

        verify(biensBaux).consulterBail(BAIL_ID, PERIODE_DEBUT, PERIODE_FIN);
        verify(paiements).consulterAffectations(BAIL_ID, PERIODE_DEBUT, PERIODE_FIN);
    }

    @Test
    void modeUtilisateurContinueDeRefuserLesSourcesSimulees() throws Exception {
        var endpoints = new ReferentielsProperties(
                new ReferentielsProperties.Endpoint(
                        "UTILISATEUR", "https://locataires.example.test",
                        "/api/v1/locataires/{id}/quittance-summary", ""),
                new ReferentielsProperties.Endpoint(
                        "SIMULATION", "http://127.0.0.1:8093",
                        "/api/biens/{id}", ""),
                new ReferentielsProperties.Endpoint(
                        "UTILISATEUR", "https://baux.example.test",
                        "/api/v1/baux/{id}/situation?debut={debut}&fin={fin}", "")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class,
                        () -> service("COMMUN", "UTILISATEUR", endpoints)
                                .verifier(authentification("BAILLEUR", BAILLEUR_ID),
                                        quittanceId()))
                        .getStatusCode());
        verifyNoInteractions(quittances, biensBaux, locataires, paiements);
    }
    @Test
    void autreBailleurNePeutPasVerifierLeDocument() throws Exception {
        when(quittances.findByIdAndBailleurId(
                quittanceId(),
                AUTRE_BAILLEUR_ID
        )).thenReturn(Optional.empty());

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service("COMMUN", "INTEGRATION", referentielsIntegres())
                        .verifier(
                                authentification(
                                        "BAILLEUR",
                                        AUTRE_BAILLEUR_ID
                                ),
                                quittanceId()
                        )
        );

        assertEquals(HttpStatus.NOT_FOUND, erreur.getStatusCode());
        verifyNoInteractions(biensBaux, locataires, paiements, affectations);
    }

    @Test
    void gestionnairePeutVerifierDocumentAutreBailleur() throws Exception {
        String gestionnaireId =
                "22222222-2222-2222-2222-222222222222";

        when(quittances.findById(quittanceId()))
                .thenReturn(Optional.of(document));

        var authentication = authentification(
                "GESTIONNAIRE",
                gestionnaireId
        );

        service("COMMUN", "INTEGRATION", referentielsIntegres())
                .verifier(authentication, quittanceId());

        verify(quittances).findById(quittanceId());

        verify(biensBaux).consulterBail(
                BAIL_ID,
                PERIODE_DEBUT,
                PERIODE_FIN
        );

        verify(paiements).consulterAffectations(
                BAIL_ID,
                PERIODE_DEBUT,
                PERIODE_FIN
        );

        assertEquals(BAILLEUR_ID, document.getBailleurId());
        assertEquals("EN_ATTENTE_SIGNATURE", document.getStatut().name());
        assertNull(document.getNumero());
    }

    @Test
    void locataireSansRattachementConfirmeEstRefuse() throws Exception {
        assertThrows(
                AccessDeniedException.class,
                () -> service("COMMUN", "INTEGRATION", referentielsIntegres())
                        .verifier(
                                authentification(
                                        "LOCATAIRE",
                                        BAILLEUR_ID
                                ),
                                quittanceId()
                        )
        );

        verifyNoInteractions(quittances, biensBaux, locataires, paiements);
    }

    @Test
    void brouillonNonValideEstRefuse() throws Exception {
        Quittance brouillon = Quittance.creerBrouillon(
                BAIL_ID,
                LOCATAIRE_ID,
                BIEN_ID,
                BAILLEUR_ID,
                PERIODE_DEBUT,
                PERIODE_FIN,
                montant("3000.00"),
                montant("200.00"),
                montant("3000.00"),
                montant("200.00"),
                TypeDocument.QUITTANCE_INTEGRALE,
                instantane()
        );
        when(quittances.findByIdAndBailleurId(
                quittanceId(),
                BAILLEUR_ID
        )).thenReturn(Optional.of(brouillon));

        assertEquals(
                HttpStatus.CONFLICT,
                assertThrows(
                        ResponseStatusException.class,
                        () -> service(
                                "COMMUN",
                                "INTEGRATION",
                                referentielsIntegres()
                        ).verifier(
                                authentification("BAILLEUR", BAILLEUR_ID),
                                quittanceId()
                        )
                ).getStatusCode()
        );
        verifyNoInteractions(biensBaux, locataires, paiements);
    }

    @Test
    void recuPartielPersisteNePassePasLePrecontrole() throws Exception {
        Quittance recuPartiel = quittance(
                BAILLEUR_ID,
                TypeDocument.RECU_PARTIEL,
                "1500.00",
                "0.00"
        );
        when(quittances.findByIdAndBailleurId(
                quittanceId(),
                BAILLEUR_ID
        )).thenReturn(Optional.of(recuPartiel));

        assertEquals(
                HttpStatus.CONFLICT,
                assertThrows(
                        ResponseStatusException.class,
                        () -> service(
                                "COMMUN",
                                "INTEGRATION",
                                referentielsIntegres()
                        ).verifier(
                                authentification("BAILLEUR", BAILLEUR_ID),
                                quittanceId()
                        )
                ).getStatusCode()
        );
        verifyNoInteractions(biensBaux, locataires, paiements);
    }

    @Test
    void quittanceDejaEmiseEstRefusee() throws Exception {
        document.emettreDemonstration(
                "DEMO-QT-2026-000001",
                Instant.now(),
                "a".repeat(64)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                assertThrows(
                        ResponseStatusException.class,
                        () -> service(
                                "COMMUN",
                                "INTEGRATION",
                                referentielsIntegres()
                        ).verifier(
                                authentification("BAILLEUR", BAILLEUR_ID),
                                quittanceId()
                        )
                ).getStatusCode()
        );
        verifyNoInteractions(biensBaux, locataires, paiements);
    }

    @Test
    void paiementPartielNePeutPasValiderUneQuittanceIntegrale()
            throws Exception {
        when(paiements.consulterAffectations(
                BAIL_ID,
                PERIODE_DEBUT,
                PERIODE_FIN
        )).thenReturn(List.of(affectation(
                "AFF-LOYER-PARTIEL",
                "PAY-LOYER-PARTIEL",
                Nature.LOYER,
                "1500.00",
                StatutPaiement.CONFIRME
        )));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service("COMMUN", "INTEGRATION", referentielsIntegres())
                        .verifier(
                                authentification("BAILLEUR", BAILLEUR_ID),
                                quittanceId()
                        )
        );

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
    }

    @Test
    void paiementsEnAttenteOuRemboursesSontRefuses() throws Exception {
        for (StatutPaiement statut : List.of(
                StatutPaiement.EN_ATTENTE,
                StatutPaiement.REMBOURSE
        )) {
            when(paiements.consulterAffectations(
                    BAIL_ID,
                    PERIODE_DEBUT,
                    PERIODE_FIN
            )).thenReturn(List.of(
                    affectation(
                            "AFF-LOYER-" + statut,
                            "PAY-LOYER-" + statut,
                            Nature.LOYER,
                            "3000.00",
                            statut
                    ),
                    affectation(
                            "AFF-CHARGES-" + statut,
                            "PAY-CHARGES-" + statut,
                            Nature.CHARGES,
                            "200.00",
                            statut
                    )
            ));

            ResponseStatusException erreur = assertThrows(
                    ResponseStatusException.class,
                    () -> service(
                            "COMMUN",
                            "INTEGRATION",
                            referentielsIntegres()
                    ).verifier(
                            authentification("BAILLEUR", BAILLEUR_ID),
                            quittanceId()
                    )
            );
            assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
        }
    }

    @Test
    void preuveDeProprieteDuBienAbsenteEstRefusee() throws Exception {
        when(biensBaux.consulterBien(BIEN_ID)).thenReturn(
                new Bien("1", BIEN_ID, ADRESSE, null)
        );

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service("COMMUN", "INTEGRATION", referentielsIntegres())
                        .verifier(
                                authentification("BAILLEUR", BAILLEUR_ID),
                                quittanceId()
                        )
        );

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
    }

    @Test
    void affectationsEnregistreesAbsentesSontRefusees() throws Exception {
        when(affectations
                .findByQuittance_IdOrderByAffectationExterneIdAsc(
                        quittanceId()
                )).thenReturn(List.of());

        assertEquals(
                HttpStatus.CONFLICT,
                assertThrows(
                        ResponseStatusException.class,
                        () -> service(
                                "COMMUN",
                                "INTEGRATION",
                                referentielsIntegres()
                        ).verifier(
                                authentification("BAILLEUR", BAILLEUR_ID),
                                quittanceId()
                        )
                ).getStatusCode()
        );
    }

    @Test
    void changementDuLoyerRendInstantaneObsolete() throws Exception {
        when(biensBaux.consulterBail(
                BAIL_ID,
                PERIODE_DEBUT,
                PERIODE_FIN
        )).thenReturn(bail(BAILLEUR_ID, "3100.00", "200.00"));
        when(paiements.consulterAffectations(
                BAIL_ID,
                PERIODE_DEBUT,
                PERIODE_FIN
        )).thenReturn(List.of(
                affectation(
                        "AFF-LOYER-ACTUEL",
                        "PAY-LOYER-ACTUEL",
                        Nature.LOYER,
                        "3100.00",
                        StatutPaiement.CONFIRME
                ),
                affectation(
                        "AFF-CHARGES-ACTUELLES",
                        "PAY-CHARGES-ACTUELLES",
                        Nature.CHARGES,
                        "200.00",
                        StatutPaiement.CONFIRME
                )
        ));

        assertEquals(
                HttpStatus.CONFLICT,
                assertThrows(
                        ResponseStatusException.class,
                        () -> service(
                                "COMMUN",
                                "INTEGRATION",
                                referentielsIntegres()
                        ).verifier(
                                authentification("BAILLEUR", BAILLEUR_ID),
                                quittanceId()
                        )
                ).getStatusCode()
        );
    }

    @Test
    void indisponibiliteExterneRefuseLePrecontrole() throws Exception {
        when(paiements.consulterAffectations(
                BAIL_ID,
                PERIODE_DEBUT,
                PERIODE_FIN
        )).thenThrow(new ServiceExterneException(
                "PAIEMENTS",
                "SERVICE_INJOIGNABLE",
                "Service de test indisponible.",
                null
        ));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service("COMMUN", "INTEGRATION", referentielsIntegres())
                        .verifier(
                                authentification("BAILLEUR", BAILLEUR_ID),
                                quittanceId()
                        )
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                erreur.getStatusCode()
        );
    }

    @Test
    void simulationDesFournisseursEstRefuseeEnModeCommun()
            throws Exception {
        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(
                        ResponseStatusException.class,
                        () -> service(
                                "COMMUN",
                                "SIMULATION",
                                referentielsIntegres()
                        ).verifier(
                                authentification("BAILLEUR", BAILLEUR_ID),
                                quittanceId()
                        )
                ).getStatusCode()
        );
        verifyNoInteractions(quittances, biensBaux, locataires, paiements);
    }

    @Test
    void lesModesLocalEtIntegrationNePeuventPasUtiliserCePrecontrole()
            throws Exception {
        for (String mode : List.of("LOCAL", "INTEGRATION")) {
            assertEquals(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    assertThrows(
                            ResponseStatusException.class,
                            () -> service(
                                    mode,
                                    "INTEGRATION",
                                    referentielsIntegres()
                            ).verifier(
                                    authentification("BAILLEUR", BAILLEUR_ID),
                                    quittanceId()
                            )
                    ).getStatusCode()
            );
        }
        verifyNoInteractions(quittances, biensBaux, locataires, paiements);
    }

    private ControlePrealableEmissionService service(
            String modeJwt,
            String modePaiements,
            ReferentielsProperties properties
    ) {
        return new ControlePrealableEmissionService(
                quittances,
                affectations,
                construction,
                new IdentiteAudit(modeJwt),
                properties,
                modeJwt,
                modePaiements
        );
    }

    private static ReferentielsProperties referentielsIntegres() {
        return new ReferentielsProperties(
                endpoint("https://locataires.example.test"),
                endpoint("https://biens.example.test"),
                endpoint("https://baux.example.test")
        );
    }

    private static ReferentielsProperties.Endpoint endpoint(String url) {
        return new ReferentielsProperties.Endpoint(
                "INTEGRATION",
                url,
                "/api/v1/{id}",
                "jeton-de-test"
        );
    }

    private static Quittance quittance(
            String bailleur,
            TypeDocument type,
            String loyerPaye,
            String chargesPayees
    ) {
        Quittance quittance = Quittance.creerBrouillon(
                BAIL_ID,
                LOCATAIRE_ID,
                BIEN_ID,
                bailleur,
                PERIODE_DEBUT,
                PERIODE_FIN,
                montant("3000.00"),
                montant("200.00"),
                montant(loyerPaye),
                montant(chargesPayees),
                type,
                instantane()
        );
        quittance.validerPourSignature();
        return quittance;
    }

    private static Map<String, String> instantane() {
        return Map.of(
                "versionSnapshot", "1",
                "referenceBail", BAIL_ID,
                "bailleurNom", "Bailleur de test",
                "locataireNom", "Locataire",
                "locatairePrenom", "Test",
                "bienAdresse", ADRESSE,
                "debutBail", DEBUT_BAIL.toString()
        );
    }

    private static BailPeriode bail(
            String bailleur,
            String loyer,
            String charges
    ) {
        return new BailPeriode(
                "1",
                BAIL_ID,
                LOCATAIRE_ID,
                BIEN_ID,
                bailleur,
                "Bailleur de test",
                DEBUT_BAIL,
                null,
                PERIODE_DEBUT,
                PERIODE_FIN,
                montant(loyer),
                montant(charges),
                "MAD"
        );
    }

    private static List<Affectation> paiementsComplets() {
        return List.of(
                affectation(
                        "AFF-LOYER",
                        "PAY-LOYER",
                        Nature.LOYER,
                        "3000.00",
                        StatutPaiement.CONFIRME
                ),
                affectation(
                        "AFF-CHARGES",
                        "PAY-CHARGES",
                        Nature.CHARGES,
                        "200.00",
                        StatutPaiement.CONFIRME
                )
        );
    }

    private static Affectation affectation(
            String affectationId,
            String paiementId,
            Nature nature,
            String montant,
            StatutPaiement statut
    ) {
        return new Affectation(
                affectationId,
                paiementId,
                BAIL_ID,
                PERIODE_DEBUT,
                PERIODE_FIN,
                nature,
                montant(montant),
                "MAD",
                statut
        );
    }

    private static List<PaiementAffectation> sauvegarder(
            Quittance quittance,
            List<Affectation> sources
    ) {
        return sources.stream()
                .map(source -> PaiementAffectation.copier(quittance, source))
                .toList();
    }

    private static BigDecimal montant(String valeur) {
        return new BigDecimal(valeur);
    }

    private static UUID quittanceId() {
        return UUID.fromString("67a8318c-27f0-4bfc-a0d8-fd099a9f0762");
    }

    private static JwtAuthenticationToken authentification(
            String role,
            String sujet
    ) throws Exception {
        byte[] secret = SECRET.getBytes(StandardCharsets.UTF_8);
        NimbusJwtDecoder delegate = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(secret, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        ValidationJwt.configurerCommun(delegate);

        Instant maintenant = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(sujet)
                .claim("role", role)
                .issueTime(Date.from(maintenant.minusSeconds(30)))
                .expirationTime(Date.from(maintenant.plusSeconds(600)))
                .build();
        SignedJWT token = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims
        );
        token.sign(new MACSigner(secret));

        Jwt verifie = new DecodeurJwtCommun(delegate).decode(
                token.serialize()
        );
        return new JwtAuthenticationToken(
                verifie,
                SecurityConfiguration.autoritesRoleCommun(verifie)
        );
    }
}
