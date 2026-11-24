package com.gestionlocative.quittances.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.gestionlocative.quittances.dto.SituationPeriode.Nature;
import com.gestionlocative.quittances.dto.SituationPeriode.StatutPaiement;
import com.gestionlocative.quittances.exception.ServiceExterneException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PaiementsCollectifsClientTest {
    private static final String BAIL = "aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa";
    private static final String ECHEANCE = "bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb";
    private static final String PAIEMENT = "cccccccc-cccc-4ccc-cccc-cccccccccccc";

    @Test
    void transformeUnPaiementReelEnAffectationDeLoyer() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8083");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:8083/api/echeances?bailId=" + BAIL))
                .andRespond(withSuccess(echeances("3000.00", "3000.00", "0.00"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:8083/api/paiements?bailId=" + BAIL))
                .andRespond(withSuccess(paiements("3000.00"), MediaType.APPLICATION_JSON));
        var liste = new PaiementsCollectifsClient(builder.build()).consulterAffectations(
                BAIL, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        assertEquals(1, liste.size());
        assertEquals(Nature.LOYER, liste.get(0).nature());
        assertEquals(StatutPaiement.CONFIRME, liste.get(0).statut());
        assertEquals(UUID.fromString(PAIEMENT).toString(), liste.get(0).paiementId());
        server.verify();
    }

    @Test
    void refuseUneEcheanceDontLeTotalNeCorrespondPasAuxPaiements() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8083");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:8083/api/echeances?bailId=" + BAIL))
                .andRespond(withSuccess(echeances("3000.00", "3000.00", "0.00"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:8083/api/paiements?bailId=" + BAIL))
                .andRespond(withSuccess(paiements("1500.00"), MediaType.APPLICATION_JSON));
        assertThrows(ServiceExterneException.class,
                () -> new PaiementsCollectifsClient(builder.build()).consulterAffectations(
                        BAIL, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
        server.verify();
    }

    private static String echeances(String montant, String total, String reste) {
        return "[{\"id\":\"" + ECHEANCE + "\",\"bailId\":\"" + BAIL
                + "\",\"mois\":10,\"annee\":2026,\"montant\":" + montant
                + ",\"statut\":\"PAYEE\",\"totalPaye\":" + total
                + ",\"resteAPayer\":" + reste + "}]";
    }

    private static String paiements(String montant) {
        return "[{\"id\":\"" + PAIEMENT + "\",\"echeanceId\":\"" + ECHEANCE
                + "\",\"montant\":" + montant
                + ",\"datePaiement\":\"2026-10-15\"}]";
    }
}
