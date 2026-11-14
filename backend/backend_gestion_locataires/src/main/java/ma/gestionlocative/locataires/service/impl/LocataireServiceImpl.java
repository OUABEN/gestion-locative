package ma.gestionlocative.locataires.service.impl;

import ma.gestionlocative.locataires.dto.LocataireQuittanceDto;
import ma.gestionlocative.locataires.dto.LocataireRequestDto;
import ma.gestionlocative.locataires.dto.LocataireResponseDto;
import ma.gestionlocative.locataires.exception.CinDejaUtiliseException;
import ma.gestionlocative.locataires.exception.ResourceNotFoundException;
import ma.gestionlocative.locataires.model.Locataire;
import ma.gestionlocative.locataires.repository.LocataireRepository;
import ma.gestionlocative.locataires.service.LocataireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class LocataireServiceImpl implements LocataireService {

    private final LocataireRepository repository;
    private final Map<UUID, Locataire> inMemoryStorage = new ConcurrentHashMap<>();

    public LocataireServiceImpl() {
        this.repository = null;
    }

    @Autowired(required = false)
    public LocataireServiceImpl(LocataireRepository repository) {
        this.repository = repository;
    }

    @Override
    public Locataire creerLocataire(Locataire locataire) {
        if (locataire == null) {
            throw new IllegalArgumentException("Le locataire ne peut pas être null");
        }
        if (locataire.getCin() == null || locataire.getCin().trim().isEmpty()) {
            throw new IllegalArgumentException("Le CIN est obligatoire");
        }
        String normalizedCin = locataire.getCin().trim().toUpperCase();
        if (existsByCin(normalizedCin)) {
            throw new CinDejaUtiliseException("Fiche refusée : CIN déjà utilisé (" + normalizedCin + ")");
        }
        if (locataire.getEmail() != null && !locataire.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Format d'email invalide");
        }
        if (locataire.getId() == null) {
            locataire.setId(UUID.randomUUID());
        }
        locataire.setCin(normalizedCin);
        if (locataire.getCreatedAt() == null) {
            locataire.setCreatedAt(LocalDateTime.now());
        }
        locataire.setUpdatedAt(LocalDateTime.now());

        if (repository != null) {
            return repository.save(locataire);
        } else {
            inMemoryStorage.put(locataire.getId(), locataire);
            return locataire;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Locataire> getById(UUID id) {
        if (id == null) return Optional.empty();
        if (repository != null) {
            return repository.findById(id);
        }
        return Optional.ofNullable(inMemoryStorage.get(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Locataire> getByCin(String cin) {
        if (cin == null || cin.trim().isEmpty()) return Optional.empty();
        String normalized = cin.trim().toUpperCase();
        if (repository != null) {
            return repository.findByCin(normalized);
        }
        return inMemoryStorage.values().stream()
                .filter(l -> normalized.equalsIgnoreCase(l.getCin()))
                .findFirst();
    }

    @Override
    public boolean modifierCoordonnees(UUID id, String email, String telephone) {
        if (id == null) return false;
        Optional<Locataire> opt = getById(id);
        if (opt.isEmpty()) return false;

        Locataire loc = opt.get();
        if (email != null) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("Format d'email invalide");
            }
            loc.setEmail(email);
        }
        if (telephone != null) {
            loc.setTelephone(telephone);
        }
        loc.setUpdatedAt(LocalDateTime.now());

        if (repository != null) {
            repository.save(loc);
        } else {
            inMemoryStorage.put(loc.getId(), loc);
        }
        return true;
    }

    @Override
    public boolean modifier(UUID id, String email, String telephone) {
        return modifierCoordonnees(id, email, telephone);
    }

    @Override
    @Transactional(readOnly = true)
    public long nombreLocataires() {
        if (repository != null) {
            return repository.count();
        }
        return inMemoryStorage.size();
    }

    @Override
    public boolean supprimer(UUID id) {
        if (id == null) return false;
        Optional<Locataire> opt = getById(id);
        if (opt.isEmpty()) return false;

        if (repository != null) {
            repository.deleteById(id);
        } else {
            inMemoryStorage.remove(id);
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCin(String cin) {
        if (cin == null) return false;
        String normalized = cin.trim().toUpperCase();
        if (repository != null) {
            return repository.existsByCin(normalized);
        }
        return inMemoryStorage.values().stream().anyMatch(l -> normalized.equalsIgnoreCase(l.getCin()));
    }

    @Override
    public LocataireResponseDto createLocataire(LocataireRequestDto dto) {
        Locataire loc = Locataire.builder()
                .nom(dto.getNom().trim())
                .prenom(dto.getPrenom().trim())
                .cin(dto.getCin().trim().toUpperCase())
                .email(dto.getEmail().trim())
                .telephone(dto.getTelephone().trim())
                .build();
        return mapToDto(creerLocataire(loc));
    }

    @Override
    @Transactional(readOnly = true)
    public LocataireResponseDto findById(UUID id) {
        return getById(id).map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Locataire introuvable avec l'identifiant : " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public LocataireResponseDto findByCin(String cin) {
        return getByCin(cin).map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Locataire introuvable avec le CIN : " + cin));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LocataireResponseDto> findAll(String search, Pageable pageable) {
        if (repository == null) throw new UnsupportedOperationException("Pagination réservée au mode repository JPA");
        if (search != null && !search.trim().isEmpty()) {
            return repository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(search.trim(), search.trim(), pageable).map(this::mapToDto);
        }
        return repository.findAll(pageable).map(this::mapToDto);
    }

    @Override
    public LocataireResponseDto update(UUID id, LocataireRequestDto requestDto) {
        boolean updated = modifierCoordonnees(id, requestDto.getEmail(), requestDto.getTelephone());
        if (!updated) {
            throw new ResourceNotFoundException("Locataire introuvable avec l'identifiant : " + id);
        }
        return findById(id);
    }

    @Override
    public void delete(UUID id) {
        if (!supprimer(id)) {
            throw new ResourceNotFoundException("Locataire introuvable avec l'identifiant : " + id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LocataireQuittanceDto getQuittanceSummary(UUID id) {
        Locataire loc = getById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Locataire introuvable pour la quittance (ID: " + id + ")"));

        return LocataireQuittanceDto.builder()
                .id(loc.getId())
                .nom(loc.getNom())
                .prenom(loc.getPrenom())
                .nomComplet(loc.getPrenom() + " " + loc.getNom())
                .cin(loc.getCin())
                .email(loc.getEmail())
                .telephone(loc.getTelephone())
                .build();
    }

    private LocataireResponseDto mapToDto(Locataire l) {
        return LocataireResponseDto.builder()
                .id(l.getId())
                .nom(l.getNom())
                .prenom(l.getPrenom())
                .cin(l.getCin())
                .email(l.getEmail())
                .telephone(l.getTelephone())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }
}
