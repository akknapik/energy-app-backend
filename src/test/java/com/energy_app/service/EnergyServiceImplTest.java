package com.energy_app.service;

import com.energy_app.client.CarbonIntensityClient;
import com.energy_app.exception.ExternalApiException;
import com.energy_app.model.dto.DailyMixDto;
import com.energy_app.model.dto.OptimalWindowDto;
import com.energy_app.model.enumeration.FuelType;
import com.energy_app.model.external.CarbonIntensityResponse;
import com.energy_app.model.external.Fuel;
import com.energy_app.model.external.GenerationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnergyServiceImplTest {
    @Mock
    private CarbonIntensityClient carbonIntensityClient;

    private EnergyServiceImpl energyService;

    @BeforeEach
    void setUp() {
        int generationMixDays = 3;
        int searchWindowHours = 48;
        energyService = new EnergyServiceImpl(carbonIntensityClient, generationMixDays, searchWindowHours);
    }

    @Test
    void getGenerationMix_returnsNonEmptyList_whenApiReturnsData() {
        // given
        String d = LocalDate.now().toString();

        GenerationData i1 = new GenerationData(
                d + "T00:00+01:00",
                d + "T00:30+01:00",
                List.of(new Fuel(FuelType.WIND, 60.0), new Fuel(FuelType.GAS, 40.0))
        );
        GenerationData i2 = new GenerationData(
                d + "T00:30+01:00",
                d + "T01:00+01:00",
                List.of(new Fuel(FuelType.WIND, 80.0), new Fuel(FuelType.GAS, 20.0))
        );

        when(carbonIntensityClient.fetchGenerationMix(anyString(), anyString()))
                .thenReturn(new CarbonIntensityResponse(List.of(i1, i2)));

        // when
        List<DailyMixDto> result = energyService.getGenerationMix();

        // then
        assertFalse(result.isEmpty());
        assertEquals(70.0, result.get(0).cleanEnergyPercentage());
    }


    @Test
    void findOptimalChargingWindow_picksBestIntervals_forRequest() {
        // given
        int numberOfHours = 1;

        GenerationData i0 = new GenerationData(
                "2025-12-19T00:00+01:00",
                "2025-12-19T00:30+01:00",
                List.of(new Fuel(FuelType.WIND, 80.0), new Fuel(FuelType.GAS, 20.0))
        );
        GenerationData i1 = new GenerationData(
                "2025-12-19T00:30+01:00",
                "2025-12-19T01:00+01:00",
                List.of(new Fuel(FuelType.WIND, 90.0), new Fuel(FuelType.GAS, 10.0))
        );
        GenerationData i2 = new GenerationData(
                "2025-12-19T01:00+01:00",
                "2025-12-19T01:30+01:00",
                List.of(new Fuel(FuelType.WIND, 10.0), new Fuel(FuelType.GAS, 90.0))
        );

        when(carbonIntensityClient.fetchGenerationMix(anyString(), anyString()))
                .thenReturn(new CarbonIntensityResponse(List.of(i0, i1, i2)));

        // when
        OptimalWindowDto result = energyService.findOptimalChargingWindow(numberOfHours);

        // then
        assertEquals(new OptimalWindowDto(i0.from(), i0.to(), 80.0), result);
    }

    @Test
    void shouldThrowException_whenApiReturnsNull() {
        // given
        int numberOfHours = 5;
        when(carbonIntensityClient.fetchGenerationMix(anyString(), anyString())).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> energyService.findOptimalChargingWindow(numberOfHours))
                .isInstanceOf(ExternalApiException.class);
    }
}
