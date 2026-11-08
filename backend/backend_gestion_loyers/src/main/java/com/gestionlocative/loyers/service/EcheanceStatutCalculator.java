package com.gestionlocative.loyers.service;

import com.gestionlocative.loyers.config.LoyersProperties;
import com.gestionlocative.loyers.model.Echeance;
import com.gestionlocative.loyers.model.StatutEcheance;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

/**
 * Règles de statut d'une échéance (logique pure, sans base de données) :
 * - PAYEE      : total payé >= montant
 * - A_PAYER    : pas encore dépassé la date limite
 * - EN_RETARD  : date limite dépassée, depuis moins de joursAvantImpaye jours
 * - IMPAYEE    : date limite dépassée depuis plus de joursAvantImpaye jours
 */
@Component
public class EcheanceStatutCalculator {

    private final LoyersProperties props;

    public EcheanceStatutCalculator(LoyersProperties props) {
        this.props = props;
    }

    /** Date limite de paiement du mois (le jour configuré, ramené à la fin du mois si besoin). */
    public LocalDate dateLimite(int mois, int annee) {
        YearMonth ym = YearMonth.of(annee, mois);
        return ym.atDay(Math.min(props.jourEcheance(), ym.lengthOfMonth()));
    }

    public StatutEcheance calculer(Echeance echeance, BigDecimal totalPaye, LocalDate aujourdhui) {
        if (totalPaye.compareTo(echeance.getMontant()) >= 0) {
            return StatutEcheance.PAYEE;
        }
        LocalDate limite = dateLimite(echeance.getMois(), echeance.getAnnee());
        if (!aujourdhui.isAfter(limite)) {
            return StatutEcheance.A_PAYER;
        }
        if (aujourdhui.isAfter(limite.plusDays(props.joursAvantImpaye()))) {
            return StatutEcheance.IMPAYEE;
        }
        return StatutEcheance.EN_RETARD;
    }
}
