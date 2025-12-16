package com.energy_app.model.external;

import java.util.List;

public record CarbonIntensityResponse(
        List<GenerationData> data
) {
}
