package com.energy_app.controller;

import com.energy_app.model.dto.DailyMixDto;
import com.energy_app.model.dto.OptimalWindowDto;
import com.energy_app.service.EnergyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/energy")
@Validated
@Tag(name = "Energy API", description = "Carbon Intensity (generation mix) and optimal charging window endpoints")
public class EnergyController {
    private final EnergyService energyService;

    public EnergyController(final EnergyService energyService) {
        this.energyService = energyService;
    }

    @Operation(summary = "Get generation mix",
            description = "Returns daily energy generation mix with clean energy percentages for configured number of days")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved generation mix"),
            @ApiResponse(responseCode = "503", description = "External Carbon Intensity API unavailable")
    })
    @GetMapping("/mix")
    ResponseEntity<List<DailyMixDto>> getGenerationMix() {
        return ResponseEntity.ok(energyService.getGenerationMix());
    }

    @Operation(summary = "Find optimal charging window",
            description = "Finds the best time window with highest clean energy percentage for EV charging")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully found optimal charging window"),
            @ApiResponse(responseCode = "400", description = "Invalid number of hours (must be 1-6)"),
            @ApiResponse(responseCode = "503", description = "External Carbon Intensity API unavailable")
    })
    @GetMapping("/optimal-charging")
    ResponseEntity<OptimalWindowDto> getOptimalChargingWindow(
            @RequestParam @Min(1) @Max(6) int numberOfHours
    ) {
        return ResponseEntity.ok(energyService.findOptimalChargingWindow(numberOfHours));
    }
}