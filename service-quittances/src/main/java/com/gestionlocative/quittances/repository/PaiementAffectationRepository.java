package com.gestionlocative.quittances.repository;

import java.util.List;
import java.util.UUID;

import com.gestionlocative.quittances.entity.PaiementAffectation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaiementAffectationRepository
        extends JpaRepository<PaiementAffectation, UUID> {

    List<PaiementAffectation> findByQuittance_IdOrderByAffectationExterneIdAsc(
            UUID quittanceId
    );
}