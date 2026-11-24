package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.config.ReferentielsProperties;
import com.gestionlocative.quittances.dto.SituationPeriode.Affectation;
import com.gestionlocative.quittances.dto.SituationPeriode.StatutPaiement;
import com.gestionlocative.quittances.entity.PaiementAffectation;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.entity.StatutQuittance;
import com.gestionlocative.quittances.entity.StatutSignature;
import com.gestionlocative.quittances.entity.TypeDocument;
import com.gestionlocative.quittances.exception.RegleMetierException;
import com.gestionlocative.quittances.exception.ServiceExterneException;
import com.gestionlocative.quittances.repository.PaiementAffectationRepository;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.security.IdentiteAudit;
import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Pré-contrôle en lecture seule. Il ne réserve pas le document et ne constitue
 * pas une autorisation réutilisable pour une émission ultérieure.
 */
@Service
public class ControlePrealableEmissionService {

    private final QuittanceRepository quittances;
    private final PaiementAffectationRepository affectations;
    private final ConstructionBrouillonService construction;
    private final IdentiteAudit identiteAudit;
    private final ReferentielsProperties referentiels;
    private final String modeJwt;
    private final String modePaiements;

    public ControlePrealableEmissionService(
            QuittanceRepository quittances,
            PaiementAffectationRepository affectations,
            ConstructionBrouillonService construction,
            IdentiteAudit identiteAudit,
            ReferentielsProperties referentiels,
            @Value("${quittances.securite.mode}") String modeJwt,
            @Value("${quittances.integration.paiements.mode}") String modePaiements
    ) {
        this.quittances = quittances;
        this.affectations = affectations;
        this.construction = construction;
        this.identiteAudit = identiteAudit;
        this.referentiels = referentiels;
        this.modeJwt = modeJwt;
        this.modePaiements = modePaiements;
    }

    @Transactional(propagation = Propagation.NEVER)
    public void verifier(Authentication authentication, UUID quittanceId) {
        LecteurQuittances lecteur =
                LecteurQuittances.depuis(authentication);
        if (!lecteur.bailleur()) {
            throw new AccessDeniedException(
                    "Le pré-contrôle est réservé au bailleur."
            );
        }

        if (!"COMMUN".equals(modeJwt)) {
            throw indisponible(
                    "Le pré-contrôle d'émission est réservé au mode COMMUN."
            );
        }

        IdentiteAudit.Acteur acteur =
                identiteAudit.depuis(authentication);
        if (!IdentiteAudit.SOURCE_JWT_COMMUN.equals(
                acteur.emetteurIdentite()
        ) || !acteur.sujet().equals(lecteur.identifiant())) {
            throw new AccessDeniedException(
                    "Le bailleur doit correspondre au sujet du JWT commun vérifié."
            );
        }

        if (!sourcesReellesConfigurees()) {
            throw indisponible(
                    "Les preuves des services réels sont nécessaires au "
                            + "pré-contrôle d'émission."
            );
        }

        Quittance document = quittances.findByIdAndBailleurId(
                quittanceId,
                lecteur.identifiant()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Document introuvable."
        ));

        verifierEtat(document);
        verifierInstantane(document);

        ConstructionBrouillonService.Resultat situationCourante;
        try {
            situationCourante = construction.construire(
                    lecteur.identifiant(),
                    document.getBailId(),
                    document.getPeriodeDebut(),
                    document.getPeriodeFin()
            );
        } catch (ServiceExterneException exception) {
            throw indisponible(
                    "Les preuves métier ne peuvent pas être vérifiées "
                            + "auprès des services sources."
            );
        } catch (RegleMetierException exception) {
            throw conflit(
                    "Les données actuelles ne permettent pas de confirmer "
                            + "l'éligibilité du document."
            );
        }

        if (situationCourante == null
                || situationCourante.document() == null) {
            throw new IllegalStateException(
                    "La construction de la situation courante est absente."
            );
        }

        Quittance documentCourant = situationCourante.document();
        if (documentCourant.getTypeDocument()
                != TypeDocument.QUITTANCE_INTEGRALE) {
            throw conflit(
                    "Les paiements actuels ne permettent qu'un reçu partiel, "
                            + "pas une quittance intégrale."
            );
        }

        if (!memeInstantane(document, documentCourant)) {
            throw conflit(
                    "Le bail, la période, les montants ou l'instantané "
                            + "ont changé depuis la préparation."
            );
        }

        List<PaiementAffectation> affectationsEnregistrees =
                affectations.findByQuittance_IdOrderByAffectationExterneIdAsc(
                        quittanceId
                );
        if (!memesAffectations(
                document,
                affectationsEnregistrees,
                situationCourante.affectations()
        )) {
            throw conflit(
                    "Les affectations confirmées ont changé depuis "
                            + "la préparation."
            );
        }
    }

    private boolean sourcesReellesConfigurees() {
        return modeIntegration(modePaiements)
                && modeIntegration(referentiels.locataires())
                && modeIntegration(referentiels.biens())
                && modeIntegration(referentiels.baux());
    }

    private static boolean modeIntegration(String mode) {
        return "INTEGRATION".equals(mode) || "UTILISATEUR".equals(mode);
    }
    private static boolean modeIntegration(
            ReferentielsProperties.Endpoint endpoint
    ) {
        return endpoint != null
                && modeIntegration(endpoint.mode());
    }

    private void verifierEtat(Quittance document) {
        if (document.getStatut() != StatutQuittance.EN_ATTENTE_SIGNATURE
                || document.getStatutSignature() != StatutSignature.EN_ATTENTE
                || document.getEmisLe() != null
                || document.getNumero() != null
                || document.getPdfReference() != null
                || document.getPdfSha256() != null) {
            throw conflit(
                    "Le document n'est pas un brouillon validé disponible "
                            + "pour ce pré-contrôle."
            );
        }

        if (document.getTypeDocument() != TypeDocument.QUITTANCE_INTEGRALE) {
            throw conflit(
                    "Un reçu partiel ne peut pas être pré-contrôlé comme "
                            + "une quittance intégrale."
            );
        }
    }

    private void verifierInstantane(Quittance document) {
        if (document.getBailId() == null
                || document.getLocataireId() == null
                || document.getBienId() == null
                || document.getBailleurId() == null
                || document.getPeriodeDebut() == null
                || document.getPeriodeFin() == null
                || document.getPeriodeFin()
                        .isBefore(document.getPeriodeDebut())) {
            throw conflit("L'identité du bail ou la période est incohérente.");
        }

        Map<String, String> instantane = document.getDonneesFigees();
        if (!"1".equals(instantane.get("versionSnapshot"))
                || !document.getBailId().equals(
                        instantane.get("referenceBail")
                )) {
            throw conflit(
                    "L'instantané documentaire requis est absent ou invalide."
            );
        }

        try {
            LocalDate debutBail = LocalDate.parse(
                    instantane.get("debutBail")
            );
            String finBailTexte = instantane.get("finBail");
            LocalDate finBail = finBailTexte == null
                    ? null
                    : LocalDate.parse(finBailTexte);

            if (document.getPeriodeDebut().isBefore(debutBail)
                    || (finBail != null
                    && document.getPeriodeFin().isAfter(finBail))) {
                throw conflit(
                        "La période du document n'est pas couverte "
                                + "par le bail figé."
                );
            }
        } catch (DateTimeParseException | NullPointerException exception) {
            throw conflit(
                    "Les dates du bail figées dans le document sont invalides."
            );
        }

        BigDecimal totalCalcule = document.getLoyerPaye()
                .add(document.getChargesPayees());
        if (document.getTotalPaye().compareTo(totalCalcule) != 0
                || document.getLoyerPaye()
                        .compareTo(document.getLoyerDu()) != 0
                || document.getChargesPayees()
                        .compareTo(document.getChargesDues()) != 0) {
            throw conflit(
                    "Les montants du document ne constituent pas "
                            + "un règlement intégral cohérent."
            );
        }
    }

    private boolean memeInstantane(
            Quittance document,
            Quittance courant
    ) {
        return Objects.equals(document.getBailId(), courant.getBailId())
                && Objects.equals(
                        document.getLocataireId(),
                        courant.getLocataireId()
                )
                && Objects.equals(document.getBienId(), courant.getBienId())
                && Objects.equals(
                        document.getBailleurId(),
                        courant.getBailleurId()
                )
                && Objects.equals(
                        document.getPeriodeDebut(),
                        courant.getPeriodeDebut()
                )
                && Objects.equals(
                        document.getPeriodeFin(),
                        courant.getPeriodeFin()
                )
                && document.getTypeDocument() == courant.getTypeDocument()
                && memeMontant(document.getLoyerDu(), courant.getLoyerDu())
                && memeMontant(
                        document.getChargesDues(),
                        courant.getChargesDues()
                )
                && memeMontant(
                        document.getLoyerPaye(),
                        courant.getLoyerPaye()
                )
                && memeMontant(
                        document.getChargesPayees(),
                        courant.getChargesPayees()
                )
                && memeMontant(
                        document.getTotalPaye(),
                        courant.getTotalPaye()
                )
                && Objects.equals(document.getDevise(), courant.getDevise())
                && document.getDonneesFigees().equals(
                        courant.getDonneesFigees()
                );
    }

    private boolean memesAffectations(
            Quittance document,
            List<PaiementAffectation> enregistrees,
            List<Affectation> actuelles
    ) {
        if (enregistrees == null
                || actuelles == null
                || enregistrees.size() != actuelles.size()) {
            return false;
        }

        Map<String, Affectation> parIdentifiant = new HashMap<>();
        for (Affectation actuelle : actuelles) {
            if (actuelle == null
                    || actuelle.affectationId() == null
                    || actuelle.statut() != StatutPaiement.CONFIRME
                    || parIdentifiant.putIfAbsent(
                            actuelle.affectationId(),
                            actuelle
                    ) != null) {
                return false;
            }
        }

        Set<String> traitees = new HashSet<>();
        for (PaiementAffectation enregistree : enregistrees) {
            if (enregistree == null
                    || !"CONFIRME".equals(enregistree.getStatutSource())
                    || !traitees.add(enregistree.getAffectationExterneId())) {
                return false;
            }

            Affectation actuelle = parIdentifiant.get(
                    enregistree.getAffectationExterneId()
            );
            if (actuelle == null
                    || !document.getBailId().equals(actuelle.bailId())
                    || !document.getPeriodeDebut().equals(
                            actuelle.periodeDebut()
                    )
                    || !document.getPeriodeFin().equals(
                            actuelle.periodeFin()
                    )
                    || !enregistree.getPaiementExterneId().equals(
                            actuelle.paiementId()
                    )
                    || !enregistree.getNature().equals(
                            actuelle.nature().name()
                    )
                    || !memeMontant(
                            enregistree.getMontant(),
                            actuelle.montant()
                    )
                    || !enregistree.getDevise().equals(
                            actuelle.devise()
                    )) {
                return false;
            }
        }

        return traitees.size() == parIdentifiant.size();
    }

    private boolean memeMontant(BigDecimal premier, BigDecimal second) {
        return premier != null
                && second != null
                && premier.compareTo(second) == 0;
    }

    private ResponseStatusException conflit(String detail) {
        return new ResponseStatusException(HttpStatus.CONFLICT, detail);
    }

    private ResponseStatusException indisponible(String detail) {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                detail
        );
    }
}
