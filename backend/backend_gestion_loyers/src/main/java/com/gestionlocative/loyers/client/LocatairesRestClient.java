package com.gestionlocative.loyers.client;

import com.gestionlocative.loyers.config.LoyersProperties;
import com.gestionlocative.loyers.exception.ServiceIndisponibleException;
import com.gestionlocative.loyers.security.JwtService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class LocatairesRestClient implements LocatairesClient {

    private final RestClient restClient;
    private final JwtService jwtService;

    public LocatairesRestClient(LoyersProperties props, JwtService jwtService, RestClient.Builder builder) {
        this.restClient = builder.baseUrl(props.services().locatairesUrl()).build();
        this.jwtService = jwtService;
    }

    @Override
    public Optional<BailDto> trouverBail(UUID bailId) {
        try {
            BailDto bail = restClient.get()
                    .uri("/api/baux/{id}", bailId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.genererTokenService())
                    .retrieve()
                    .body(BailDto.class);
            return Optional.ofNullable(bail);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ServiceIndisponibleException("Service Locataires indisponible", e);
        }
    }

    @Override
    public List<BailDto> listerBauxActifs() {
        try {
            List<BailDto> baux = restClient.get()
                    .uri(uri -> uri.path("/api/baux").queryParam("statut", "ACTIF").build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.genererTokenService())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<BailDto>>() {
                    });
            return baux == null ? List.of() : baux;
        } catch (RestClientException e) {
            throw new ServiceIndisponibleException("Service Locataires indisponible", e);
        }
    }
}
