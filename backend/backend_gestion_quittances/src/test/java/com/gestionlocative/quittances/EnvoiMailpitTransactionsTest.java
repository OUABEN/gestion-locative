package com.gestionlocative.quittances;

import com.gestionlocative.quittances.document.SignataireReel;
import com.gestionlocative.quittances.document.SignatureLocale;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.notification.EnvoiMailpitTransactions;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.service.EmissionReelleTransactions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Execution(ExecutionMode.SAME_THREAD)
class EnvoiMailpitTransactionsTest {

    @Autowired
    private EnvoiMailpitTransactions transactions;

    @Autowired
    private EmissionReelleTransactions emission;

    @Autowired
    private QuittanceRepository quittances;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private SignataireReel signataire;

    private TransactionTemplate transaction;
    private UUID id;
    private String bailleurId;

    @BeforeEach
    void preparer() {
        assertEquals("quittances_automatisation_test",
                jdbc.queryForObject(
                        "SELECT current_database()", String.class));

        transaction = new TransactionTemplate(transactionManager);
        bailleurId = UUID.randomUUID().toString();

        id = transaction.execute(status -> {
            Quittance document = Quittance.creerBrouillon(
                    "BAIL-MAIL-" + UUID.randomUUID(),
                    "LOC-MAIL-" + UUID.randomUUID(),
                    "BIEN-MAIL-" + UUID.randomUUID(),
                    bailleurId,
                    LocalDate.of(2026, 10, 1),
                    LocalDate.of(2026, 10, 31),
                    new BigDecimal("3000.00"),
                    new BigDecimal("200.00"),
                    new BigDecimal("3000.00"),
                    new BigDecimal("200.00"),
                    TypeDocument.QUITTANCE_INTEGRALE,
                    Map.of(
                            "versionSnapshot", "1",
                            "referenceBail", "BAIL-MAIL",
                            "debutBail", "2026-01-01",
                            "bailleurNom", "Bailleur de test",
                            "locataireNom", "Locataire de test",
                            "locatairePrenom", "Test",
                            "bienAdresse", "Adresse de test"));

            document.validerPourSignature();

            return quittances.saveAndFlush(document).getId();
        });
    }

    @AfterEach
    void nettoyer() {
        if (id == null || transaction == null) {
            return;
        }

        transaction.executeWithoutResult(status -> {
            jdbc.update(
                    "DELETE FROM quittance_envoi WHERE quittance_id=?", id);

            jdbc.update(
                    "DELETE FROM quittance_audit WHERE quittance_id=?", id);

            jdbc.update(
                    "DELETE FROM emission_reelle WHERE quittance_id=?", id);

            jdbc.update(
                    "DELETE FROM quittance WHERE id=?", id);
        });
    }

    private JwtAuthenticationToken identite(
            String proprietaire, String role) {

        Jwt jeton = Jwt.withTokenValue("jeton-test")
                .header("alg", "RS256")
                .subject(proprietaire)
                .claim("bailleur_id", proprietaire)
                .issuer("urn:gestionlocative:dev")
                .build();

        return new JwtAuthenticationToken(jeton,
                List.of(new SimpleGrantedAuthority(role)));
    }

    private JwtAuthenticationToken bailleur() {
        return identite(bailleurId, "ROLE_BAILLEUR");
    }

    private void emettre() throws Exception {
        var document = quittances.findById(id).orElseThrow();

        Instant date = Instant.now();

        Long sequence = jdbc.queryForObject(
                "SELECT nextval('numero_document_seq')", Long.class);

        String numero = "QT-" + date.atZone(ZoneOffset.UTC).getYear()
                + "-" + String.format(
                        java.util.Locale.ROOT, "%06d", sequence);

        byte[] pdf = "%PDF-1.7 test Mailpit"
                .getBytes(StandardCharsets.UTF_8);

        var preuve = new SignatureLocale.Preuve(
                new byte[256], new byte[]{1},
                SignatureLocale.empreinte(pdf));

        when(signataire.verifier(
                any(byte[].class),
                any(SignatureLocale.Preuve.class),
                any(Instant.class)))
                .thenReturn(true);

        emission.enregistrer(bailleur(), id,
                document.getVersion(), numero, date, pdf, preuve);
    }

    private long compterEnvois() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM quittance_envoi WHERE quittance_id=?",
                Long.class, id);
    }

    private EnvoiMailpitTransactions.Reservation reserver() {
        return transactions.reserver(
                bailleur(), id, UUID.randomUUID(), true);
    }

    @Test
    void reserveUnEnvoiReel() throws Exception {
        emettre();

        var reservation = reserver();

        assertTrue(reservation.nouveau());
        assertEquals("EN_ATTENTE_ENVOI",
                reservation.envoi().statut());

        assertEquals("locataire-" + id + "@example.test",
                reservation.envoi().destinataire());

        assertEquals(1L, compterEnvois());
    }

    @Test
    void memeRequeteRetourneLeMemeEnvoi() throws Exception {
        emettre();

        UUID requete = UUID.randomUUID();

        var premier = transactions.reserver(
                bailleur(), id, requete, true);

        var second = transactions.reserver(
                bailleur(), id, requete, true);

        assertTrue(premier.nouveau());
        assertFalse(second.nouveau());
        assertEquals(premier.envoi().id(), second.envoi().id());
        assertEquals(1L, compterEnvois());
    }

    @Test
    void refuseUnDocumentNonEmis() {
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        this::reserver).getStatusCode());

        assertEquals(0L, compterEnvois());
    }

    @Test
    void refuseUnDocumentReelEnModeDemo() throws Exception {
        emettre();

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        () -> transactions.reserver(
                                bailleur(), id, UUID.randomUUID()))
                        .getStatusCode());

        assertEquals(0L, compterEnvois());
    }

    @Test
    void refuseUneNouvelleRequeteSiEnvoiExistant() throws Exception {
        emettre();
        reserver();

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        this::reserver).getStatusCode());

        assertEquals(1L, compterEnvois());
    }

    @Test
    void termineEnvoiAvecSucces() throws Exception {
        emettre();

        var reservation = reserver();

        var resultat = transactions.terminer(
                bailleur(), id, reservation.envoi().id(),
                "ENVOYE", null);

        assertEquals("ENVOYE", resultat.statut());
        assertNotNull(resultat.termineLe());
        assertNull(resultat.erreurCode());

        assertEquals(1L, compterEnvois());
    }

    @Test
    void termineEnvoiAvecEchec() throws Exception {
        emettre();

        var reservation = reserver();

        var resultat = transactions.terminer(
                bailleur(), id, reservation.envoi().id(),
                "ECHEC_ENVOI", "PREPARATION_OU_CONNEXION");

        assertEquals("ECHEC_ENVOI", resultat.statut());
        assertEquals("PREPARATION_OU_CONNEXION",
                resultat.erreurCode());

        assertNotNull(resultat.termineLe());
    }

    @Test
    void refuseNouvelleTentativeTropRapide() throws Exception {
        emettre();

        var reservation = reserver();

        transactions.terminer(
                bailleur(), id, reservation.envoi().id(),
                "ECHEC_ENVOI", "PREPARATION_OU_CONNEXION");

        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                assertThrows(ResponseStatusException.class,
                        this::reserver).getStatusCode());

        assertEquals(1L, compterEnvois());
    }

    @Test
    void refuseAutreBailleur() {
        var autre = identite(
                UUID.randomUUID().toString(), "ROLE_BAILLEUR");

        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class,
                        () -> transactions.reserver(
                                autre, id, UUID.randomUUID(), true))
                        .getStatusCode());
    }

    @Test
    void refuseRoleLocataire() {
        var locataire = identite(bailleurId, "ROLE_LOCATAIRE");

        assertThrows(AccessDeniedException.class,
                () -> transactions.reserver(
                        locataire, id, UUID.randomUUID(), true));
    }

    @Test
    void listeHistoriqueDesEnvois() throws Exception {
        emettre();

        var reservation = reserver();

        var page = transactions.lister(bailleur(), id, 0, 10);

        assertEquals(1L, page.totalElements());
        assertEquals(1L, page.totalPages());
        assertEquals(1, page.contenu().size());

        assertEquals(reservation.envoi().id(),
                page.contenu().getFirst().id());
    }

    @Test
    void refusePaginationInvalide() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class,
                        () -> transactions.lister(
                                bailleur(), id, -1, 10))
                        .getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class,
                        () -> transactions.lister(
                                bailleur(), id, 0, 51))
                        .getStatusCode());
    }
}