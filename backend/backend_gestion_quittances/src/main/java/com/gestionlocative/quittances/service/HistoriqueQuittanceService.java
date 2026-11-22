package com.gestionlocative.quittances.service;

import com.gestionlocative.quittances.dto.PageHistoriqueResponse;
import com.gestionlocative.quittances.repository.HistoriqueQuittanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class HistoriqueQuittanceService {

    private final ConsultationQuittanceService consultationService;
    private final HistoriqueQuittanceRepository historiqueRepository;

    public HistoriqueQuittanceService(
            ConsultationQuittanceService consultationService,
            HistoriqueQuittanceRepository historiqueRepository
    ) {
        this.consultationService = consultationService;
        this.historiqueRepository = historiqueRepository;
    }

    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public PageHistoriqueResponse consulter(
            Authentication authentication,
            UUID quittanceId,
            int page,
            int taille
    ) {
        consultationService.consulter(authentication, quittanceId);

        if (page < 0 || taille < 1 || taille > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La page doit être positive ou nulle, "
                            + "et la taille comprise entre 1 et 100."
            );
        }

        long decalage = (long) page * taille;
        long total = historiqueRepository.compter(quittanceId);

        var evenements = historiqueRepository.consulter(
                quittanceId,
                taille,
                decalage
        );

        long totalPages = total / taille;

        if (total % taille != 0) {
            totalPages++;
        }

        return new PageHistoriqueResponse(
                evenements,
                page,
                taille,
                total,
                totalPages
        );
    }
}