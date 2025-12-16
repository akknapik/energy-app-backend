package com.energy_app.model.dto;

public record OptimalWindowDto(
        String startDateTime,
        String endDateTime,
        double percentage
) {
}
