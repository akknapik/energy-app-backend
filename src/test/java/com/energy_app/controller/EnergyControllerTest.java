package com.energy_app.controller;

import com.energy_app.model.dto.DailyMixDto;
import com.energy_app.model.dto.FuelDto;
import com.energy_app.model.dto.OptimalWindowDto;
import com.energy_app.model.enumeration.FuelType;
import com.energy_app.model.external.ChargingRequest;
import com.energy_app.service.EnergyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(EnergyController.class)
public class EnergyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnergyService energyService;

    @TestConfiguration
    static class TestCacheConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("generationMix");
        }
    }

    @Test
    void getMix_returns200AndList() throws Exception {
        // given
        when(energyService.getGenerationMix()).thenReturn(List.of(
                new DailyMixDto("2025-12-19",
                        List.of(new FuelDto(FuelType.WIND, 70.0)),
                        70.0)
        ));

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/energy/mix"));

        // then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].date").value("2025-12-19"))
                .andExpect(jsonPath("$[0].cleanEnergyPercentage").value(70.0));
    }

    @Test
    void postOptimalCharging_returns200AndBody() throws Exception {
        // given
        when(energyService.findOptimalChargingWindow(new ChargingRequest(2)))
                .thenReturn(new OptimalWindowDto(
                        "2025-12-19T02:00+01:00",
                        "2025-12-19T04:00+01:00",
                        55.5
                ));

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/energy/optimal-charging")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"numberOfHours": 2}
                    """));

        // then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.startDateTime").value("2025-12-19T02:00+01:00"))
                .andExpect(jsonPath("$.endDateTime").value("2025-12-19T04:00+01:00"))
                .andExpect(jsonPath("$.percentage").value(55.5));
    }

}
