package com.gestionlocative.quittances;

import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.StatutQuittance;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.service.AnnulationQuittanceService;
import com.gestionlocative.quittances.service.ValidationQuittanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AnnulationQuittanceServiceTest {

    @Autowired
    private AnnulationQuittanceService service;

    @Autowired
    private ValidationQuittanceService validationService;

    @Autowired
    private QuittanceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void doitAnnulerSansSupprimerEtTracerLeMotif() {
        String bailleur = identifiant();
        Quittance document = creer(bailleur);
        Map<String, String> donneesAvant = document.getDonneesFigees();

        var resultat = service.annuler(
                identite("BAILLEUR", bailleur),
                document.getId(),
                "  Document préparé par erreur.  ",
                true
        );

        assertEquals("ANNULE", resultat.statut());
        assertEquals("Document préparé par erreur.", resultat.motif());

        Quittance conserve = repository.findById(document.getId())
                .orElseThrow();

        assertEquals(StatutQuittance.ANNULE, conserve.getStatut());
        assertEquals(donneesAvant, conserve.getDonneesFigees());
        assertEquals(new BigDecimal("3200.00"), conserve.getTotalPaye());

        String motifAudit = jdbcTemplate.queryForObject(
                """
                SELECT motif
                FROM quittance_audit
                WHERE quittance_id = ?
                  AND operation = 'ANNULATION'
                """,
                String.class,
                document.getId()
        );

        assertEquals("Document préparé par erreur.", motifAudit);
    }

    @Test
    void doitConserverLaValidationAnterieure() {
        String bailleur = identifiant();
        Quittance document = creer(bailleur);
        var utilisateur = identite("BAILLEUR", bailleur);

        validationService.valider(utilisateur, document.getId(), true);

        var resultat = service.annuler(
                utilisateur,
                document.getId(),
                "Correction documentaire nécessaire.",
                true
        );

        assertEquals("ANNULE", resultat.statut());
        assertEquals("NON_SIGNE", resultat.statutSignature());

        Long validations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quittance_validation "
                        + "WHERE quittance_id = ?",
                Long.class,
                document.getId()
        );

        Long audits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quittance_audit "
                        + "WHERE quittance_id = ?",
                Long.class,
                document.getId()
        );

        assertEquals(1L, validations.longValue());
        assertEquals(2L, audits.longValue());
    }

    @Test
    void doitRefuserUneDeuxiemeAnnulation() {
        String bailleur = identifiant();
        Quittance document = creer(bailleur);
        var utilisateur = identite("BAILLEUR", bailleur);

        service.annuler(
                utilisateur,
                document.getId(),
                "Erreur de préparation.",
                true
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.annuler(
                        utilisateur,
                        document.getId(),
                        "Deuxième demande.",
                        true
                )
        );

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void doitRefuserUnAutreBailleur() {
        Quittance document = creer(identifiant());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.annuler(
                        identite("BAILLEUR", identifiant()),
                        document.getId(),
                        "Erreur.",
                        true
                )
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void doitRefuserUnMotifVide() {
        String bailleur = identifiant();
        Quittance document = creer(bailleur);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.annuler(
                        identite("BAILLEUR", bailleur),
                        document.getId(),
                        "   ",
                        true
                )
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void doitRefuserUnLocataire() {
        Quittance document = creer(identifiant());

        assertThrows(
                AccessDeniedException.class,
                () -> service.annuler(
                        identite("LOCATAIRE", identifiant()),
                        document.getId(),
                        "Erreur.",
                        true
                )
        );
    }

    private Quittance creer(String bailleur) {
        return repository.saveAndFlush(
                Quittance.creerBrouillon(
                        identifiant(),
                        identifiant(),
                        identifiant(),
                        bailleur,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31),
                        new BigDecimal("3000.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("3000.00"),
                        new BigDecimal("200.00"),
                        TypeDocument.QUITTANCE_INTEGRALE,
                        Map.of(
                                "versionSnapshot", "1",
                                "bailleurNom", "Bailleur fictif",
                                "locataireNom", "Locataire fictif",
                                "locatairePrenom", "Test",
                                "bienAdresse", "Adresse fictive"
                        )
                )
        );
    }

    private JwtAuthenticationToken identite(
            String role,
            String identifiant
    ) {
        String claim = "BAILLEUR".equals(role)
                ? "bailleur_id"
                : "locataire_id";

        Jwt jwt = Jwt.withTokenValue("jeton-test")
                .header("alg", "RS256")
                .subject("acteur-test-annulation")
                .issuer("urn:gestionlocative:dev")
                .claim(claim, identifiant)
                .build();

        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    private String identifiant() {
        return "TEST-ANNULATION-" + UUID.randomUUID();
    }


    @Test
    void gestionnairePeutAnnulerDocumentAutreBailleur() {
        String bailleur = identifiant();
        Quittance document = creer(bailleur);

        var resultat = service.annuler(
                identite("GESTIONNAIRE", identifiant()),
                document.getId(),
                "Annulation par le gestionnaire.",
                true
        );

        assertEquals("ANNULE", resultat.statut());

        Quittance conserve = repository.findById(
                document.getId()).orElseThrow();

        assertEquals(bailleur, conserve.getBailleurId());
        assertEquals(StatutQuittance.ANNULE, conserve.getStatut());

        String acteur = jdbcTemplate.queryForObject(
                "SELECT acteur FROM quittance_audit "
                        + "WHERE quittance_id = ? AND operation = 'ANNULATION'",
                String.class,
                document.getId());

        assertEquals("acteur-test-annulation", acteur);
    }

    @Test
    void gestionnaireNePeutAnnulerDocumentInexistant() {
        var erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.annuler(
                        identite("GESTIONNAIRE", identifiant()),
                        UUID.randomUUID(),
                        "Document absent.",
                        true));

        assertEquals(404, erreur.getStatusCode().value());
    }

}