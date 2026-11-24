package com.gestionlocative.loyers.service;

import com.gestionlocative.loyers.dto.EcheanceResponse;
import com.gestionlocative.loyers.dto.GenerationResultat;
import com.gestionlocative.loyers.exception.BusinessRuleException;
import com.gestionlocative.loyers.exception.ResourceNotFoundException;
import com.gestionlocative.loyers.model.Bail;
import com.gestionlocative.loyers.model.Echeance;
import com.gestionlocative.loyers.model.StatutBail;
import com.gestionlocative.loyers.model.StatutEcheance;
import com.gestionlocative.loyers.repository.BailRepository;
import com.gestionlocative.loyers.repository.EcheanceRepository;
import com.gestionlocative.loyers.repository.PaiementRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EcheanceService {

    private final EcheanceRepository echeanceRepository;
    private final PaiementRepository paiementRepository;
    private final BailRepository bailRepository;
    private final EcheanceStatutCalculator calculateur;
    private final Clock clock;

    public EcheanceService(EcheanceRepository echeanceRepository, PaiementRepository paiementRepository,
                           BailRepository bailRepository, EcheanceStatutCalculator calculateur, Clock clock) {
        this.echeanceRepository = echeanceRepository;
        this.paiementRepository = paiementRepository;
        this.bailRepository = bailRepository;
        this.calculateur = calculateur;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EcheanceResponse trouver(UUID id) {
        Echeance e = echeanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Échéance introuvable : " + id));
        return versReponse(e);
    }

    @Transactional(readOnly = true)
    public List<EcheanceResponse> rechercher(UUID bailId, StatutEcheance statut) {
        List<Echeance> liste;
        if (bailId != null) {
            liste = echeanceRepository.findByBailIdOrderByAnneeAscMoisAsc(bailId);
            if (statut != null) {
                liste = liste.stream().filter(e -> e.getStatut() == statut).toList();
            }
        } else if (statut != null) {
            liste = echeanceRepository.findByStatut(statut);
        } else {
            liste = echeanceRepository.findAll();
        }
        return liste.stream().map(this::versReponse).toList();
    }

    /** Crée l'échéance d'un bail pour un mois donné (bail ACTIF, période couverte, pas de doublon). */
    @Transactional
    public EcheanceResponse genererPourBail(UUID bailId, int mois, int annee) {
        Bail bail = bailRepository.findById(bailId)
                .orElseThrow(() -> new ResourceNotFoundException("Bail introuvable : " + bailId));
        if (bail.getStatut() != StatutBail.ACTIF) {
            throw new BusinessRuleException("Le bail n'est pas actif");
        }
        if (!bailConcerneParMois(bail, mois, annee)) {
            throw new BusinessRuleException("Le bail ne couvre pas la période " + mois + "/" + annee);
        }
        if (echeanceRepository.existsByBailIdAndMoisAndAnnee(bailId, mois, annee)) {
            throw new BusinessRuleException("Une échéance existe déjà pour ce bail et cette période");
        }
        return versReponse(creer(bail, mois, annee));
    }

    /** Variante sans exception, utilisée à la création d'un bail : ne crée rien si le bail ne concerne pas ce mois. */
    @Transactional
    public Optional<Echeance> genererSiConcerne(Bail bail, int mois, int annee) {
        if (bail.getStatut() != StatutBail.ACTIF
                || !bailConcerneParMois(bail, mois, annee)
                || echeanceRepository.existsByBailIdAndMoisAndAnnee(bail.getId(), mois, annee)) {
            return Optional.empty();
        }
        return Optional.of(creer(bail, mois, annee));
    }

    /** Crée les échéances manquantes de tous les baux ACTIFS pour un mois (idempotent). */
    @Transactional
    public GenerationResultat genererPourMois(int mois, int annee) {
        int creees = 0;
        int dejaExistantes = 0;
        for (Bail bail : bailRepository.findByStatut(StatutBail.ACTIF)) {
            if (!bailConcerneParMois(bail, mois, annee)) {
                continue;
            }
            if (echeanceRepository.existsByBailIdAndMoisAndAnnee(bail.getId(), mois, annee)) {
                dejaExistantes++;
                continue;
            }
            creer(bail, mois, annee);
            creees++;
        }
        return new GenerationResultat(creees, dejaExistantes);
    }

    /** Fait passer A_PAYER -> EN_RETARD -> IMPAYEE selon la date du jour. Retourne le nombre de changements. */
    @Transactional
    public int mettreAJourStatuts() {
        LocalDate aujourdhui = LocalDate.now(clock);
        int modifiees = 0;
        for (Echeance e : echeanceRepository.findByStatutIn(List.of(StatutEcheance.A_PAYER, StatutEcheance.EN_RETARD))) {
            BigDecimal totalPaye = paiementRepository.totalPayePourEcheance(e.getId());
            StatutEcheance nouveau = calculateur.calculer(e, totalPaye, aujourdhui);
            if (nouveau != e.getStatut()) {
                e.setStatut(nouveau);
                echeanceRepository.save(e);
                modifiees++;
            }
        }
        return modifiees;
    }

    private Echeance creer(Bail bail, int mois, int annee) {
        Echeance e = new Echeance(bail.getId(), mois, annee, bail.getMontantLoyer());
        e.setStatut(calculateur.calculer(e, BigDecimal.ZERO, LocalDate.now(clock)));
        return echeanceRepository.save(e);
    }

    private boolean bailConcerneParMois(Bail bail, int mois, int annee) {
        YearMonth ym = YearMonth.of(annee, mois);
        LocalDate debutMois = ym.atDay(1);
        LocalDate finMois = ym.atEndOfMonth();
        return !bail.getDateDebut().isAfter(finMois)
                && (bail.getDateFin() == null || !bail.getDateFin().isBefore(debutMois));
    }

    private EcheanceResponse versReponse(Echeance e) {
        BigDecimal totalPaye = paiementRepository.totalPayePourEcheance(e.getId());
        return EcheanceResponse.from(e, calculateur.dateLimite(e.getMois(), e.getAnnee()), totalPaye);
    }
}
