package com.energy_app.controller;

import com.energy_app.model.dto.DailyMixDto;
import com.energy_app.model.dto.OptimalWindowDto;
import com.energy_app.model.external.ChargingRequest;
import com.energy_app.service.EnergyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/energy")
@CrossOrigin(origins = "http://localhost:4200")
public class EnergyController {
    private final EnergyService energyService;

    public EnergyController(EnergyService energyService) {
        this.energyService = energyService;
    }

    @GetMapping("/mix")
    ResponseEntity<List<DailyMixDto>> getGenerationMix() {
        return ResponseEntity.ok(energyService.getGenerationMixForThreeDays());
    }

    @PostMapping("/optimal-charging")
    ResponseEntity<OptimalWindowDto> getOptimalChargingWindow(@Valid @RequestBody ChargingRequest chargingRequest) {
        return ResponseEntity.ok(energyService.findOptimalChargingWindow(chargingRequest));
    }
}