package com.gestionlocative.quittances;

import com.gestionlocative.quittances.service.EmissionReelleTransactions.Fichier;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EmissionReelleFichierTest {

    private final Instant date = Instant.parse("2026-11-24T14:00:00Z");

    @Test
    void fichiersAvecMemeContenuSontEgaux() {
        var premier = new Fichier(
                new byte[]{1, 2, 3}, null, date, "QT-2026-000001");
        var second = new Fichier(
                new byte[]{1, 2, 3}, null, date, "QT-2026-000001");

        assertEquals(premier, second);
        assertEquals(premier.hashCode(), second.hashCode());
    }

    @Test
    void fichiersAvecContenusDifferentsNeSontPasEgaux() {
        var premier = new Fichier(
                new byte[]{1, 2, 3}, null, date, "QT-2026-000001");
        var second = new Fichier(
                new byte[]{1, 2, 4}, null, date, "QT-2026-000001");

        assertNotEquals(premier, second);
    }
}