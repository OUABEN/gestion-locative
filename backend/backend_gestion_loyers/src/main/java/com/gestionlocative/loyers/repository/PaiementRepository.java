package com.gestionlocative.loyers.repository;

import com.gestionlocative.loyers.model.Paiement;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaiementRepository extends JpaRepository<Paiement, UUID> {

    List<Paiement> findByEcheanceIdOrderByDatePaiementAsc(UUID echeanceId);

    List<Paiement> findByEcheanceIdInOrderByDatePaiementDesc(Collection<UUID> echeanceIds);

    @Query("select sum(p.montant) from Paiement p where p.echeanceId = :echeanceId")
    BigDecimal sommePourEcheance(@Param("echeanceId") UUID echeanceId);

    /** Total déjà payé pour une échéance (0 si aucun paiement). */
    default BigDecimal totalPayePourEcheance(UUID echeanceId) {
        BigDecimal somme = sommePourEcheance(echeanceId);
        return somme == null ? BigDecimal.ZERO : somme;
    }
}
