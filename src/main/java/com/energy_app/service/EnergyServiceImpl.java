package com.energy_app.service;

import com.energy_app.client.CarbonIntensityClient;
import com.energy_app.exception.ExternalApiException;
import com.energy_app.model.dto.DailyMixDto;
import com.energy_app.model.dto.FuelDto;
import com.energy_app.model.dto.OptimalWindowDto;
import com.energy_app.model.enumeration.FuelType;
import com.energy_app.model.external.CarbonIntensityResponse;
import com.energy_app.model.external.Fuel;
import com.energy_app.model.external.GenerationData;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.energy_app.config.CleanFuels.CLEAN;
import static java.util.stream.Collectors.*;

@Service
public class EnergyServiceImpl implements EnergyService {
    private final CarbonIntensityClient carbonIntensityClient;

    private final int generationMixDays;
    private final int searchWindowHours;

    public EnergyServiceImpl(CarbonIntensityClient carbonIntensityClient,
                             @Value("${energy.app.generation-mix.days}") int generationMixDays,
                             @Value("${energy.app.optimization.search-window-hours}") int searchWindowHours) {
        this.carbonIntensityClient = carbonIntensityClient;
        this.generationMixDays = generationMixDays;
        this.searchWindowHours = searchWindowHours;
    }

    public List<DailyMixDto> getGenerationMix() {
        LocalDate today = LocalDate.now();
        String from = today.atStartOfDay().toString();
        String to = today.plusDays(generationMixDays).atStartOfDay().toString();
        CarbonIntensityResponse carbonIntensityResponse = getCarbonIntensityResponse(from, to);

        return calculateAveragesAndPercentage(carbonIntensityResponse);
    }

    public OptimalWindowDto findOptimalChargingWindow(int numberOfHours) {
        OffsetDateTime start = snapToNextHalfHour(OffsetDateTime.now());

        /* Search window is a rolling 48 hours from the next half-hour slot.
          Using “next 2 calendar days” would truncate today's remaining hours (e.g. morning requests)
          and could miss an optimal window later today, while also not providing a full second day of data. */
        OffsetDateTime end = start.plusHours(searchWindowHours);
        CarbonIntensityResponse carbonIntensityResponse = getCarbonIntensityResponse(start.toString(),
                end.toString());

        List<GenerationData> intervals = carbonIntensityResponse.data();

        /* Each interval represents 30 minutes, so 1 hour equals 2 intervals. */
        int windowSize = numberOfHours * 2;
        if(intervals.size() < windowSize) {
            throw new IllegalArgumentException("Not enough data from api.");
        }

        return calculateOptimalWindow(intervals, windowSize);
    }

    private CarbonIntensityResponse getCarbonIntensityResponse(final String from, final String to) {
        CarbonIntensityResponse carbonIntensityResponse = carbonIntensityClient.fetchGenerationMix(from, to);

        if(carbonIntensityResponse == null || carbonIntensityResponse.data() == null) {
            throw new ExternalApiException("Received empty data from Carbon Intensity API.");
        }

        return carbonIntensityResponse;
    }

    private List<DailyMixDto> calculateAveragesAndPercentage(CarbonIntensityResponse carbonIntensityResponse) {
        Map<LocalDate, List<GenerationData>> groupedDays =
                carbonIntensityResponse.data().stream()
                        .filter(gd -> {
                            LocalDate dataDate = OffsetDateTime.parse(gd.from()).toLocalDate();

                            /* When requesting data from today 00:00, the API may include the 23:30–00:00 interval,
                              which belongs to the previous day. Filter it out to avoid mixing yesterday into
                              today's results. */
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

    private static double round2(final double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /* Returns the start of the next interval so the search for the optimal charging window begins on a full slot
    rather than an interval that is already in progress.
     */
    private OffsetDateTime snapToNextHalfHour(final OffsetDateTime time) {
        int minute = time.getMinute();
        OffsetDateTime baseTime;

        if(minute < 30) {
            baseTime = time.withMinute(30).withSecond(0).withNano(0);
        } else {
            baseTime = time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        }

        return baseTime.plusMinutes(30);
    }

    private OptimalWindowDto calculateOptimalWindow(@NotNull List<GenerationData> intervals, int windowSize) {
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

        if(bestStartIndex == -1) {
            throw new IllegalArgumentException("Could not find optimal charging window.");
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
