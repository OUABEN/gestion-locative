package ma.gestionlocative.locataires.service;

import ma.gestionlocative.locataires.dto.LocataireQuittanceDto;
import ma.gestionlocative.locataires.dto.LocataireRequestDto;
import ma.gestionlocative.locataires.dto.LocataireResponseDto;
import ma.gestionlocative.locataires.model.Locataire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface LocataireService {

    // Méthodes du dossier de tests unitaires (UT-LOC-01 à UT-LOC-14)
    Locataire creerLocataire(Locataire locataire);

    Optional<Locataire> getById(UUID id);

    Optional<Locataire> getByCin(String cin);

    boolean modifierCoordonnees(UUID id, String email, String telephone);

    boolean modifier(UUID id, String email, String telephone);

    long nombreLocataires();

    boolean supprimer(UUID id);

    boolean existsByCin(String cin);

    // Méthodes applicatives REST (DTOs)
    LocataireResponseDto createLocataire(LocataireRequestDto requestDto);

    LocataireResponseDto findById(UUID id);

    LocataireResponseDto findByCin(String cin);

    Page<LocataireResponseDto> findAll(String search, Pageable pageable);

    LocataireResponseDto update(UUID id, LocataireRequestDto requestDto);

    void delete(UUID id);

    LocataireQuittanceDto getQuittanceSummary(UUID id);
}
