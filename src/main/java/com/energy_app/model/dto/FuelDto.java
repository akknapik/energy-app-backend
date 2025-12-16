package com.energy_app.model.dto;

import com.energy_app.model.enumeration.FuelType;

public record FuelDto(
        FuelType fuelType,
        double percentage
) {
}
