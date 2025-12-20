package com.energy_app.service;

import com.energy_app.model.dto.DailyMixDto;
import com.energy_app.model.dto.OptimalWindowDto;
import com.energy_app.model.external.ChargingRequest;

import java.util.List;

public interface EnergyService {
    List<DailyMixDto> getGenerationMix();
    OptimalWindowDto findOptimalChargingWindow(ChargingRequest chargingRequest);
}
