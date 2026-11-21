package com.gestionlocative.quittances;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.StatutQuittance;
import com.gestionlocative.quittances.entity.StatutSignature;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.repository.QuittanceRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class QuittanceRepositoryTest {

    @Autowired
    private QuittanceRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void doitEnregistrerEtRelireUnBrouillonIntegral() {
        String bailId = "TEST-BAIL-" + UUID.randomUUID();

        Map<String, String> snapshot = new HashMap<>();
        snapshot.put("locataireNom", "Locataire de démonstration");
        snapshot.put("bienAdresse", "Adresse fictive à Agadir");

        Quittance quittance = creerDocument(
                bailId,
                "3000.00",
                "200.00",
                TypeDocument.QUITTANCE_INTEGRALE,
                snapshot
        );

        snapshot.put("locataireNom", "Nom modifié après création");

        Quittance enregistree = repository.saveAndFlush(quittance);
        UUID id = enregistree.getId();

        entityManager.clear();

        Quittance relue = repository.findById(id).orElseThrow();

        assertNotNull(relue.getId());
        assertNotNull(relue.getVersion());
        assertNotNull(relue.getCreeLe());

        assertEquals(bailId, relue.getBailId());
        assertEquals(new BigDecimal("3000.00"), relue.getLoyerDu());
        assertEquals(new BigDecimal("200.00"), relue.getChargesDues());
        assertEquals(new BigDecimal("3200.00"), relue.getTotalPaye());

        assertEquals("MAD", relue.getDevise());
        assertEquals(TypeDocument.QUITTANCE_INTEGRALE, relue.getTypeDocument());
        assertEquals(StatutQuittance.BROUILLON, relue.getStatut());
        assertEquals(StatutSignature.NON_SIGNE, relue.getStatutSignature());

        assertNull(relue.getNumero());
        assertNull(relue.getEmisLe());

        assertEquals(
                "Locataire de démonstration",
                relue.getDonneesFigees().get("locataireNom")
        );

        assertEquals(
                "Adresse fictive à Agadir",
                relue.getDonneesFigees().get("bienAdresse")
        );
    }

    @Test
    void doitEnregistrerUnRecuPartiel() {
        Quittance quittance = creerDocument(
                "TEST-BAIL-" + UUID.randomUUID(),
                "1500.25",
                "0.00",
                TypeDocument.RECU_PARTIEL,
                Map.of("origine", "test automatique")
        );

        UUID id = repository.saveAndFlush(quittance).getId();

        entityManager.clear();

        Quittance relue = repository.findById(id).orElseThrow();

        assertEquals(TypeDocument.RECU_PARTIEL, relue.getTypeDocument());
        assertEquals(new BigDecimal("1500.25"), relue.getTotalPaye());
        assertEquals(new BigDecimal("0.00"), relue.getChargesPayees());
    }

    @Test
    void doitRefuserDeuxQuittancesIntegralesActivesPourLaMemePeriode() {
        String bailId = "TEST-BAIL-" + UUID.randomUUID();

        Quittance premiere = creerDocument(
                bailId,
                "3000.00",
                "200.00",
                TypeDocument.QUITTANCE_INTEGRALE,
                Map.of("origine", "premier document de test")
        );

        Quittance seconde = creerDocument(
                bailId,
                "3000.00",
                "200.00",
                TypeDocument.QUITTANCE_INTEGRALE,
                Map.of("origine", "second document de test")
        );

        repository.saveAndFlush(premiere);

        DataIntegrityViolationException erreur = assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(seconde)
        );

        assertTrue(
                erreur.getMostSpecificCause()
                        .getMessage()
                        .contains("uq_quittance_integrale_active")
        );
    }

    private Quittance creerDocument(
            String bailId,
            String loyerPaye,
            String chargesPayees,
            TypeDocument type,
            Map<String, String> snapshot) {

        return Quittance.creerBrouillon(
                bailId,
                "TEST-LOCATAIRE-1",
                "TEST-BIEN-1",
                "TEST-BAILLEUR-1",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                new BigDecimal("3000.00"),
                new BigDecimal("200.00"),
                new BigDecimal(loyerPaye),
                new BigDecimal(chargesPayees),
                type,
                snapshot
        );
    }
}