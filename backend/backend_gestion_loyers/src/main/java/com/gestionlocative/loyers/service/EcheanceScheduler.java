package com.gestionlocative.loyers.service;

import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Tâches automatiques : génération mensuelle des échéances et passage en retard / impayé. */
@Component
public class EcheanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(EcheanceScheduler.class);

    private final EcheanceService echeanceService;
    private final Clock clock;

    public EcheanceScheduler(EcheanceService echeanceService, Clock clock) {
        this.echeanceService = echeanceService;
        this.clock = clock;
    }

    /** Le 1er de chaque mois à 01:00. */
    @Scheduled(cron = "0 0 1 1 * *")
    public void genererEcheancesDuMois() {
        LocalDate d = LocalDate.now(clock);
        try {
            var r = echeanceService.genererPourMois(d.getMonthValue(), d.getYear());
            log.info("Échéances {}/{} : {} créées, {} déjà existantes", d.getMonthValue(), d.getYear(),
                    r.creees(), r.dejaExistantes());
        } catch (RuntimeException e) {
            log.error("Échec de la génération mensuelle des échéances", e);
        }
    }

    /** Tous les jours à 01:30. */
    @Scheduled(cron = "0 30 1 * * *")
    public void mettreAJourStatuts() {
        try {
            int n = echeanceService.mettreAJourStatuts();
            log.info("Mise à jour des statuts : {} échéance(s) modifiée(s)", n);
        } catch (RuntimeException e) {
            log.error("Échec de la mise à jour des statuts", e);
        }
    }
}
