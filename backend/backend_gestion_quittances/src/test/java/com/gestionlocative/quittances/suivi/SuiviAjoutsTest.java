package com.gestionlocative.quittances.suivi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SuiviAjoutsTest {
    @Test
    void compareLesPaiementsEtRefuseLesSourcesIncoherentes() {
        assertDoesNotThrow(SuiviScenarios::comparaison);
    }

    @Test
    void exporteToutesLesPagesEtNeutraliseLesFormules() {
        assertDoesNotThrow(SuiviScenarios::csv);
    }

    @Test
    void controleLesDroitsAvantToutAppelExterne() {
        assertDoesNotThrow(SuiviScenarios::acces);
    }
}
