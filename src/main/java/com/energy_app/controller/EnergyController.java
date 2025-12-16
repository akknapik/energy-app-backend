package com.energy_app.controller;

import com.energy_app.model.dto.DailyMixDto;
import com.energy_app.service.EnergyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/energy")
public class EnergyController {
    private final EnergyService energyService;

    public EnergyController(EnergyService energyService) {
        this.energyService = energyService;
    }

    @GetMapping("/mix")
    ResponseEntity<List<DailyMixDto>> getGenerationMix() {
        return ResponseEntity.ok(energyService.getGenerationMixForThreeDays());
    }
}
