package com.energy_app.model.enumeration;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FuelType {
    @JsonProperty("gas")
    GAS,

    @JsonProperty("coal")
    COAL,

    @JsonProperty("biomass")
    BIOMASS,

    @JsonProperty("nuclear")
    NUCLEAR,

    @JsonProperty("hydro")
    HYDRO,

    @JsonProperty("wind")
    WIND,

    @JsonProperty("solar")
    SOLAR,

    @JsonProperty("imports")
    IMPORTS,

    @JsonProperty("other")
    OTHER
}
