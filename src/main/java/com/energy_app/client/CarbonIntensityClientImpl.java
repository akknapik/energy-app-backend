package com.energy_app.client;

import com.energy_app.exception.ExternalApiException;
import com.energy_app.model.external.CarbonIntensityResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class CarbonIntensityClientImpl implements CarbonIntensityClient {
    private final RestClient restClient;

    public CarbonIntensityClientImpl(final RestClient restClient) {
        this.restClient = restClient;
    }

    @Cacheable(cacheNames = "generationMix",  key = "#from + '-' + #to")
    public CarbonIntensityResponse fetchGenerationMix(final String from, final String to) {
        final String extensionUrl = "/generation/{from}/{to}";

        final URI uri = UriComponentsBuilder
                .fromUriString(extensionUrl)
                .buildAndExpand(from, to)
                .toUri();
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(CarbonIntensityResponse.class);
        } catch (Exception exception) {
            throw new ExternalApiException("Failed to connect to Carbon Intensity API.", exception);
        }
    }
}
