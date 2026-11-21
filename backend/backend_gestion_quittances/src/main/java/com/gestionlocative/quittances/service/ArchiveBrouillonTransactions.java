package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.document.PdfStocke;
import com.gestionlocative.quittances.dto.ArchiveBrouillonResponse;
import com.gestionlocative.quittances.dto.DonneesApercu;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.security.LecteurQuittances;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class ArchiveBrouillonTransactions {

    private final PreparationApercuService preparationService;
    private final QuittanceRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public ArchiveBrouillonTransactions(
            PreparationApercuService preparationService,
            QuittanceRepository repository,
            JdbcTemplate jdbcTemplate
    ) {
        this.preparationService = preparationService;
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public Source charger(Authentication authentication, UUID id) {
        Acteur acteur = identifier(authentication);

        DonneesApercu donnees = preparationService.charger(
                authentication,
                id
        );

        Quittance document = repository.findByIdAndBailleurId(
                id,
                acteur.bailleurId()
        ).orElseThrow(this::introuvable);

        if (archiveExiste(id, document.getVersion())) {
            throw conflit(
                    "Un brouillon est déjà archivé pour cette version."
            );
        }

        return new Source(donnees, document.getVersion());
    }

    @Transactional(timeout = 10)
    public ArchiveBrouillonResponse enregistrer(
            Authentication authentication,
            UUID quittanceId,
            long versionAttendue,
            PdfStocke fichier
    ) {
        Acteur acteur = identifier(authentication);

        Quittance document = repository.trouverPourModification(
                quittanceId,
                acteur.bailleurId()
        ).orElseThrow(this::introuvable);

        if (!Objects.equals(
                document.getVersion(),
                Long.valueOf(versionAttendue)
        )) {
            throw conflit(
                    "Le document a changé pendant la génération du PDF."
            );
        }

        String statut = document.getStatut().name();

        boolean autorise =
                "BROUILLON".equals(statut)
                        || "EN_ATTENTE_VALIDATION".equals(statut)
                        || "EN_ATTENTE_SIGNATURE".equals(statut);

        if (!autorise || document.getEmisLe() != null) {
            throw conflit(
                    "Ce document ne peut plus être archivé comme brouillon."
            );
        }

        if (archiveExiste(quittanceId, versionAttendue)) {
            throw conflit(
                    "Un brouillon est déjà archivé pour cette version."
            );
        }

        UUID archiveId = UUID.randomUUID();
        Instant date = Instant.now();

        jdbcTemplate.update("""
                INSERT INTO document_fichier (
                    id,
                    quittance_id,
                    version_document,
                    nature,
                    reference_stockage,
                    sha256,
                    taille_octets,
                    cree_le
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                archiveId,
                quittanceId,
                versionAttendue,
                "BROUILLON",
                fichier.reference(),
                fichier.sha256(),
                fichier.tailleOctets(),
                Timestamp.from(date)
        );

        jdbcTemplate.update("""
                INSERT INTO quittance_audit (
                    id,
                    quittance_id,
                    acteur,
                    emetteur_identite,
                    operation,
                    resultat,
                    horodatage,
                    statut_avant,
                    statut_apres
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                quittanceId,
                acteur.sujet(),
                acteur.emetteur(),
                "ARCHIVAGE_BROUILLON",
                "SUCCES",
                Timestamp.from(date),
                statut,
                statut
        );

        return new ArchiveBrouillonResponse(
                archiveId,
                quittanceId,
                versionAttendue,
                "BROUILLON",
                fichier.sha256(),
                fichier.tailleOctets(),
                date
        );
    }

    @Transactional(readOnly = true)
    public Fichier consulterFichier(
            Authentication authentication,
            UUID quittanceId,
            UUID archiveId
    ) {
        Acteur acteur = identifier(authentication);

        repository.findByIdAndBailleurId(
                quittanceId,
                acteur.bailleurId()
        ).orElseThrow(this::introuvable);

        var fichiers = jdbcTemplate.query(
                """
                SELECT reference_stockage, sha256
                FROM document_fichier
                WHERE id = ?
                  AND quittance_id = ?
                  AND nature = 'BROUILLON'
                """,
                (resultat, numeroLigne) -> new Fichier(
                        resultat.getString("reference_stockage"),
                        resultat.getString("sha256")
                ),
                archiveId,
                quittanceId
        );

        if (fichiers.isEmpty()) {
            throw introuvable();
        }

        return fichiers.getFirst();
    }

    private boolean archiveExiste(UUID quittanceId, long version) {
        Long nombre = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM document_fichier
                WHERE quittance_id = ?
                  AND version_document = ?
                  AND nature = 'BROUILLON'
                """,
                Long.class,
                quittanceId,
                version
        );

        return nombre.longValue() > 0;
    }

    private Acteur identifier(Authentication authentication) {
        LecteurQuittances lecteur =
                LecteurQuittances.depuis(authentication);

        if (!lecteur.bailleur()) {
            throw new AccessDeniedException(
                    "Les archives de brouillons sont réservées au bailleur."
            );
        }

        JwtAuthenticationToken jwt =
                (JwtAuthenticationToken) authentication;

        String sujet = jwt.getToken().getSubject();
        String emetteur = jwt.getToken().getClaimAsString("iss");

        if (sujet == null
                || sujet.isBlank()
                || sujet.length() > 200
                || emetteur == null
                || emetteur.isBlank()
                || emetteur.length() > 500) {
            throw new AccessDeniedException(
                    "L'identité ne permet pas une traçabilité suffisante."
            );
        }

        return new Acteur(
                lecteur.identifiant(),
                sujet,
                emetteur
        );
    }

    private ResponseStatusException introuvable() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Document ou archive introuvable."
        );
    }

    private ResponseStatusException conflit(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }

    public record Source(
            DonneesApercu donnees,
            long version
    ) {
    }

    public record Fichier(
            String reference,
            String sha256
    ) {
    }

    private record Acteur(
            String bailleurId,
            String sujet,
            String emetteur
    ) {
    }
}