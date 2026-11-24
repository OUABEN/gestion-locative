package com.gestionlocative.loyers.client;

import com.gestionlocative.loyers.config.LoyersProperties;
import com.gestionlocative.loyers.exception.ServiceIndisponibleException;
import com.gestionlocative.loyers.security.CurrentUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class LocatairesRestClient implements LocatairesClient {

    private final RestClient restClient;

    public LocatairesRestClient(LoyersProperties props, RestClient.Builder builder) {
        this.restClient = builder.baseUrl(props.services().locatairesUrl()).build();
    }

    @Override
    public Optional<LocataireDto> trouver(UUID locataireId) {
        return lire("/api/locataires/" + locataireId);
    }

    @Override
    public Optional<LocataireDto> moi() {
        return lire("/api/locataires/me");
    }

    private Optional<LocataireDto> lire(String chemin) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri(chemin)
                    .header(HttpHeaders.AUTHORIZATION, CurrentUser.bearerHeader())
                    .retrieve()
                    .body(LocataireDto.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientResponseException e) {
            throw new ServiceIndisponibleException("Le service Locataires a répondu " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new ServiceIndisponibleException("Service Locataires indisponible", e);
        }
    }
}
