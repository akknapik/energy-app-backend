package com.energy_app.model.external;

import com.energy_app.model.enumeration.FuelType;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Fuel(
        @JsonProperty("fuel")
        FuelType fuelType,

        @JsonProperty("perc")
        Double percentage
) {
}
