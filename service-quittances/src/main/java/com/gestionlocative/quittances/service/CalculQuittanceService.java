package com.gestionlocative.quittances.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.gestionlocative.quittances.dto.ResultatCalcul;
import com.gestionlocative.quittances.dto.SituationPeriode;
import com.gestionlocative.quittances.dto.SituationPeriode.Affectation;
import com.gestionlocative.quittances.dto.SituationPeriode.Nature;
import com.gestionlocative.quittances.dto.SituationPeriode.StatutPaiement;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.exception.RegleMetierException;

import org.springframework.stereotype.Service;

@Service
public class CalculQuittanceService {

    public ResultatCalcul calculer(SituationPeriode situation) {
        verifierSituation(situation);

        BigDecimal loyerDu = montant(situation.loyerDu(), "loyer dû");
        BigDecimal chargesDues = montant(situation.chargesDues(), "charges dues");

        if (loyerDu.add(chargesDues).signum() <= 0) {
            throw erreur("MONTANT_DU_NUL", "La période ne comporte aucune somme due.");
        }

        Map<String, Affectation> affectationsUniques = new LinkedHashMap<>();
        Map<String, StatutPaiement> statutsPaiements = new HashMap<>();

        for (Affectation affectation : situation.affectations()) {
            verifierAffectation(affectation);

            StatutPaiement statutPrecedent = statutsPaiements.putIfAbsent(
                    affectation.paiementId(),
                    affectation.statut()
            );

            if (statutPrecedent != null
                    && statutPrecedent != affectation.statut()) {
                throw erreur(
                        "STATUT_PAIEMENT_INCOHERENT",
                        "Un même paiement possède plusieurs statuts dans la réponse."
                );
            }

            Affectation precedente = affectationsUniques.putIfAbsent(
                    affectation.affectationId(),
                    affectation
            );

            if (precedente != null && !precedente.equals(affectation)) {
                throw erreur(
                        "AFFECTATION_CONFLICTUELLE",
                        "Un identifiant d'affectation correspond à des données différentes."
                );
            }
        }

        BigDecimal loyerPaye = new BigDecimal("0.00");
        BigDecimal chargesPayees = new BigDecimal("0.00");
        var retenues = new ArrayList<Affectation>();

        for (Affectation affectation : affectationsUniques.values()) {
            if (!situation.bailId().equals(affectation.bailId())) {
                continue;
            }

            if (affectation.statut() != StatutPaiement.CONFIRME) {
                continue;
            }

            if (affectation.nature() != Nature.LOYER
                    && affectation.nature() != Nature.CHARGES) {
                continue;
            }

            boolean periodeIdentique =
                    situation.periodeDebut().equals(affectation.periodeDebut())
                            && situation.periodeFin().equals(affectation.periodeFin());

            if (!periodeIdentique) {
                boolean chevauchement =
                        !affectation.periodeFin().isBefore(situation.periodeDebut())
                                && !affectation.periodeDebut().isAfter(situation.periodeFin());

                if (chevauchement) {
                    throw erreur(
                            "AFFECTATION_PERIODE_AMBIGUE",
                            "Une affectation chevauche la période sans lui correspondre exactement."
                    );
                }

                continue;
            }

            BigDecimal valeur = montant(affectation.montant(), "affectation");

            if (valeur.signum() == 0) {
                continue;
            }

            if (affectation.nature() == Nature.LOYER) {
                loyerPaye = loyerPaye.add(valeur);
            } else {
                chargesPayees = chargesPayees.add(valeur);
            }

            retenues.add(affectation);
        }

        if (loyerPaye.compareTo(loyerDu) > 0
                || chargesPayees.compareTo(chargesDues) > 0) {
            throw erreur(
                    "AFFECTATION_SUPERIEURE_AU_DU",
                    "Les affectations dépassent le loyer ou les charges dus."
            );
        }

        BigDecimal totalPaye = loyerPaye.add(chargesPayees);

        if (totalPaye.signum() == 0) {
            throw erreur(
                    "AUCUN_PAIEMENT_CONFIRME",
                    "Aucune somme confirmée n'est affectée à cette période."
            );
        }

        boolean integral =
                loyerPaye.compareTo(loyerDu) == 0
                        && chargesPayees.compareTo(chargesDues) == 0;

        TypeDocument type = integral
                ? TypeDocument.QUITTANCE_INTEGRALE
                : TypeDocument.RECU_PARTIEL;

        return new ResultatCalcul(
                loyerDu,
                chargesDues,
                loyerPaye,
                chargesPayees,
                totalPaye,
                type,
                retenues
        );
    }

    private void verifierSituation(SituationPeriode situation) {
        if (situation == null) {
            throw erreur("SITUATION_ABSENTE", "Les données de la période sont absentes.");
        }

        verifierIdentifiant(situation.bailId());

        if (!"MAD".equals(situation.devise())) {
            throw erreur("DEVISE_INVALIDE", "La devise attendue est MAD.");
        }

        if (situation.debutBail() == null
                || situation.periodeDebut() == null
                || situation.periodeFin() == null) {
            throw erreur("DATE_ABSENTE", "Les dates nécessaires sont absentes.");
        }

        if (situation.finBail() != null
                && situation.finBail().isBefore(situation.debutBail())) {
            throw erreur("BAIL_INCOHERENT", "La fin du bail précède son début.");
        }

        if (situation.periodeFin().isBefore(situation.periodeDebut())) {
            throw erreur("PERIODE_INVALIDE", "La fin de période précède son début.");
        }

        if (situation.periodeDebut().isBefore(situation.debutBail())
                || (situation.finBail() != null
                && situation.periodeFin().isAfter(situation.finBail()))) {
            throw erreur(
                    "PERIODE_HORS_BAIL",
                    "La période n'est pas entièrement couverte par le bail."
            );
        }
    }

    private void verifierAffectation(Affectation affectation) {
        if (affectation == null) {
            throw erreur("AFFECTATION_ABSENTE", "Une affectation est absente.");
        }

        verifierIdentifiant(affectation.affectationId());
        verifierIdentifiant(affectation.paiementId());
        verifierIdentifiant(affectation.bailId());

        if (affectation.nature() == null || affectation.statut() == null) {
            throw erreur(
                    "AFFECTATION_INCOMPLETE",
                    "La nature et le statut de l'affectation sont obligatoires."
            );
        }

        if (affectation.periodeDebut() == null
                || affectation.periodeFin() == null
                || affectation.periodeFin().isBefore(affectation.periodeDebut())) {
            throw erreur(
                    "PERIODE_AFFECTATION_INVALIDE",
                    "La période de l'affectation est invalide."
            );
        }

        if (!"MAD".equals(affectation.devise())) {
            throw erreur("DEVISE_INVALIDE", "La devise attendue est MAD.");
        }

        montant(affectation.montant(), "affectation");
    }

    private void verifierIdentifiant(String identifiant) {
        if (identifiant == null
                || identifiant.isBlank()
                || identifiant.length() > 100) {
            throw erreur("IDENTIFIANT_INVALIDE", "Un identifiant métier est invalide.");
        }
    }

    private BigDecimal montant(BigDecimal valeur, String libelle) {
        if (valeur == null || valeur.signum() < 0) {
            throw erreur(
                    "MONTANT_INVALIDE",
                    "Le montant " + libelle + " doit être présent et positif ou nul."
            );
        }

        BigDecimal normalise;

        try {
            normalise = valeur.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw erreur(
                    "PRECISION_INVALIDE",
                    "Le montant " + libelle + " comporte une fraction inférieure au centime."
            );
        }

        if (normalise.precision() > 12) {
            throw erreur("MONTANT_TROP_ELEVE", "Le montant dépasse la capacité autorisée.");
        }

        return normalise;
    }

    private RegleMetierException erreur(String code, String message) {
        return new RegleMetierException(code, message);
    }
}