package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.dto.PageQuittancesResponse;
import com.gestionlocative.quittances.dto.QuittanceConsultationResponse;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.mapper.QuittanceConsultationMapper;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ConsultationQuittanceService {

    private static final Set<String> TRIS_AUTORISES = Set.of(
            "creeLe",
            "periodeDebut",
            "numero",
            "totalPaye"
    );

    private final QuittanceRepository repository;
    private final QuittanceConsultationMapper mapper;

    public ConsultationQuittanceService(
            QuittanceRepository repository,
            QuittanceConsultationMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public PageQuittancesResponse lister(
            Authentication authentication,
            int page,
            int taille,
            String tri,
            String sens
    ) {
        LecteurQuittances lecteur =
                LecteurQuittances.depuis(authentication);

        if (page < 0 || taille < 1 || taille > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La page doit être positive ou nulle, "
                            + "et la taille comprise entre 1 et 100."
            );
        }

        if (tri == null || !TRIS_AUTORISES.contains(tri)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tri autorisé : creeLe, periodeDebut, numero ou totalPaye."
            );
        }

        Sort.Direction direction;

        if ("asc".equalsIgnoreCase(sens)) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(sens)) {
            direction = Sort.Direction.DESC;
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le sens du tri doit être asc ou desc."
            );
        }

        Sort ordre = Sort.by(direction, tri)
                .and(Sort.by(direction, "id"));

        PageRequest pagination = PageRequest.of(page, taille, ordre);

        Page<Quittance> resultat = lecteur.gestionnaire()
                ? repository.findAll(pagination)
                : lecteur.bailleur()
                ? repository.findByBailleurId(
                lecteur.identifiant(),
                pagination
        )
                : repository.findByLocataireIdAndEmisLeIsNotNull(
                lecteur.identifiant(),
                pagination
        );

        return new PageQuittancesResponse(
                resultat.getContent().stream()
                        .map(mapper::convertir)
                        .toList(),
                resultat.getNumber(),
                resultat.getSize(),
                resultat.getTotalElements(),
                resultat.getTotalPages()
        );
    }

    public PageQuittancesResponse lister(Authentication authentication, int page, int taille,
            String tri, String sens, String bailId, String numero, String statut,
            java.time.LocalDate debut, java.time.LocalDate fin) {
        for (String valeur : new String[]{bailId, numero, statut}) {
            if (valeur != null && valeur.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtre trop long.");
        }
        if (debut != null && fin != null && fin.isBefore(debut)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intervalle de dates invalide.");
        LecteurQuittances lecteur =
                LecteurQuittances.depuis(authentication);

        if (page < 0 || taille < 1 || taille > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La page doit être positive ou nulle, "
                            + "et la taille comprise entre 1 et 100."
            );
        }

        if (tri == null || !TRIS_AUTORISES.contains(tri)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tri autorisé : creeLe, periodeDebut, numero ou totalPaye."
            );
        }

        Sort.Direction direction;

        if ("asc".equalsIgnoreCase(sens)) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(sens)) {
            direction = Sort.Direction.DESC;
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le sens du tri doit être asc ou desc."
            );
        }

        Sort ordre = Sort.by(direction, tri)
                .and(Sort.by(direction, "id"));

        PageRequest pagination = PageRequest.of(page, taille, ordre);

        org.springframework.data.jpa.domain.Specification<Quittance> filtre = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> conditions = new java.util.ArrayList<>();
            if (!lecteur.gestionnaire()) {
                conditions.add(cb.equal(root.get(lecteur.bailleur() ? "bailleurId" : "locataireId"), lecteur.identifiant()));
            }
            if (lecteur.locataire()) conditions.add(cb.isNotNull(root.get("emisLe")));
            if (bailId != null && !bailId.isBlank()) conditions.add(cb.equal(root.get("bailId"), bailId.trim()));
            if (numero != null && !numero.isBlank()) conditions.add(cb.equal(root.get("numero"), numero.trim()));
            if (statut != null && !statut.isBlank()) {
                try {
                    conditions.add(cb.equal(root.get("statut"), com.gestionlocative.quittances.entity.StatutQuittance.valueOf(statut)));
                } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut inconnu."); }
            }
            if (debut != null) conditions.add(cb.greaterThanOrEqualTo(root.get("periodeFin"), debut));
            if (fin != null) conditions.add(cb.lessThanOrEqualTo(root.get("periodeDebut"), fin));
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<Quittance> resultat = repository.findAll(filtre, pagination);

        return new PageQuittancesResponse(
                resultat.getContent().stream()
                        .map(mapper::convertir)
                        .toList(),
                resultat.getNumber(),
                resultat.getSize(),
                resultat.getTotalElements(),
                resultat.getTotalPages()
        );
    }

    public QuittanceConsultationResponse consulter(
            Authentication authentication,
            UUID id
    ) {
        LecteurQuittances lecteur =
                LecteurQuittances.depuis(authentication);

        Optional<Quittance> resultat = lecteur.gestionnaire()
                ? repository.findById(id)
                : lecteur.bailleur()
                ? repository.findByIdAndBailleurId(
                id,
                lecteur.identifiant()
        )
                : repository.findByIdAndLocataireIdAndEmisLeIsNotNull(
                id,
                lecteur.identifiant()
        );

        Quittance quittance = resultat.orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document introuvable."
                )
        );

        return mapper.convertir(quittance);
    }
}