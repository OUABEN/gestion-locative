package com.gestionlocative.quittances.repository;

import com.gestionlocative.quittances.entity.Quittance;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface QuittanceRepository
        extends JpaRepository<Quittance, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Quittance> {

    Page<Quittance> findByBailleurId(
            String bailleurId,
            Pageable pageable
    );

    Optional<Quittance> findByIdAndBailleurId(
            UUID id,
            String bailleurId
    );

    Page<Quittance> findByLocataireIdAndEmisLeIsNotNull(
            String locataireId,
            Pageable pageable
    );

    Optional<Quittance> findByIdAndLocataireIdAndEmisLeIsNotNull(
            UUID id,
            String locataireId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select q
            from Quittance q
            where q.id = :id
              and q.bailleurId = :bailleurId
            """)
    Optional<Quittance> trouverPourModification(
            @Param("id") UUID id,
            @Param("bailleurId") String bailleurId
    );
}