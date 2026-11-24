package com.gestionlocative.loyers;

import com.gestionlocative.loyers.config.LoyersProperties;
import com.gestionlocative.loyers.model.Bail;
import com.gestionlocative.loyers.model.Echeance;
import com.gestionlocative.loyers.model.StatutBail;
import com.gestionlocative.loyers.model.StatutEcheance;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/** Données de test partagées : règles par défaut = échéance le 5, impayé après 30 jours. */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static LoyersProperties props() {
        return new LoyersProperties(5, 30,
                new LoyersProperties.Services("http://localhost:8082", "http://localhost:8081"));
    }

    public static Clock clockAt(LocalDate date) {
        return Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    }

    public static Echeance echeance(String montant, int mois, int annee, StatutEcheance statut) {
        Echeance e = new Echeance(UUID.randomUUID(), mois, annee, new BigDecimal(montant));
        e.setId(UUID.randomUUID());
        e.setStatut(statut);
        return e;
    }

    public static Bail bail(String statut, LocalDate debut, LocalDate fin, String loyer) {
        Bail b = new Bail(UUID.randomUUID(), UUID.randomUUID(), debut, fin, new BigDecimal(loyer), BigDecimal.ZERO);
        b.setId(UUID.randomUUID());
        b.setStatut(StatutBail.valueOf(statut));
        return b;
    }
}
