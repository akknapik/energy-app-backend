package com.energy_app.client;

import com.energy_app.model.external.CarbonIntensityResponse;

public interface CarbonIntensityClient {
    CarbonIntensityResponse fetchGenerationMix(final String from, final String to);
}
