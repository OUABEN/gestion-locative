package com.gestionlocative.quittances;

import com.gestionlocative.quittances.document.SignatureLocale;
import com.gestionlocative.quittances.service.EmissionDemoTransactions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SonarRecordEqualityTest {
    @Test
    void preuveCompareLeContenuDesTableaux() {
        var premiere = new SignatureLocale.Preuve(new byte[]{1, 2}, new byte[]{3, 4}, "abc");
        var identique = new SignatureLocale.Preuve(new byte[]{1, 2}, new byte[]{3, 4}, "abc");
        var differente = new SignatureLocale.Preuve(new byte[]{1, 9}, new byte[]{3, 4}, "abc");

        assertEquals(premiere, identique);
        assertEquals(premiere.hashCode(), identique.hashCode());
        assertNotEquals(premiere, differente);
        assertTrue(premiere.toString().contains("tailleSignature=2"));
    }

    @Test
    void fichierCompareLePdfEtLaPreuveParContenu() {
        var date = Instant.parse("2026-11-23T00:00:00Z");
        var preuveA = new SignatureLocale.Preuve(new byte[]{1}, new byte[]{2}, "abc");
        var preuveB = new SignatureLocale.Preuve(new byte[]{1}, new byte[]{2}, "abc");
        var premier = new EmissionDemoTransactions.Fichier(new byte[]{37, 80, 68, 70}, preuveA, date, "QT-1");
        var identique = new EmissionDemoTransactions.Fichier(new byte[]{37, 80, 68, 70}, preuveB, date, "QT-1");
        var different = new EmissionDemoTransactions.Fichier(new byte[]{37, 80, 0, 70}, preuveA, date, "QT-1");

        assertEquals(premier, identique);
        assertEquals(premier.hashCode(), identique.hashCode());
        assertNotEquals(premier, different);
        assertTrue(premier.toString().contains("taillePdf=4"));
        assertFalse(premier.toString().contains("37, 80, 68, 70"));
    }
}
