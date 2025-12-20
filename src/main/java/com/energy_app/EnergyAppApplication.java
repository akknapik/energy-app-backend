package com.energy_app;

import com.energy_app.config.CarbonIntensityApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@EnableConfigurationProperties(CarbonIntensityApiProperties.class)
@SpringBootApplication
@EnableCaching
public class EnergyAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnergyAppApplication.class, args);
	}

}
