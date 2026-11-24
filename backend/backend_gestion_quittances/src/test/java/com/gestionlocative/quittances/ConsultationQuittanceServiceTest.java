package com.gestionlocative.quittances;

import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.service.ConsultationQuittanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class ConsultationQuittanceServiceTest {

    @Autowired
    private ConsultationQuittanceService service;

    @Autowired
    private QuittanceRepository repository;

    @Test
    void bailleurPeutConsulterSonBrouillon() {
        String bailleur = identifiant();
        String locataire = identifiant();

        Quittance document = creer(bailleur, locataire);

        var resultat = service.consulter(
                identite("BAILLEUR", bailleur),
                document.getId()
        );

        assertEquals(document.getId(), resultat.id());
        assertEquals("BROUILLON", resultat.statut());
        assertEquals("Locataire test", resultat.nomLocataire());
        assertEquals(new BigDecimal("3200.00"), resultat.totalPaye());
    }

    @Test
    void autreBailleurNePeutPasConsulterDocument() {
        Quittance document = creer(identifiant(), identifiant());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.consulter(
                        identite("BAILLEUR", identifiant()),
                        document.getId()
                )
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void locataireNePeutPasConsulterSonBrouillon() {
        String locataire = identifiant();
        Quittance document = creer(identifiant(), locataire);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.consulter(
                        identite("LOCATAIRE", locataire),
                        document.getId()
                )
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void listeBailleurExclutDocumentsDesAutres() {
        String bailleur = identifiant();

        creer(bailleur, identifiant());
        creer(bailleur, identifiant());
        creer(identifiant(), identifiant());

        var resultat = service.lister(
                identite("BAILLEUR", bailleur),
                0,
                1,
                "creeLe",
                "desc"
        );

        assertEquals(1, resultat.contenu().size());
        assertEquals(2L, resultat.totalElements());
        assertEquals(2, resultat.totalPages());
    }

    @Test
    void listeBailleurCommunEstLimiteeAuSubVerifie() {
        String sub = UUID.randomUUID().toString();

        creer(sub, identifiant());
        creer(identifiant(), identifiant());

        var resultat = service.lister(
                identiteCommunBailleur(sub),
                0,
                20,
                "creeLe",
                "desc"
        );

        assertEquals(1, resultat.contenu().size());
        assertEquals(1L, resultat.totalElements());
    }

    @Test
    void listeLocataireExclutBrouillons() {
        String locataire = identifiant();
        creer(identifiant(), locataire);

        var resultat = service.lister(
                identite("LOCATAIRE", locataire),
                0,
                20,
                "creeLe",
                "desc"
        );

        assertEquals(0L, resultat.totalElements());
        assertEquals(0, resultat.contenu().size());
    }

    @Test
    void refuseTailleExcessive() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.lister(
                        identite("BAILLEUR", identifiant()),
                        0,
                        101,
                        "creeLe",
                        "desc"
                )
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    private Quittance creer(String bailleur, String locataire) {
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
                        "bailleurNom", "Bailleur test",
                        "locataireNom", "Locataire test",
                        "locatairePrenom", "Prenom test",
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
                .subject("utilisateur-test")
                .claim(claim, identifiant)
                .build();

        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    private JwtAuthenticationToken identiteCommunBailleur(String sub) {
        Jwt jwt = Jwt.withTokenValue("jeton-commun-test")
                .header("alg", "HS256")
                .subject(sub)
                .claim("role", "BAILLEUR")
                .claim("bailleur_id", sub)
                .build();

        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_BAILLEUR"))
        );
    }

    private String identifiant() {
        return "TEST-LECTURE-" + UUID.randomUUID();
    }
}