package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.client.BienBailClient;
import com.gestionlocative.quittances.client.LocataireClient;
import com.gestionlocative.quittances.client.PaiementClient;
import com.gestionlocative.quittances.dto.SituationPeriode;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.exception.RegleMetierException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConstructionBrouillonService {

    private final BienBailClient bienBailClient;
    private final LocataireClient locataireClient;
    private final PaiementClient paiementClient;
    private final CalculQuittanceService calculService;

    public ConstructionBrouillonService(
            BienBailClient bienBailClient,
            LocataireClient locataireClient,
            PaiementClient paiementClient,
            CalculQuittanceService calculService
    ) {
        this.bienBailClient = bienBailClient;
        this.locataireClient = locataireClient;
        this.paiementClient = paiementClient;
        this.calculService = calculService;
    }

    @Transactional(propagation = Propagation.NEVER)
    public Resultat construire(
            String bailleurId,
            String bailId,
            LocalDate debut,
            LocalDate fin
    ) {
        return construireInterne(bailleurId, bailId, debut, fin, false);
    }

    @Transactional(propagation = Propagation.NEVER)
    public Resultat construirePourGestionnaire(
            org.springframework.security.core.Authentication authentication,
            String bailId,
            LocalDate debut,
            LocalDate fin
    ) {
        var lecteur = com.gestionlocative.quittances.security
                .LecteurQuittances.depuis(authentication);

        if (!lecteur.gestionnaire()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Cette operation est reservee au gestionnaire.");
        }

        return construireInterne(null, bailId, debut, fin, true);
    }

    private Resultat construireInterne(
            String bailleurId,
            String bailId,
            LocalDate debut,
            LocalDate fin,
            boolean gestionnaire
    ) {
        if (!gestionnaire && (bailleurId == null || bailleurId.isBlank())) {
            throw new RegleMetierException(
                    "ACTEUR_ABSENT",
                    "Le bailleur authentifié est obligatoire."
            );
        }

        var bail = bienBailClient.consulterBail(bailId, debut, fin);

        if (bail.bailleurId() == null || bail.bailleurId().isBlank()) {
            throw new RegleMetierException(
                    "PROPRIETAIRE_ABSENT",
                    "Le proprietaire du bail doit etre identifie.");
        }

        if (!gestionnaire && !bailleurId.equals(bail.bailleurId())) {
            throw new RegleMetierException(
                    "ACCES_INTERDIT",
                    "Ce bail ne relève pas de cet utilisateur."
            );
        }

        var bien = bienBailClient.consulterBien(bail.bienId());

        if (!bail.bienId().equals(bien.id())
                || !bail.bailleurId().equals(bien.bailleurId())) {
            throw new RegleMetierException(
                    "BIEN_BAIL_INCOHERENT",
                    "Le bien ne correspond pas aux données du bail."
            );
        }

        var locataire = locataireClient.consulter(bail.locataireId());

        if (!bail.locataireId().equals(locataire.id())) {
            throw new RegleMetierException(
                    "LOCATAIRE_BAIL_INCOHERENT",
                    "Le locataire ne correspond pas au bail."
            );
        }

        var affectations = paiementClient.consulterAffectations(
                bailId,
                debut,
                fin
        );

        var calcul = calculService.calculer(
                new SituationPeriode(
                        bail.bailId(),
                        bail.debutBail(),
                        bail.finBail(),
                        debut,
                        fin,
                        bail.loyerDu(),
                        bail.chargesDues(),
                        bail.devise(),
                        affectations
                )
        );

        Map<String, String> donnees = new HashMap<>();
        donnees.put("versionSnapshot", "1");
        donnees.put("referenceBail", bail.bailId());
        donnees.put("bailleurNom", bail.nomBailleur());
        donnees.put("locataireNom", locataire.nom());
        donnees.put(
                "locatairePrenom",
                locataire.prenom() == null ? "" : locataire.prenom()
        );
        donnees.put("bienAdresse", bien.adresse());
        donnees.put("debutBail", bail.debutBail().toString());

        if (bail.finBail() != null) {
            donnees.put("finBail", bail.finBail().toString());
        }

        Quittance document = Quittance.creerBrouillon(
                bail.bailId(),
                locataire.id(),
                bien.id(),
                bail.bailleurId(),
                debut,
                fin,
                calcul.loyerDu(),
                calcul.chargesDues(),
                calcul.loyerPaye(),
                calcul.chargesPayees(),
                calcul.typeDocument(),
                donnees
        );

        return new Resultat(document, calcul.affectationsRetenues());
    }

    public record Resultat(
            Quittance document,
            List<SituationPeriode.Affectation> affectations
    ) {
        public Resultat {
            affectations = List.copyOf(affectations);
        }
    }
}