package ma.gestionlocative.locataires.service;

import ma.gestionlocative.locataires.exception.CinDejaUtiliseException;
import ma.gestionlocative.locataires.model.Locataire;
import ma.gestionlocative.locataires.service.impl.LocataireServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Campagne de Tests Unitaires - Module Gestion des Locataires (Ahmed LAARIF)")
public class LocataireServiceTest {

    private LocataireService service;
    private UUID id1;
    private UUID id2;

    @BeforeEach
    void setUp() {
        // Initialisation autonome du service en mémoire pour un test instantané
        service = new LocataireServiceImpl();
        id1 = UUID.randomUUID();
        id2 = UUID.randomUUID();

        // 2 fiches existantes conformément au dossier de test
        Locataire loc1 = new Locataire(id1, "El Amrani", "Yassine", "yassine@mail.ma", "0661001122", "AB123456");
        Locataire loc2 = new Locataire(id2, "Bennani", "Fatima Zahra", "fatima@mail.ma", "0662334455", "CD789012");

        service.creerLocataire(loc1);
        service.creerLocataire(loc2);
    }

    @Test
    @DisplayName("UT-LOC-01 : Création valide d'un locataire complet avec CIN unique")
    void testCreerLocataireValide() {
        Locataire nouveau = new Locataire(null, "Benjelloun", "Mehdi", "mehdi@mail.ma", "0661223344", "EF345678");
        Locataire resultat = service.creerLocataire(nouveau);

        assertNotNull(resultat);
        assertNotNull(resultat.getId());
        assertEquals("EF345678", resultat.getCin());
        assertEquals("Benjelloun", resultat.getNom());
        assertEquals(3, service.nombreLocataires());
    }

    @Test
    @DisplayName("UT-LOC-02 : Refus si CIN déjà existant (levée de CinDejaUtiliseException)")
    void testCreerLocataireCinDejaExistant() {
        // Doublon portant le CIN 'AB123456' déjà attribué à Yassine El Amrani
        Locataire doublon = new Locataire(null, "Alami", "Karim", "k.alami@mail.ma", "0661998877", "AB123456");

        CinDejaUtiliseException exception = assertThrows(CinDejaUtiliseException.class, () -> {
            service.creerLocataire(doublon);
        });

        assertTrue(exception.getMessage().contains("Fiche refusée : CIN déjà utilisé"));
        assertEquals(2, service.nombreLocataires());
    }

    @Test
    @DisplayName("UT-LOC-03 : Refus si argument null (levée de IllegalArgumentException)")
    void testCreerLocataireNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.creerLocataire(null);
        });
        assertEquals(2, service.nombreLocataires());
    }

    @Test
    @DisplayName("UT-LOC-04 : Refus si CIN vide ou composé d'espaces")
    void testCreerLocataireCinVideOuNull() {
        Locataire locataireCinVide = new Locataire(null, "Naciri", "Samir", "samir@mail.ma", "0663112233", "   ");

        assertThrows(IllegalArgumentException.class, () -> {
            service.creerLocataire(locataireCinVide);
        });
        assertEquals(2, service.nombreLocataires());
    }

    @Test
    @DisplayName("UT-LOC-05 : Refus si format d'email invalide")
    void testCreerLocataireEmailInvalide() {
        Locataire emailInvalide = new Locataire(null, "Tazi", "Kenza", "email_invalide_sans_arobase", "0664556677", "GH901234");

        assertThrows(IllegalArgumentException.class, () -> {
            service.creerLocataire(emailInvalide);
        });
        assertEquals(2, service.nombreLocataires());
    }

    @Test
    @DisplayName("UT-LOC-06 : Recherche par UUID existant (locataire conforme retourné)")
    void testGetByIdExistant() {
        Optional<Locataire> resultat = service.getById(id1);

        assertTrue(resultat.isPresent());
        assertEquals("El Amrani", resultat.get().getNom());
        assertEquals("AB123456", resultat.get().getCin());
    }

    @Test
    @DisplayName("UT-LOC-07 : Recherche par UUID inexistant (Optional.empty() retourné)")
    void testGetByIdInexistant() {
        Optional<Locataire> resultat = service.getById(UUID.randomUUID());

        assertTrue(resultat.isEmpty());
    }

    @Test
    @DisplayName("UT-LOC-08 : Recherche par CIN existant (fiche correspondante retournée)")
    void testGetByCinExistant() {
        Optional<Locataire> resultat = service.getByCin("CD789012");

        assertTrue(resultat.isPresent());
        assertEquals("Fatima Zahra", resultat.get().getPrenom());
    }

    @Test
    @DisplayName("UT-LOC-09 : Recherche par CIN inexistant (Optional.empty() retourné)")
    void testGetByCinInexistant() {
        Optional<Locataire> resultat = service.getByCin("ZZ999999");

        assertTrue(resultat.isEmpty());
    }

    @Test
    @DisplayName("UT-LOC-10 : Modification réussie de l'email et du téléphone")
    void testModifierCoordonneesExistant() {
        boolean succes = service.modifierCoordonnees(id1, "yassine.pro@nouvel-email.ma", "0661001122");

        assertTrue(succes);
        Locataire maj = service.getById(id1).orElseThrow();
        assertEquals("yassine.pro@nouvel-email.ma", maj.getEmail());
        assertEquals("0661001122", maj.getTelephone());
    }

    @Test
    @DisplayName("UT-LOC-11 : Tentative de modification sur UUID inexistant (retourne false)")
    void testModifierCoordonneesInexistant() {
        boolean resultat = service.modifierCoordonnees(UUID.randomUUID(), "inconnu@mail.ma", "0600000000");

        assertFalse(resultat);
    }

    @Test
    @DisplayName("UT-LOC-12 : Comptage exact du nombre total de locataires enregistrés")
    void testNombreLocataires() {
        assertEquals(2, service.nombreLocataires());
    }

    @Test
    @DisplayName("UT-LOC-13 : Suppression d'un locataire existant (retourne true)")
    void testSupprimerLocataireExistant() {
        boolean suppressionReussie = service.supprimer(id1);

        assertTrue(suppressionReussie);
        assertTrue(service.getById(id1).isEmpty());
        assertEquals(1, service.nombreLocataires());
    }

    @Test
    @DisplayName("UT-LOC-14 : Tentative de suppression sur UUID inexistant (retourne false)")
    void testSupprimerLocataireInexistant() {
        boolean suppressionEchouee = service.supprimer(UUID.randomUUID());

        assertFalse(suppressionEchouee);
        assertEquals(2, service.nombreLocataires());
    }
}
