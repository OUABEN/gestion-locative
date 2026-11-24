package com.gestionlocative.loyers.repository;

import com.gestionlocative.loyers.model.Bail;
import com.gestionlocative.loyers.model.StatutBail;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BailRepository extends JpaRepository<Bail, UUID> {

    List<Bail> findByStatut(StatutBail statut);

    List<Bail> findByLocataireIdOrderByDateDebutDesc(UUID locataireId);

    boolean existsByBienIdAndStatut(UUID bienId, StatutBail statut);
}
