package com.louiekury.pet.pet.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.louiekury.pet.vaccine.adapter.in.web.CreateVaccineRequestDto;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

final class RequestTestConverter {
    private RequestTestConverter() { }

    static JacksonJsonHttpMessageConverter create() {
        return new JacksonJsonHttpMessageConverter(JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .withConfigOverride(CreateVaccineRequestDto.class, config ->
                config.setIgnorals(JsonIgnoreProperties.Value.forIgnoreUnknown(true)))
            .build());
    }
}
