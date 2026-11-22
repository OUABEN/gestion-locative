package com.gestionlocative.quittances.suivi;
import org.junit.jupiter.api.Test;
class SuiviAjoutsTest {
    @Test void compareLesPaiementsEtRefuseLesSourcesIncoherentes() { SuiviScenarios.comparaison(); }
    @Test void exporteToutesLesPagesEtNeutraliseLesFormules() { SuiviScenarios.csv(); }
    @Test void controleLesDroitsAvantToutAppelExterne() { SuiviScenarios.acces(); }
}
