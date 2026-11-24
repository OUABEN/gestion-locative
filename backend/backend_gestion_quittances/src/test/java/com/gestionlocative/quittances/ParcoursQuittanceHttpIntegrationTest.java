package com.gestionlocative.quittances;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestionlocative.quittances.client.BienBailClient;
import com.gestionlocative.quittances.dto.ReferentielsExternes.BailPeriode;
import com.gestionlocative.quittances.dto.ReferentielsExternes.Bien;
import com.gestionlocative.quittances.dto.SituationPeriode.Affectation;
import com.gestionlocative.quittances.dto.SituationPeriode.Nature;
import com.gestionlocative.quittances.dto.SituationPeriode.StatutPaiement;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ParcoursQuittanceHttpIntegrationTest {

    private static final String SECRET =
            "secret-fictif-parcours-quittances-32-octets";
    private static final LocalDate DEBUT = LocalDate.of(2026, 10, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 10, 31);
    private static final String BAIL_ID = "BAIL-TEST";
    private static final String BIEN_ID = "BIEN-TEST";
    private static final String LOCATAIRE_ID = "LOC-TEST";
    private static final AtomicReference<List<Affectation>>
            PAIEMENTS_SIMULES = new AtomicReference<>(List.of());
    private static final Path STOCKAGE_TEMPORAIRE = Path.of(
            System.getProperty("java.io.tmpdir"),
            "quittances-e2e-" + UUID.randomUUID()
    );
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static HttpServer serveurFournisseurs;

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private BienBailClient biensBaux;

    private String bailleurId;

    @DynamicPropertySource
    static void proprietesTest(DynamicPropertyRegistry proprietes) {
        String hoteBaseTest = variableEnvironnementRequise(
                "QUITTANCES_DB_HOST"
        );
        if (!"127.0.0.1".equals(hoteBaseTest)
                && !"localhost".equalsIgnoreCase(hoteBaseTest)) {
            throw new IllegalStateException(
                    "Le test d'intégration exige une base PostgreSQL locale "
                            + "isolée."
            );
        }

        String portBaseTest = variableEnvironnementRequise(
                "QUITTANCES_DB_PORT"
        );
        int port = Integer.parseInt(portBaseTest);
        if (port < 1 || port > 65535) {
            throw new IllegalStateException(
                    "QUITTANCES_DB_PORT doit désigner un port valide."
            );
        }

        String nomBaseTest = variableEnvironnementRequise(
                "QUITTANCES_DB_NAME"
        );
        if (!nomBaseTest.matches("(?i)[a-z0-9_]+_test")) {
            throw new IllegalStateException(
                    "Le test d'intégration exige une base dédiée dont le nom "
                            + "se termine par _test."
            );
        }

        proprietes.add(
                "spring.datasource.url",
                () -> "jdbc:postgresql://" + hoteBaseTest + ":" + port
                        + "/" + nomBaseTest
        );
        proprietes.add(
                "spring.datasource.username",
                () -> variableEnvironnementRequise("QUITTANCES_DB_USER")
        );
        proprietes.add(
                "spring.datasource.password",
                () -> variableEnvironnementRequise("QUITTANCES_DB_PASSWORD")
        );

        demarrerFournisseursHttp();
        proprietes.add("quittances.securite.mode", () -> "COMMUN");
        proprietes.add("quittances.securite.jwt-secret", () -> SECRET);
        proprietes.add(
                "quittances.stockage.repertoire",
                () -> STOCKAGE_TEMPORAIRE.toString()
        );
        proprietes.add(
                "quittances.referentiels.locataires.mode",
                () -> "SIMULATION"
        );
        proprietes.add(
                "quittances.referentiels.locataires.base-url",
                ParcoursQuittanceHttpIntegrationTest::urlFournisseurs
        );
        proprietes.add(
                "quittances.referentiels.locataires.chemin",
                () -> "/api/v1/locataires/{id}"
        );
        proprietes.add("quittances.referentiels.locataires.token", () -> "");
        proprietes.add(
                "quittances.integration.paiements.mode",
                () -> "SIMULATION"
        );
        proprietes.add(
                "quittances.integration.paiements.base-url",
                ParcoursQuittanceHttpIntegrationTest::urlFournisseurs
        );
        proprietes.add(
                "quittances.integration.paiements.chemin",
                () -> "/api/v1/paiements/affectations"
                        + "?bailId={bailId}&debut={debut}&fin={fin}"
        );
        proprietes.add("quittances.integration.paiements.token", () -> "");
    }

    private static String variableEnvironnementRequise(String nom) {
        String valeur = System.getenv(nom);
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalStateException(
                    "Définir " + nom + " pour lancer le test d'intégration."
            );
        }
        return valeur;
    }

    @BeforeEach
    void preparerFournisseursMetierSimules() {
        bailleurId = UUID.randomUUID().toString();

        when(biensBaux.consulterBail(BAIL_ID, DEBUT, FIN))
                .thenReturn(new BailPeriode(
                        "1",
                        BAIL_ID,
                        LOCATAIRE_ID,
                        BIEN_ID,
                        bailleurId,
                        "Bailleur de test",
                        LocalDate.of(2026, 1, 1),
                        null,
                        DEBUT,
                        FIN,
                        new BigDecimal("3000.00"),
                        new BigDecimal("200.00"),
                        "MAD"
                ));
        when(biensBaux.consulterBien(BIEN_ID))
                .thenReturn(new Bien(
                        "1",
                        BIEN_ID,
                        "12 rue de test, Agadir",
                        bailleurId
                ));
        PAIEMENTS_SIMULES.set(paiementsComplets());
    }

    @AfterEach
    void nettoyerDonneesDuTest() {
        if (bailleurId == null) {
            return;
        }

        jdbc.update("""
                DELETE FROM document_fichier
                WHERE quittance_id IN (
                    SELECT id FROM quittance WHERE bail_id = ?
                )
                """, BAIL_ID);
        jdbc.update(
                "DELETE FROM quittance_audit WHERE quittance_id IN "
                        + "(SELECT id FROM quittance WHERE bail_id = ?)",
                BAIL_ID
        );
        jdbc.update(
                "DELETE FROM quittance_validation WHERE quittance_id IN "
                        + "(SELECT id FROM quittance WHERE bail_id = ?)",
                BAIL_ID
        );
        jdbc.update(
                "DELETE FROM paiement_affectation WHERE quittance_id IN "
                        + "(SELECT id FROM quittance WHERE bail_id = ?)",
                BAIL_ID
        );
        jdbc.update("DELETE FROM quittance WHERE bail_id = ?", BAIL_ID);
    }

    @AfterAll
    static void nettoyerStockagePdfTemporaire() throws IOException {
        HTTP.close();
        if (serveurFournisseurs != null) {
            serveurFournisseurs.stop(0);
        }

        if (!Files.exists(STOCKAGE_TEMPORAIRE)) {
            return;
        }

        List<Path> chemins;
        try (Stream<Path> parcours = Files.walk(STOCKAGE_TEMPORAIRE)) {
            chemins = parcours.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path chemin : chemins) {
            Files.deleteIfExists(chemin);
        }
    }

    @Test
    void parcoursHttpCompletAvecJwtCommunEtProprieteDuBailleur()
            throws Exception {
        String jwt = signerJwt(bailleurId, "BAILLEUR");

        HttpResponse<byte[]> creation = appeler(
                "POST",
                "/api/v1/quittances",
                jwt,
                """
                {"bailId":"BAIL-TEST",
                 "periodeDebut":"2026-10-01",
                 "periodeFin":"2026-10-31"}
                """
        );
        assertEquals(201, creation.statusCode());

        JsonNode brouillon = objectMapper.readTree(creation.body());
        UUID quittanceId = UUID.fromString(brouillon.get("id").asText());
        assertEquals("BROUILLON", brouillon.get("statut").asText());
        assertEquals("NON_SIGNE", brouillon.get("statutSignature").asText());

        HttpResponse<byte[]> doublon = appeler(
                "POST",
                "/api/v1/quittances",
                jwt,
                """
                {"bailId":"BAIL-TEST",
                 "periodeDebut":"2026-10-01",
                 "periodeFin":"2026-10-31"}
                """
        );
        assertEquals(409, doublon.statusCode());
        assertTrue(new String(doublon.body(), StandardCharsets.UTF_8)
                .contains("QUITTANCE_DEJA_EXISTANTE"));

        HttpResponse<byte[]> consultation = appeler(
                "GET",
                "/api/v1/quittances/" + quittanceId,
                jwt,
                null
        );
        assertEquals(200, consultation.statusCode());
        JsonNode document = objectMapper.readTree(consultation.body());
        assertEquals("QUITTANCE_INTEGRALE", document.get("typeDocument").asText());
        assertEquals(
                0,
                new BigDecimal("3000.00")
                        .compareTo(document.get("loyerPaye").decimalValue())
        );
        assertEquals(
                0,
                new BigDecimal("200.00")
                        .compareTo(document.get("chargesPayees").decimalValue())
        );
        assertEquals(
                0,
                new BigDecimal("3200.00")
                        .compareTo(document.get("totalPaye").decimalValue())
        );

        HttpResponse<byte[]> validation = appeler(
                "POST",
                "/api/v1/quittances/" + quittanceId + "/valider",
                jwt,
                "{\"confirmation\":true}"
        );
        assertEquals(200, validation.statusCode());
        JsonNode resultatValidation = objectMapper.readTree(validation.body());
        assertEquals(
                "EN_ATTENTE_SIGNATURE",
                resultatValidation.get("statut").asText()
        );
        JsonNode documentValide = objectMapper.readTree(
                appeler(
                        "GET",
                        "/api/v1/quittances/" + quittanceId,
                        jwt,
                        null
                ).body()
        );
        assertEquals(
                "EN_ATTENTE_SIGNATURE",
                documentValide.get("statut").asText()
        );
        assertTrue(documentValide.get("numero").isNull());
        assertTrue(documentValide.get("emisLe").isNull());

        assertEquals(
                bailleurId,
                jdbc.queryForObject(
                        "SELECT acteur FROM quittance_validation "
                                + "WHERE quittance_id = ?",
                        String.class,
                        quittanceId
                )
        );
        assertEquals(
                "JWT_COMMUN",
                jdbc.queryForObject(
                        "SELECT emetteur_identite FROM quittance_validation "
                                + "WHERE quittance_id = ?",
                        String.class,
                        quittanceId
                )
        );

        HttpResponse<byte[]> archive = appeler(
                "POST",
                "/api/v1/quittances/" + quittanceId
                        + "/archives-brouillon",
                jwt,
                null
        );
        assertEquals(201, archive.statusCode());
        JsonNode pdfBrouillon = objectMapper.readTree(archive.body());
        UUID archiveId = UUID.fromString(pdfBrouillon.get("id").asText());
        assertEquals("BROUILLON", pdfBrouillon.get("nature").asText());

        String referenceStockage = jdbc.queryForObject(
                "SELECT reference_stockage FROM document_fichier WHERE id = ?",
                String.class,
                archiveId
        );
        Path pdfStocke = STOCKAGE_TEMPORAIRE.resolve(referenceStockage)
                .resolve("document.pdf");
        assertTrue(Files.isRegularFile(pdfStocke));

        HttpResponse<byte[]> pdf = appeler(
                "GET",
                "/api/v1/quittances/" + quittanceId
                        + "/archives-brouillon/" + archiveId + "/pdf",
                jwt,
                null
        );
        assertEquals(
                200,
                pdf.statusCode(),
                new String(pdf.body(), StandardCharsets.UTF_8)
        );
        assertTrue(pdf.headers().firstValue("Content-Type")
                .orElse("")
                .startsWith("application/pdf"));
        assertTrue(new String(pdf.body(), 0, 4, StandardCharsets.US_ASCII)
                .startsWith("%PDF"));
        assertEquals(Files.size(pdfStocke), pdf.body().length);

        assertEquals(404, appeler(
                "GET",
                "/api/v1/quittances/" + quittanceId,
                signerJwt(UUID.randomUUID().toString(), "BAILLEUR"),
                null
        ).statusCode());
        assertEquals(404, appeler(
                "GET",
                "/api/v1/quittances/" + quittanceId
                        + "/archives-brouillon/" + archiveId + "/pdf",
                signerJwt(UUID.randomUUID().toString(), "BAILLEUR"),
                null
        ).statusCode());
        String jwtGestionnaire =
                signerJwt(UUID.randomUUID().toString(), "GESTIONNAIRE");

        assertEquals(200, appeler(
                "GET",
                "/api/v1/quittances",
                jwtGestionnaire,
                null
        ).statusCode());

        assertEquals(200, appeler(
                "GET",
                "/api/v1/quittances/" + quittanceId,
                jwtGestionnaire,
                null
        ).statusCode());
        assertEquals(403, appeler(
                "GET",
                "/api/v1/quittances",
                signerJwt(UUID.randomUUID().toString(), "LOCATAIRE"),
                null
        ).statusCode());

        assertEquals(
                2L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM quittance_audit "
                                + "WHERE quittance_id = ? AND acteur = ? "
                                + "AND emetteur_identite = 'JWT_COMMUN'",
                        Long.class,
                        quittanceId,
                        bailleurId
                )
        );
    }

    @Test
    void paiementIncompletProduitUnRecuPartielEtPasUneQuittanceIntegrale()
            throws Exception {
        PAIEMENTS_SIMULES.set(List.of(
                affectation(
                        "AFF-LOYER-PARTIEL",
                        Nature.LOYER,
                        "1500.00",
                        StatutPaiement.CONFIRME
                ),
                affectation(
                        "AFF-CHARGES-EN-ATTENTE",
                        Nature.CHARGES,
                        "200.00",
                        StatutPaiement.EN_ATTENTE
                )
        ));

        String jwt = signerJwt(bailleurId, "BAILLEUR");
        HttpResponse<byte[]> creation = appeler(
                "POST",
                "/api/v1/quittances",
                jwt,
                """
                {"bailId":"BAIL-TEST",
                 "periodeDebut":"2026-10-01",
                 "periodeFin":"2026-10-31"}
                """
        );
        assertEquals(201, creation.statusCode());

        JsonNode brouillon = objectMapper.readTree(creation.body());
        UUID quittanceId = UUID.fromString(brouillon.get("id").asText());
        HttpResponse<byte[]> consultation = appeler(
                "GET",
                "/api/v1/quittances/" + quittanceId,
                jwt,
                null
        );
        assertEquals(200, consultation.statusCode());

        JsonNode document = objectMapper.readTree(consultation.body());
        assertEquals("RECU_PARTIEL", document.get("typeDocument").asText());
        assertEquals("BROUILLON", document.get("statut").asText());
        assertTrue(document.get("numero").isNull());
        assertTrue(document.get("emisLe").isNull());
        assertEquals(
                0,
                new BigDecimal("1500.00")
                        .compareTo(document.get("totalPaye").decimalValue())
        );
        assertEquals(
                0,
                new BigDecimal("0.00")
                        .compareTo(document.get("chargesPayees").decimalValue())
        );
    }

    @Test
    void paiementsNonConfirmesNePermettentPasDePreparerUnDocument()
            throws Exception {
        PAIEMENTS_SIMULES.set(List.of(
                affectation(
                        "AFF-LOYER-EN-ATTENTE",
                        Nature.LOYER,
                        "3000.00",
                        StatutPaiement.EN_ATTENTE
                ),
                affectation(
                        "AFF-CHARGES-REMBOURSEES",
                        Nature.CHARGES,
                        "200.00",
                        StatutPaiement.REMBOURSE
                )
        ));

        HttpResponse<byte[]> creation = appeler(
                "POST",
                "/api/v1/quittances",
                signerJwt(bailleurId, "BAILLEUR"),
                """
                {"bailId":"BAIL-TEST",
                 "periodeDebut":"2026-10-01",
                 "periodeFin":"2026-10-31"}
                """
        );

        assertEquals(422, creation.statusCode());
        assertTrue(new String(creation.body(), StandardCharsets.UTF_8)
                .contains("AUCUN_PAIEMENT_CONFIRME"));
        assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM quittance WHERE bail_id = ?",
                        Integer.class,
                        BAIL_ID
                )
        );
    }

    private static List<Affectation> paiementsComplets() {
        return List.of(
                affectation(
                        "AFF-LOYER",
                        Nature.LOYER,
                        "3000.00",
                        StatutPaiement.CONFIRME
                ),
                affectation(
                        "AFF-CHARGES",
                        Nature.CHARGES,
                        "200.00",
                        StatutPaiement.CONFIRME
                )
        );
    }

    private static Affectation affectation(
            String id,
            Nature nature,
            String montant,
            StatutPaiement statut
    ) {
        return new Affectation(
                id,
                "PAY-" + id,
                BAIL_ID,
                DEBUT,
                FIN,
                nature,
                new BigDecimal(montant),
                "MAD",
                statut
        );
    }

    private String signerJwt(String sub, String role) throws Exception {
        Instant maintenant = Instant.now();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder()
                        .subject(sub)
                        .claim("role", role)
                        .issueTime(Date.from(maintenant.minusSeconds(30)))
                        .expirationTime(Date.from(maintenant.plusSeconds(600)))
                        .build()
        );
        jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    private static synchronized void demarrerFournisseursHttp() {
        if (serveurFournisseurs != null) {
            return;
        }

        try {
            serveurFournisseurs = HttpServer.create(
                    new InetSocketAddress("127.0.0.1", 0),
                    0
            );
            serveurFournisseurs.createContext(
                    "/api/v1/locataires/",
                    ParcoursQuittanceHttpIntegrationTest::repondreLocataire
            );
            serveurFournisseurs.createContext(
                    "/api/v1/paiements/affectations",
                    ParcoursQuittanceHttpIntegrationTest::repondrePaiements
            );
            serveurFournisseurs.start();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Impossible de démarrer les fournisseurs HTTP de test.",
                    exception
            );
        }
    }

    private static String urlFournisseurs() {
        return "http://127.0.0.1:"
                + serveurFournisseurs.getAddress().getPort();
    }

    private static void repondreLocataire(HttpExchange exchange)
            throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())
                || !("/api/v1/locataires/" + LOCATAIRE_ID)
                        .equals(exchange.getRequestURI().getPath())) {
            repondre(exchange, 404, "{}");
            return;
        }

        repondre(exchange, 200, """
                {
                  "versionContrat": "1",
                  "id": "LOC-TEST",
                  "nom": "Locataire",
                  "prenom": "Test",
                  "email": null,
                  "emailVerifie": false
                }
                """);
    }

    private static void repondrePaiements(HttpExchange exchange)
            throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        if (!"GET".equals(exchange.getRequestMethod())
                || query == null
                || !query.contains("bailId=" + BAIL_ID)
                || !query.contains("debut=" + DEBUT)
                || !query.contains("fin=" + FIN)) {
            repondre(exchange, 400, "{}");
            return;
        }

        StringBuilder json = new StringBuilder("""
                {
                  "versionContrat": "1",
                  "complet": true,
                  "bailId": "BAIL-TEST",
                  "periodeDebut": "2026-10-01",
                  "periodeFin": "2026-10-31",
                  "affectations": [
                """);
        List<Affectation> affectations = PAIEMENTS_SIMULES.get();
        for (int index = 0; index < affectations.size(); index++) {
            Affectation affectation = affectations.get(index);
            if (index > 0) {
                json.append(',');
            }
            json.append("""
                    {
                      "affectationId": "%s",
                      "paiementId": "%s",
                      "bailId": "%s",
                      "periodeDebut": "%s",
                      "periodeFin": "%s",
                      "nature": "%s",
                      "montant": %s,
                      "devise": "%s",
                      "statut": "%s"
                    }
                    """.formatted(
                    affectation.affectationId(),
                    affectation.paiementId(),
                    affectation.bailId(),
                    affectation.periodeDebut(),
                    affectation.periodeFin(),
                    affectation.nature(),
                    affectation.montant().toPlainString(),
                    affectation.devise(),
                    affectation.statut()
            ));
        }
        json.append("]}");
        repondre(exchange, 200, json.toString());
    }

    private static void repondre(
            HttpExchange exchange,
            int statut,
            String corps
    ) throws IOException {
        byte[] contenu = corps.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );
        exchange.sendResponseHeaders(statut, contenu.length);
        try (OutputStream sortie = exchange.getResponseBody()) {
            sortie.write(contenu);
        } finally {
            exchange.close();
        }
    }

    private HttpResponse<byte[]> appeler(
            String methode,
            String chemin,
            String jwt,
            String corps
    ) throws Exception {
        HttpRequest.Builder requete = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + chemin))
                .timeout(java.time.Duration.ofSeconds(10))
                .header(
                        "Accept",
                        chemin.endsWith("/pdf")
                                ? "application/pdf"
                                : "application/json"
                );

        if (jwt != null) {
            requete.header("Authorization", "Bearer " + jwt);
        }
        if (corps != null) {
            requete.header("Content-Type", "application/json");
        }

        HttpRequest.BodyPublisher contenu = corps == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(corps);
        requete.method(methode, contenu);

        return HTTP.send(
                requete.build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );
    }
}
