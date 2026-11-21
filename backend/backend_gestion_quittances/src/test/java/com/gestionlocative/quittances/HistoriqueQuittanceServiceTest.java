package com.gestionlocative.quittances;

import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.service.HistoriqueQuittanceService;
import com.gestionlocative.quittances.service.ValidationQuittanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Isolation;
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
@Transactional(isolation = Isolation.REPEATABLE_READ)
class HistoriqueQuittanceServiceTest {

    @Autowired
    private HistoriqueQuittanceService historiqueService;

    @Autowired
    private ValidationQuittanceService validationService;

    @Autowired
    private QuittanceRepository repository;

    @Test
    void doitRetournerLaValidationEnregistree() {
        String bailleur = identifiant();
        String locataire = identifiant();
        Quittance document = creer(bailleur, locataire);

        var utilisateur = identite("BAILLEUR", bailleur);

        validationService.valider(
                utilisateur,
                document.getId(),
                true
        );

        var resultat = historiqueService.consulter(
                utilisateur,
                document.getId(),
                0,
                20
        );

        assertEquals(1L, resultat.totalElements());
        assertEquals(1L, resultat.totalPages());
        assertEquals(1, resultat.contenu().size());

        var evenement = resultat.contenu().getFirst();

        assertEquals("VALIDATION", evenement.operation());
        assertEquals("SUCCES", evenement.resultat());
        assertEquals("BROUILLON", evenement.statutAvant());
        assertEquals("EN_ATTENTE_SIGNATURE", evenement.statutApres());
    }

    @Test
    void doitRetournerPageVideAuDelaDesResultats() {
        String bailleur = identifiant();
        Quittance document = creer(bailleur, identifiant());

        var utilisateur = identite("BAILLEUR", bailleur);

        validationService.valider(
                utilisateur,
                document.getId(),
                true
        );

        var resultat = historiqueService.consulter(
                utilisateur,
                document.getId(),
                1,
                1
        );

        assertEquals(1L, resultat.totalElements());
        assertEquals(1L, resultat.totalPages());
        assertEquals(0, resultat.contenu().size());
        assertEquals(1, resultat.page());
    }

    @Test
    void doitRefuserHistoriqueDUnAutreBailleur() {
        Quittance document = creer(identifiant(), identifiant());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> historiqueService.consulter(
                        identite("BAILLEUR", identifiant()),
                        document.getId(),
                        0,
                        20
                )
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void doitRefuserHistoriqueDuBrouillonAuLocataire() {
        String locataire = identifiant();
        Quittance document = creer(identifiant(), locataire);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> historiqueService.consulter(
                        identite("LOCATAIRE", locataire),
                        document.getId(),
                        0,
                        20
                )
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void doitRefuserPaginationInvalide() {
        String bailleur = identifiant();
        Quittance document = creer(bailleur, identifiant());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> historiqueService.consulter(
                        identite("BAILLEUR", bailleur),
                        document.getId(),
                        0,
                        101
                )
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    private Quittance creer(
            String bailleur,
            String locataire
    ) {
        Quittance document = Quittance.creerBrouillon(
                identifiant(),
                locataire,
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
        );

        return repository.saveAndFlush(document);
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
                .subject("acteur-test-historique")
                .issuer("urn:gestionlocative:dev")
                .claim(claim, identifiant)
                .build();

        return new JwtAuthenticationToken(
                jwt,
                List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                )
        );
    }

    private String identifiant() {
        return "TEST-HISTORIQUE-" + UUID.randomUUID();
    }
}