package com.energy_app.client;

import com.energy_app.exception.ExternalApiException;
import com.energy_app.model.external.CarbonIntensityResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(CarbonIntensityClientImpl.class)
@Import(CarbonIntensityClientTest.TestConfig.class)
class CarbonIntensityClientTest {

    @Autowired
    private CarbonIntensityClient  client;

    @Autowired
    private MockRestServiceServer server;

    @TestConfiguration
    static class TestConfig {

        @Bean
        RestClient restClient(RestClient.Builder builder) {
            return builder.build();
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("generationMix");
        }
    }

    @Test
    void shouldFetchAndParseData_whenApiReturns200() {
        // given
        String jsonResponse = """
            {
              "data": [
                {
                  "from": "2023-01-01T12:00Z",
                  "to": "2023-01-01T12:30Z",
                  "generationmix": []
                }
              ]
            }
            """;

        server.expect(requestTo(org.hamcrest.Matchers.containsString("/generation")))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // when
        CarbonIntensityResponse response = client.fetchGenerationMix("2023-01-01T00:00Z", "2023-01-01T23:00Z");

        // then
        assertThat(response).isNotNull();
        assertThat(response.data()).hasSize(1);
    }

    @Test
    void shouldThrowExternalApiException_whenApiReturns500() {
        // given
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/generation")))
                .andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> client.fetchGenerationMix("start", "end"))
                .isInstanceOf(ExternalApiException.class);
    }
}