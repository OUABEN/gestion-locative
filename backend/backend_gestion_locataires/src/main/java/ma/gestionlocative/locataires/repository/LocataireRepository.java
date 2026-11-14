package ma.gestionlocative.locataires.repository;

import ma.gestionlocative.locataires.model.Locataire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocataireRepository extends JpaRepository<Locataire, UUID> {

    boolean existsByCin(String cin);

    Optional<Locataire> findByCin(String cin);

    Page<Locataire> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(String nom, String prenom, Pageable pageable);
}
