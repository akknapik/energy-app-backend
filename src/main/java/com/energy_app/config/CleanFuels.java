package com.energy_app.config;

import com.energy_app.model.enumeration.FuelType;

import java.util.EnumSet;
import java.util.Set;

public class CleanFuels {
    private CleanFuels() {}

    public static final Set<FuelType> CLEAN = EnumSet.of(
            FuelType.BIOMASS,
            FuelType.NUCLEAR,
            FuelType.HYDRO,
            FuelType.WIND,
            FuelType.SOLAR
    );
}
