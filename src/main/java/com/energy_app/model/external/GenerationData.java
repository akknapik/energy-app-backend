package com.energy_app.model.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GenerationData(
        String from,
        String to,

        @JsonProperty("generationmix")
        List<Fuel> generationMix
) {
}
