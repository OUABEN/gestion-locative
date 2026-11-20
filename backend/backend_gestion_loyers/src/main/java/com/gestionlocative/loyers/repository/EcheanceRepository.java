package com.gestionlocative.loyers.repository;

import com.gestionlocative.loyers.model.Echeance;
import com.gestionlocative.loyers.model.StatutEcheance;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EcheanceRepository extends JpaRepository<Echeance, UUID> {

    List<Echeance> findByBailIdOrderByAnneeAscMoisAsc(UUID bailId);

    Optional<Echeance> findByBailIdAndMoisAndAnnee(UUID bailId, Integer mois, Integer annee);

    boolean existsByBailIdAndMoisAndAnnee(UUID bailId, Integer mois, Integer annee);

    List<Echeance> findByStatutIn(Collection<StatutEcheance> statuts);

    List<Echeance> findByStatut(StatutEcheance statut);
}
