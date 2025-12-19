package com.energy_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CarbonIntensityConfig {

    @Bean
    RestClient carbonIntensityRestClient(RestClient.Builder builder, CarbonIntensityProperties properties) {
        return builder
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
