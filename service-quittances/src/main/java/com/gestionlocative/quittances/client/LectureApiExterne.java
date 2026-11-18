package com.gestionlocative.quittances.client;

import java.util.Map;

import com.gestionlocative.quittances.exception.RegleMetierException;
import com.gestionlocative.quittances.exception.ServiceExterneException;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public final class LectureApiExterne {

    private LectureApiExterne() {
    }

    public static <T> T lire(
            RestClient client,
            String service,
            String chemin,
            Map<String, ?> parametres,
            Class<T> type) {

        try {
            T resultat = client.get()
                    .uri(chemin, parametres)
                    .retrieve()
                    .body(type);

            if (resultat == null) {
                throw incoherence(service);
            }

            return resultat;

        } catch (RestClientResponseException exception) {
            throw new ServiceExterneException(
                    service,
                    "ERREUR_HTTP_EXTERNE",
                    "Le service externe a refusé ou échoué à traiter la demande.",
                    exception.getStatusCode().value()
            );

        } catch (ResourceAccessException exception) {
            throw new ServiceExterneException(
                    service,
                    "SERVICE_INJOIGNABLE",
                    "Le service externe est inaccessible ou le délai est dépassé.",
                    null
            );

        } catch (RestClientException exception) {
            throw new ServiceExterneException(
                    service,
                    "REPONSE_ILLISIBLE",
                    "La réponse du service externe ne peut pas être interprétée.",
                    null
            );
        }
    }

    public static void verifierIdentifiant(String id) {
        if (!identifiantValide(id)) {
            throw new RegleMetierException(
                    "IDENTIFIANT_INVALIDE",
                    "L'identifiant demandé est invalide."
            );
        }
    }

    public static boolean identifiantValide(String id) {
        return id != null && !id.isBlank() && id.length() <= 100;
    }

    public static boolean textePresent(String texte) {
        return texte != null && !texte.isBlank();
    }

    public static ServiceExterneException incoherence(String service) {
        return new ServiceExterneException(
                service,
                "REPONSE_INCOHERENTE",
                "La réponse externe est incomplète ou ne correspond pas à la demande.",
                null
        );
    }
}