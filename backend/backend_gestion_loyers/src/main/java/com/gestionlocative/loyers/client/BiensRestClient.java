package com.gestionlocative.loyers.client;

import com.gestionlocative.loyers.config.LoyersProperties;
import com.gestionlocative.loyers.exception.ResourceNotFoundException;
import com.gestionlocative.loyers.exception.ServiceIndisponibleException;
import com.gestionlocative.loyers.security.CurrentUser;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class BiensRestClient implements BiensClient {

    private final RestClient restClient;

    public BiensRestClient(LoyersProperties props, RestClient.Builder builder) {
        this.restClient = builder.baseUrl(props.services().biensUrl()).build();
    }

    @Override
    public Optional<DisponibiliteBien> disponibilite(UUID bienId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/biens/{id}/disponibilite", bienId)
                    .header(HttpHeaders.AUTHORIZATION, CurrentUser.bearerHeader())
                    .retrieve()
                    .body(DisponibiliteBien.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientResponseException e) {
            throw new ServiceIndisponibleException("Le service Biens a répondu " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new ServiceIndisponibleException("Service Biens indisponible", e);
        }
    }

    @Override
    public boolean changerStatut(UUID bienId, String statut) {
        try {
            restClient.patch()
                    .uri("/api/biens/{id}/statut", bienId)
                    .header(HttpHeaders.AUTHORIZATION, CurrentUser.bearerHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("statut", statut))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.Conflict e) {
            return false;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Bien introuvable : " + bienId);
        } catch (RestClientResponseException e) {
            throw new ServiceIndisponibleException("Le service Biens a répondu " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new ServiceIndisponibleException("Service Biens indisponible", e);
        }
    }
}
