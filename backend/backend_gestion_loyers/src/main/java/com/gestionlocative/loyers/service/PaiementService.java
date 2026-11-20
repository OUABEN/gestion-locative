package com.gestionlocative.loyers.service;

import com.gestionlocative.loyers.dto.CreatePaiementRequest;
import com.gestionlocative.loyers.dto.PaiementResponse;
import com.gestionlocative.loyers.exception.BusinessRuleException;
import com.gestionlocative.loyers.exception.ResourceNotFoundException;
import com.gestionlocative.loyers.model.Echeance;
import com.gestionlocative.loyers.model.Paiement;
import com.gestionlocative.loyers.model.StatutEcheance;
import com.gestionlocative.loyers.repository.EcheanceRepository;
import com.gestionlocative.loyers.repository.PaiementRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final EcheanceRepository echeanceRepository;
    private final EcheanceStatutCalculator calculateur;
    private final Clock clock;

    public PaiementService(PaiementRepository paiementRepository, EcheanceRepository echeanceRepository,
                           EcheanceStatutCalculator calculateur, Clock clock) {
        this.paiementRepository = paiementRepository;
        this.echeanceRepository = echeanceRepository;
        this.calculateur = calculateur;
        this.clock = clock;
    }

    /**
     * Enregistre un paiement (total ou partiel) puis recalcule le statut de l'échéance.
     * Une échéance en retard ou impayée peut toujours être réglée.
     */
    @Transactional
    public PaiementResponse enregistrer(CreatePaiementRequest request) {
        Echeance echeance = echeanceRepository.findById(request.echeanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Échéance introuvable : " + request.echeanceId()));

        if (echeance.getStatut() == StatutEcheance.PAYEE) {
            throw new BusinessRuleException("Cette échéance est déjà payée");
        }

        LocalDate aujourdhui = LocalDate.now(clock);
        LocalDate datePaiement = request.datePaiement() != null ? request.datePaiement() : aujourdhui;
        if (datePaiement.isAfter(aujourdhui)) {
            throw new BusinessRuleException("La date de paiement ne peut pas être dans le futur");
        }

        BigDecimal dejaPaye = paiementRepository.totalPayePourEcheance(echeance.getId());
        BigDecimal reste = echeance.getMontant().subtract(dejaPaye);
        if (request.montant().compareTo(reste) > 0) {
            throw new BusinessRuleException("Le montant dépasse le reste à payer (" + reste + ")");
        }

        Paiement paiement = paiementRepository.save(
                new Paiement(echeance.getId(), request.montant(), datePaiement, request.modePaiement()));

        echeance.setStatut(calculateur.calculer(echeance, dejaPaye.add(request.montant()), aujourdhui));
        echeanceRepository.save(echeance);

        return PaiementResponse.from(paiement);
    }

    @Transactional(readOnly = true)
    public PaiementResponse trouver(UUID id) {
        return paiementRepository.findById(id)
                .map(PaiementResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public List<PaiementResponse> listerParEcheance(UUID echeanceId) {
        if (!echeanceRepository.existsById(echeanceId)) {
            throw new ResourceNotFoundException("Échéance introuvable : " + echeanceId);
        }
        return paiementRepository.findByEcheanceIdOrderByDatePaiementAsc(echeanceId)
                .stream().map(PaiementResponse::from).toList();
    }

    /** Historique de tous les paiements d'un bail, du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<PaiementResponse> historiqueParBail(UUID bailId) {
        List<UUID> ids = echeanceRepository.findByBailIdOrderByAnneeAscMoisAsc(bailId)
                .stream().map(Echeance::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return paiementRepository.findByEcheanceIdInOrderByDatePaiementDesc(ids)
                .stream().map(PaiementResponse::from).toList();
    }
}
