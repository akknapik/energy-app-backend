package com.energy_app.model.dto;

import java.util.List;

public record DailyMixDto(
        String date,
        List<FuelDto> metrics,
        double cleanEnergyPercentage
) {
}
