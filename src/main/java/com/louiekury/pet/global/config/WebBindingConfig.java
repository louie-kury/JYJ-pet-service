package com.louiekury.pet.global.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.louiekury.pet.vaccine.adapter.in.web.CreateVaccineRequestDto;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebBindingConfig {
    @Bean
    JsonMapperBuilderCustomizer vaccineUnknownFields() {
        return builder -> builder.withConfigOverride(CreateVaccineRequestDto.class,
            config -> config.setIgnorals(JsonIgnoreProperties.Value.forIgnoreUnknown(true)));
    }
}
