package com.gestionlocative.loyers.service;

import com.gestionlocative.loyers.client.BiensClient;
import com.gestionlocative.loyers.client.DisponibiliteBien;
import com.gestionlocative.loyers.client.LocatairesClient;
import com.gestionlocative.loyers.dto.BailResponse;
import com.gestionlocative.loyers.dto.CreateBailRequest;
import com.gestionlocative.loyers.exception.BusinessRuleException;
import com.gestionlocative.loyers.exception.ResourceNotFoundException;
import com.gestionlocative.loyers.model.Bail;
import com.gestionlocative.loyers.model.StatutBail;
import com.gestionlocative.loyers.repository.BailRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Le service Loyers possède l'entité Bail.
 * Création : locataire connu, bien disponible (service Biens), bail enregistré, bien passé à OCCUPE,
 * échéance du mois générée. Fin de bail : bien repassé à LIBRE.
 * Si un appel à Biens échoue, la transaction est annulée : aucun bail à moitié créé.
 */
@Service
public class BailService {

    private final BailRepository bailRepository;
    private final EcheanceService echeanceService;
    private final BiensClient biensClient;
    private final LocatairesClient locatairesClient;
    private final Clock clock;

    public BailService(BailRepository bailRepository, EcheanceService echeanceService, BiensClient biensClient,
                       LocatairesClient locatairesClient, Clock clock) {
        this.bailRepository = bailRepository;
        this.echeanceService = echeanceService;
        this.biensClient = biensClient;
        this.locatairesClient = locatairesClient;
        this.clock = clock;
    }

    @Transactional
    public BailResponse creer(CreateBailRequest r) {
        if (r.dateFin() != null && r.dateFin().isBefore(r.dateDebut())) {
            throw new BusinessRuleException("La date de fin ne peut pas précéder la date de début");
        }
        locatairesClient.trouver(r.locataireId())
                .orElseThrow(() -> new ResourceNotFoundException("Locataire introuvable : " + r.locataireId()));
        DisponibiliteBien dispo = biensClient.disponibilite(r.bienId())
                .orElseThrow(() -> new ResourceNotFoundException("Bien introuvable : " + r.bienId()));
        if (!dispo.disponible() || bailRepository.existsByBienIdAndStatut(r.bienId(), StatutBail.ACTIF)) {
            throw new BusinessRuleException("Le bien est déjà loué");
        }

        BigDecimal depot = r.depotGarantie() != null ? r.depotGarantie() : BigDecimal.ZERO;
        Bail bail;
        try {
            bail = bailRepository.saveAndFlush(new Bail(r.bienId(), r.locataireId(), r.dateDebut(),
                    r.dateFin(), r.montantLoyer(), depot));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleException("Le bien est déjà loué");
        }

        if (!biensClient.changerStatut(r.bienId(), "OCCUPE")) {
            throw new BusinessRuleException("Le bien est déjà occupé");
        }

        LocalDate aujourdhui = LocalDate.now(clock);
        echeanceService.genererSiConcerne(bail, aujourdhui.getMonthValue(), aujourdhui.getYear());
        return BailResponse.from(bail);
    }

    @Transactional
    public BailResponse terminer(UUID id, LocalDate dateFin) {
        Bail bail = bailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bail introuvable : " + id));
        if (bail.getStatut() == StatutBail.TERMINE) {
            throw new BusinessRuleException("Le bail est déjà terminé");
        }
        LocalDate fin = dateFin != null ? dateFin : LocalDate.now(clock);
        if (fin.isBefore(bail.getDateDebut())) {
            throw new BusinessRuleException("La date de fin ne peut pas précéder la date de début");
        }
        bail.setDateFin(fin);
        bail.setStatut(StatutBail.TERMINE);
        bailRepository.save(bail);

        // false = le bien était déjà LIBRE : sans importance ici
        biensClient.changerStatut(bail.getBienId(), "LIBRE");
        return BailResponse.from(bail);
    }

    @Transactional(readOnly = true)
    public BailResponse trouver(UUID id) {
        return bailRepository.findById(id)
                .map(BailResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Bail introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public List<BailResponse> lister(StatutBail statut, UUID bienId, UUID locataireId) {
        return bailRepository.findAll().stream()
                .filter(b -> statut == null || b.getStatut() == statut)
                .filter(b -> bienId == null || b.getBienId().equals(bienId))
                .filter(b -> locataireId == null || b.getLocataireId().equals(locataireId))
                .map(BailResponse::from)
                .toList();
    }
}
