package com.gestionlocative.quittances;

import com.gestionlocative.quittances.document.SignataireReel;
import com.gestionlocative.quittances.document.SignatureLocale;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.security.IdentiteAudit;
import com.gestionlocative.quittances.service.ConsultationQuittanceService;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
class EmissionReelleTransactionsTest {

    @Autowired
    private EmissionReelleTransactions transactions;

    @Autowired
    private ConsultationQuittanceService consultation;

    @Autowired
    private QuittanceRepository quittances;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private IdentiteAudit audit;

    @MockitoBean
    private SignataireReel signataire;

    private TransactionTemplate transaction;

    private UUID id;
    private String bailleurId;
    private Long version;
    private String numero;
    private Instant date;

    private byte[] pdf;
    private SignatureLocale.Preuve preuve;

    @BeforeEach
    void preparer() throws Exception {
        String base = jdbc.queryForObject(
                "SELECT current_database()", String.class);

        assertEquals("quittances_automatisation_test", base,
                "Ces tests doivent utiliser la base dediee.");

        transaction = new TransactionTemplate(transactionManager);
        bailleurId = UUID.randomUUID().toString();

        id = transaction.execute(status -> {
            Quittance document = Quittance.creerBrouillon(
                    "BAIL-REEL-" + UUID.randomUUID(),
                    "LOC-REEL-" + UUID.randomUUID(),
                    "BIEN-REEL-" + UUID.randomUUID(),
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
                            "referenceBail", "BAIL-REEL",
                            "debutBail", "2026-01-01",
                            "bailleurNom", "Bailleur de test",
                            "locataireNom", "Locataire de test",
                            "locatairePrenom", "Test",
                            "bienAdresse", "Adresse de test"
                    )
            );

            document.validerPourSignature();

            return quittances.saveAndFlush(document).getId();
        });

        version = document().getVersion();

        date = Instant.now();

        Long sequence = jdbc.queryForObject(
                "SELECT nextval('numero_document_seq')",
                Long.class);

        numero = "QT-" + date.atZone(ZoneOffset.UTC).getYear()
                + "-" + String.format(
                        java.util.Locale.ROOT, "%06d", sequence);

        pdf = "%PDF-1.7 document transactionnel de test"
                .getBytes(StandardCharsets.UTF_8);

        preuve = new SignatureLocale.Preuve(
                new byte[256],
                new byte[]{1, 2, 3},
                SignatureLocale.empreinte(pdf));

        when(signataire.verifier(
                any(byte[].class),
                any(SignatureLocale.Preuve.class),
                any(Instant.class)))
                .thenReturn(true);
    }

    @AfterEach
    void nettoyer() {
        if (transaction == null || id == null) {
            return;
        }

        transaction.executeWithoutResult(status -> {
            jdbc.update(
                    "DELETE FROM quittance_audit WHERE quittance_id=?",
                    id);

            jdbc.update(
                    "DELETE FROM emission_reelle WHERE quittance_id=?",
                    id);

            jdbc.update(
                    "DELETE FROM quittance WHERE id=?",
                    id);
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

        return new JwtAuthenticationToken(
                jeton,
                List.of(new SimpleGrantedAuthority(role)));
    }

    private JwtAuthenticationToken bailleur() {
        return identite(bailleurId, "ROLE_BAILLEUR");
    }

    private Quittance document() {
        return quittances.findById(id).orElseThrow();
    }

    private long nombreEmissions() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM emission_reelle WHERE quittance_id=?",
                Long.class, id);
    }

    private long nombreAudits() {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM quittance_audit
                WHERE quittance_id=?
                AND operation='EMISSION_REELLE'
                """, Long.class, id);
    }

    private EmissionReelleTransactions.Resultat emettre()
            throws Exception {
        return transactions.enregistrer(
                bailleur(), id, version,
                numero, date, pdf, preuve);
    }

    @Test
    void enregistreEmissionEtAudit() throws Exception {
        var resultat = emettre();

        assertEquals(id, resultat.id());
        assertEquals(numero, resultat.numero());
        assertEquals("EMIS", resultat.statut());
        assertEquals("SIGNE_VERIFIE", resultat.statutSignature());
        assertEquals("SIGNATURE_DETACHEE", resultat.modeSignature());
        assertEquals(preuve.sha256(), resultat.sha256());

        assertEquals("EMIS", document().getStatut().name());
        assertEquals(numero, document().getNumero());
        assertEquals("db-reelle:" + id, document().getPdfReference());
        assertEquals(preuve.sha256(), document().getPdfSha256());

        assertEquals(1L, nombreEmissions());
        assertEquals(1L, nombreAudits());
    }

    @Test
    void refuseDeuxiemeEmission() throws Exception {
        emettre();

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        () -> emettre()).getStatusCode());

        assertEquals(1L, nombreEmissions());
        assertEquals(1L, nombreAudits());
    }

    @Test
    void refuseVersionObsolete() {
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        () -> transactions.enregistrer(
                                bailleur(), id, version + 1,
                                numero, date, pdf, preuve))
                        .getStatusCode());

        assertEquals(0L, nombreEmissions());
    }

    @Test
    void refuseSignatureInvalide() throws Exception {
        when(signataire.verifier(
                any(byte[].class),
                any(SignatureLocale.Preuve.class),
                any(Instant.class)))
                .thenReturn(false);

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        () -> emettre()).getStatusCode());

        assertEquals(0L, nombreEmissions());
        assertEquals("EN_ATTENTE_SIGNATURE",
                document().getStatut().name());
    }

    @Test
    void refusePdfVide() {
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        () -> transactions.enregistrer(
                                bailleur(), id, version,
                                numero, date, new byte[0], preuve))
                        .getStatusCode());

        assertEquals(0L, nombreEmissions());
    }

    @Test
    void refusePdfAbsent() {
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        () -> transactions.enregistrer(
                                bailleur(), id, version,
                                numero, date, null, preuve))
                        .getStatusCode());

        assertEquals(0L, nombreEmissions());
    }

    @Test
    void refusePdfTropVolumineux() {
        byte[] tropGrand = new byte[10485761];

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        () -> transactions.enregistrer(
                                bailleur(), id, version,
                                numero, date, tropGrand, preuve))
                        .getStatusCode());

        assertEquals(0L, nombreEmissions());
    }

    @Test
    void refuseEmissionParAutreBailleur() {
        var autre = identite(
                UUID.randomUUID().toString(), "ROLE_BAILLEUR");

        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class,
                        () -> transactions.enregistrer(
                                autre, id, version,
                                numero, date, pdf, preuve))
                        .getStatusCode());

        assertEquals(0L, nombreEmissions());
    }

    @Test
    void echecAuditAnnuleTouteEmission() {
        var auth = bailleur();

        doThrow(new AccessDeniedException("Echec audit simule"))
                .when(audit).depuis(auth);

        assertThrows(AccessDeniedException.class,
                () -> transactions.enregistrer(
                        auth, id, version,
                        numero, date, pdf, preuve));

        assertEquals("EN_ATTENTE_SIGNATURE",
                document().getStatut().name());

        assertNull(document().getNumero());
        assertEquals(0L, nombreEmissions());
        assertEquals(0L, nombreAudits());
    }

    @Test
    void consulteLePdfEtLaPreuveEnregistres()
            throws Exception {
        emettre();

        var fichier = transactions.consulter(
                bailleur(), id, consultation);

        assertArrayEquals(pdf, fichier.pdf());
        assertEquals(numero, fichier.numero());

        assertArrayEquals(preuve.signature(),
                fichier.preuve().signature());

        assertArrayEquals(preuve.certificat(),
                fichier.preuve().certificat());

        assertEquals(preuve.sha256(),
                fichier.preuve().sha256());
    }

    @Test
    void refuseConsultationAvantEmission() {
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class,
                        () -> transactions.consulter(
                                bailleur(), id, consultation))
                        .getStatusCode());
    }

    @Test
    void refuseConsultationSiSignatureInvalide()
            throws Exception {
        emettre();

        when(signataire.verifier(
                any(byte[].class),
                any(SignatureLocale.Preuve.class),
                any(Instant.class)))
                .thenReturn(false);

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        () -> transactions.consulter(
                                bailleur(), id, consultation))
                        .getStatusCode());
    }

    @Test
    void refuseConsultationParAutreBailleur()
            throws Exception {
        emettre();

        var autre = identite(
                UUID.randomUUID().toString(), "ROLE_BAILLEUR");

        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class,
                        () -> transactions.consulter(
                                autre, id, consultation))
                        .getStatusCode());
    }
}