package com.gestionlocative.quittances.client;

import com.gestionlocative.quittances.dto.ReferentielsExternes.BailPeriode;
import com.gestionlocative.quittances.dto.ReferentielsExternes.Bien;
import com.gestionlocative.quittances.exception.RegleMetierException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

/**
 * Adaptateur en lecture seule vers les API collectives actuelles.
 * Aucun propriétaire n'est déduit d'une simple permission de lecture.
 */
public final class BienBailCollectifClient implements BienBailClient {
    private final RestClient biens;
    private final RestClient baux;
    private final String cheminBien;
    private final String cheminBail;

    public BienBailCollectifClient(RestClient biens, String cheminBien,
            RestClient baux, String cheminBail) {
        this.biens = biens;
        this.cheminBien = cheminBien;
        this.baux = baux;
        this.cheminBail = cheminBail;
    }

    public record FicheBien(String id, String adresse, String ville,
            String bailleurId, BigDecimal charges) { }

    public record FicheBail(String id, String bienId, String locataireId,
            LocalDate dateDebut, LocalDate dateFin, BigDecimal montantLoyer,
            String statut) { }

    public record FicheUtilisateur(
            String versionContrat, String id, String nom,
            String prenom, String nomComplet, String role) { }

    private String lireIdentiteBailleur(String bailleurId) {
        FicheUtilisateur utilisateur = LectureApiExterne.lire(
                biens, "BIENS", "/api/users/{id}",
                Map.of("id", bailleurId), FicheUtilisateur.class);

        if (!"1".equals(utilisateur.versionContrat())
                || !bailleurId.equals(utilisateur.id())
                || !"BAILLEUR".equals(utilisateur.role())
                || !LectureApiExterne.textePresent(utilisateur.nom())
                || !LectureApiExterne.textePresent(utilisateur.prenom())
                || !LectureApiExterne.textePresent(utilisateur.nomComplet())) {
            throw LectureApiExterne.incoherence("BIENS");
        }

        String nomComplet = utilisateur.prenom().trim()
                + " " + utilisateur.nom().trim();

        if (!nomComplet.equals(utilisateur.nomComplet().trim())) {
            throw LectureApiExterne.incoherence("BIENS");
        }

        return nomComplet;
    }

    private FicheBien lireBien(String id) {
        FicheBien bien = LectureApiExterne.lire(biens, "BIENS", cheminBien,
                Map.of("id", id), FicheBien.class);
        if (!id.equals(bien.id()) || !LectureApiExterne.textePresent(bien.adresse())
                || !LectureApiExterne.identifiantValide(bien.bailleurId())) {
            throw LectureApiExterne.incoherence("BIENS");
        }
        return bien;
    }

    @Override
    public Bien consulterBien(String id) {
        LectureApiExterne.verifierIdentifiant(id);
        FicheBien source = lireBien(id);
        String adresse = source.adresse().trim();
        if (LectureApiExterne.textePresent(source.ville())) {
            adresse += ", " + source.ville().trim();
        }
        return new Bien("1", id, adresse, source.bailleurId());
    }

    @Override
    public BailPeriode consulterBail(String id, LocalDate debut, LocalDate fin) {
        LectureApiExterne.verifierIdentifiant(id);
        if (debut == null || fin == null || fin.isBefore(debut)) {
            throw new RegleMetierException("PERIODE_INVALIDE", "Période invalide.");
        }
        // Les API collectives décrivent des mensualités, sans règle de prorata.
        YearMonth mois = YearMonth.from(debut);
        if (!debut.equals(mois.atDay(1)) || !fin.equals(mois.atEndOfMonth())) {
            throw new RegleMetierException("PERIODE_NON_MENSUELLE",
                    "Le contrat collectif ne prend pas en charge les périodes partielles.");
        }
        FicheBail bail = LectureApiExterne.lire(baux, "BAUX", cheminBail,
                Map.of("id", id), FicheBail.class);
        if (!id.equals(bail.id())
                || !LectureApiExterne.identifiantValide(bail.bienId())
                || !LectureApiExterne.identifiantValide(bail.locataireId())
                || bail.dateDebut() == null || bail.montantLoyer() == null
                || bail.montantLoyer().signum() < 0
                || !("ACTIF".equals(bail.statut()) || "TERMINE".equals(bail.statut()))
                || debut.isBefore(bail.dateDebut())
                || (bail.dateFin() != null && fin.isAfter(bail.dateFin()))) {
            throw LectureApiExterne.incoherence("BAUX");
        }
        FicheBien bien = lireBien(bail.bienId());
        if (bien.charges() == null || bien.charges().signum() < 0) {
            throw LectureApiExterne.incoherence("BIENS");
        }
        // L'identité provient du JWT vérifié; la propriété provient de Biens.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var lecteur = com.gestionlocative.quittances.security
                .LecteurQuittances.depuis(auth);

        if (!lecteur.peutEcrire()) {
            throw new AccessDeniedException(
                    "Operation reservee au bailleur et au gestionnaire.");
        }

        JwtAuthenticationToken jwt = (JwtAuthenticationToken) auth;
        String sujet = jwt.getToken().getSubject();

        if (lecteur.bailleur() && !bien.bailleurId().equals(sujet)) {
            throw new AccessDeniedException(
                    "Le bien n'appartient pas au bailleur authentifie.");
        }

        String nomBailleur;

        if (lecteur.gestionnaire()) {
            nomBailleur = lireIdentiteBailleur(bien.bailleurId());
        } else {
            String nom = jwt.getToken().getClaimAsString("nom");
            String prenom = jwt.getToken().getClaimAsString("prenom");

            if (!LectureApiExterne.textePresent(nom)
                    || !LectureApiExterne.textePresent(prenom)) {
                throw new AccessDeniedException(
                        "Nom du bailleur absent du JWT verifie.");
            }

            nomBailleur = prenom.trim() + " " + nom.trim();
        }

        return new BailPeriode("1", id, bail.locataireId(), bail.bienId(),
                bien.bailleurId(), nomBailleur, bail.dateDebut(),
                bail.dateFin(), debut, fin, bail.montantLoyer(), bien.charges(), "MAD");
    }
}
