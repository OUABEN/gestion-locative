package com.gestionlocative.quittances.client;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.gestionlocative.quittances.dto.PaiementsPeriodeResponse;
import com.gestionlocative.quittances.dto.SituationPeriode.Affectation;
import com.gestionlocative.quittances.exception.RegleMetierException;
import com.gestionlocative.quittances.exception.ServiceExterneException;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class HttpPaiementClient implements PaiementClient {

    private final RestClient restClient;
    private final String chemin;

    public HttpPaiementClient(RestClient restClient, String chemin) {
        this.restClient = Objects.requireNonNull(restClient);
        this.chemin = Objects.requireNonNull(chemin);
    }

    @Override
    public List<Affectation> consulterAffectations(
            String bailId,
            LocalDate debut,
            LocalDate fin) {

        if (bailId == null || bailId.isBlank() || bailId.length() > 100) {
            throw new RegleMetierException(
                    "IDENTIFIANT_INVALIDE",
                    "L'identifiant du bail est invalide."
            );
        }

        if (debut == null || fin == null || fin.isBefore(debut)) {
            throw new RegleMetierException(
                    "PERIODE_INVALIDE",
                    "La période demandée est invalide."
            );
        }

        PaiementsPeriodeResponse reponse;

        try {
            reponse = restClient.get()
                    .uri(chemin, Map.of(
                            "bailId", bailId,
                            "debut", debut.toString(),
                            "fin", fin.toString()
                    ))
                    .retrieve()
                    .body(PaiementsPeriodeResponse.class);

        } catch (RestClientResponseException exception) {
            throw new ServiceExterneException(
                    "PAIEMENTS",
                    "ERREUR_HTTP_EXTERNE",
                    "Le service Paiements a refusé ou échoué à traiter la demande.",
                    exception.getStatusCode().value()
            );

        } catch (ResourceAccessException exception) {
            throw new ServiceExterneException(
                    "PAIEMENTS",
                    "SERVICE_INJOIGNABLE",
                    "Le service Paiements est inaccessible ou le délai est dépassé.",
                    null
            );

        } catch (RestClientException exception) {
            throw new ServiceExterneException(
                    "PAIEMENTS",
                    "REPONSE_ILLISIBLE",
                    "La réponse du service Paiements ne peut pas être interprétée.",
                    null
            );
        }

        if (reponse == null
                || !"1".equals(reponse.versionContrat())
                || !Boolean.TRUE.equals(reponse.complet())
                || !bailId.equals(reponse.bailId())
                || !debut.equals(reponse.periodeDebut())
                || !fin.equals(reponse.periodeFin())) {

            throw new ServiceExterneException(
                    "PAIEMENTS",
                    "REPONSE_INCOHERENTE",
                    "La réponse Paiements est vide, incomplète ou ne correspond pas à la demande.",
                    null
            );
        }

        return reponse.affectations();
    }
}