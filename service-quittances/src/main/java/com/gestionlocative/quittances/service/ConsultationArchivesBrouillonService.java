package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.dto.ArchiveBrouillonResponse;
import com.gestionlocative.quittances.dto.PageArchivesBrouillonResponse;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ConsultationArchivesBrouillonService {
    private final QuittanceRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public ConsultationArchivesBrouillonService(
            QuittanceRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageArchivesBrouillonResponse consulter(
            Authentication authentication, UUID id, int page, int taille) {
        LecteurQuittances lecteur = LecteurQuittances.depuis(authentication);
        if (!lecteur.bailleur()) {
            throw new AccessDeniedException(
                    "Les archives de brouillons sont réservées au bailleur.");
        }
        if (page < 0 || page > 1_000_000 || taille < 1 || taille > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La page doit être comprise entre 0 et 1000000 et la taille entre 1 et 100.");
        }
        repository.findByIdAndBailleurId(id, lecteur.identifiant())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document introuvable."));

        Long nombre = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM document_fichier
                WHERE quittance_id = ? AND nature = 'BROUILLON'
                """, Long.class, id);
        if (nombre == null) {
            throw new IllegalStateException("Le décompte des archives est absent.");
        }
        var contenu = jdbcTemplate.query("""
                SELECT id, quittance_id, version_document, nature,
                       sha256, taille_octets, cree_le
                FROM document_fichier
                WHERE quittance_id = ? AND nature = 'BROUILLON'
                ORDER BY cree_le DESC, id DESC
                LIMIT ? OFFSET ?
                """, (rs, ligne) -> new ArchiveBrouillonResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("quittance_id", UUID.class),
                        rs.getLong("version_document"),
                        rs.getString("nature"),
                        rs.getString("sha256"),
                        rs.getLong("taille_octets"),
                        rs.getTimestamp("cree_le").toInstant()
                ), id, taille, (long) page * taille);
        long pages = nombre / taille + (nombre % taille == 0 ? 0 : 1);
        return new PageArchivesBrouillonResponse(contenu, page, taille, nombre, pages);
    }
}
