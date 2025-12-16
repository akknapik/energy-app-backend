package com.energy_app.service;

import com.energy_app.model.dto.DailyMixDto;
import com.energy_app.model.dto.FuelDto;
import com.energy_app.model.dto.OptimalWindowDto;
import com.energy_app.model.enumeration.FuelType;
import com.energy_app.model.external.CarbonIntensityResponse;
import com.energy_app.model.external.ChargingRequest;
import com.energy_app.model.external.Fuel;
import com.energy_app.model.external.GenerationData;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Service
public class EnergyService {
    private final RestClient restClient;

    Set<FuelType> CLEAN = EnumSet.of(
            FuelType.BIOMASS,
            FuelType.NUCLEAR,
            FuelType.HYDRO,
            FuelType.WIND,
            FuelType.SOLAR
    );

    public EnergyService() {
        restClient = RestClient.builder()
                .baseUrl("https://api.carbonintensity.org.uk")
                .build();
    }

    public List<DailyMixDto> getGenerationMixForThreeDays() {
        LocalDate today = LocalDate.now();
        String from = today.atStartOfDay().toString();
        String to = today.plusDays(3).atStartOfDay().toString();
        CarbonIntensityResponse carbonIntensityResponse = fetchGenerationMix(from, to);

        return calculateAveragesAndPercentage(carbonIntensityResponse);
    }

    public OptimalWindowDto findOptimalChargingWindow(ChargingRequest chargingRequest) {
        if(chargingRequest.numberOfHours() < 1 || chargingRequest.numberOfHours() > 6) {
            throw new IllegalArgumentException("Wrong number of hours");
        }

        OffsetDateTime start = snapToNextHalfHour(OffsetDateTime.now());
        OffsetDateTime end = start.plusHours(48);

        CarbonIntensityResponse carbonIntensityResponse = fetchGenerationMix(start.toString(), end.toString());

        List<GenerationData> intervals = carbonIntensityResponse.data();
        int windowSize = chargingRequest.numberOfHours() * 2;
        if(intervals.size() < windowSize) {
            throw new IllegalArgumentException("Not enough data from api");
        }

        return calculateBestWindow(intervals, windowSize);
    }

    private CarbonIntensityResponse fetchGenerationMix(String from, String to) {
        String extensionUrl = "/generation/{from}/{to}";

        URI uri = UriComponentsBuilder
                .fromUriString(extensionUrl)
                .buildAndExpand(from, to)
                .toUri();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(CarbonIntensityResponse.class);
    }

    private List<DailyMixDto> calculateAveragesAndPercentage(CarbonIntensityResponse carbonIntensityResponse) {
        Map<LocalDate, List<GenerationData>> groupedDays =
                carbonIntensityResponse.data().stream()
                        .filter(gd -> {
                            LocalDate dataDate = OffsetDateTime.parse(gd.from()).toLocalDate();
                            return !dataDate.isBefore(LocalDate.now());
                        })
                        .collect(groupingBy(gd -> OffsetDateTime.parse(gd.from()).toLocalDate()));

        Map<LocalDate, Map<FuelType, Double>> averageByFuelForDay = calculateAverageByFuelForDay(groupedDays);

        return averageByFuelForDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> toDailyMixDto(e.getKey(), e.getValue()))
                .toList();
    }

    private Map<LocalDate, Map<FuelType, Double>> calculateAverageByFuelForDay(Map<LocalDate, List<GenerationData>> groupedDays) {
        return groupedDays.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        value -> value.getValue().stream()
                                .flatMap(gd -> gd.generationMix().stream())
                                .collect(Collectors.groupingBy(
                                        Fuel::fuelType,
                                        averagingDouble(Fuel::percentage)
                                ))
                ));
    }

    private DailyMixDto toDailyMixDto(LocalDate day, Map<FuelType, Double> averageByFuel) {
        List<FuelDto> metrics = averageByFuel.entrySet().stream()
                .sorted(Map.Entry.<FuelType, Double>comparingByValue().reversed())
                .map(e -> new FuelDto(e.getKey(), round2(e.getValue())))
                .toList();

        double cleanPerc = round2(CLEAN.stream()
                .mapToDouble(ft -> averageByFuel.getOrDefault(ft, 0.0))
                .sum());

        return new DailyMixDto(day.toString(), metrics, cleanPerc);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private OffsetDateTime snapToNextHalfHour(OffsetDateTime time) {
        int minute = time.getMinute();
        OffsetDateTime baseTime;

        if(minute < 30) {
            baseTime = time.withMinute(0).withSecond(0).withNano(0);
        } else {
            baseTime = time.withMinute(30).withSecond(0).withNano(0);
        }

        return baseTime.plusMinutes(30);
    }

    private OptimalWindowDto calculateBestWindow(List<GenerationData> intervals, int windowSize) {
        double maxTotalPerc = -1.0;
        int bestStartIndex = -1;

        for(int i = 0; i <= intervals.size() - windowSize; i++) {
            double currentSum = 0;
            for(int j = 0; j < windowSize; j++) {
                currentSum += calculateCleanEnergyPercentageInInterval(intervals.get(i+j));
            }

            if(currentSum > maxTotalPerc) {
                maxTotalPerc = currentSum;
                bestStartIndex = i;
            }
        }

        GenerationData startInterval = intervals.get(bestStartIndex);
        GenerationData endInterval = intervals.get(bestStartIndex + windowSize - 1);
        double averagePerc = maxTotalPerc / windowSize;

        return new OptimalWindowDto(startInterval.from(), endInterval.to(), round2(averagePerc));
    }

    private double calculateCleanEnergyPercentageInInterval(GenerationData generationData) {
        return generationData.generationMix().stream()
                .filter(fuel -> CLEAN.contains(fuel.fuelType()))
                .mapToDouble(Fuel::percentage)
                .sum();
    }
}
