package com.energy_app.model.external;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChargingRequest(
        @NotNull
        @Min(1)
        @Max(6)
        int numberOfHours
) {
}
