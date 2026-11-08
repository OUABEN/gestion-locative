package com.gestionlocative.loyers.repository;

import com.gestionlocative.loyers.model.Paiement;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaiementRepository extends JpaRepository<Paiement, UUID> {

    List<Paiement> findByEcheanceIdOrderByDatePaiementAsc(UUID echeanceId);

    @Query("select coalesce(sum(p.montant), 0) from Paiement p where p.echeanceId = :echeanceId")
    BigDecimal totalPayePourEcheance(@Param("echeanceId") UUID echeanceId);
}
