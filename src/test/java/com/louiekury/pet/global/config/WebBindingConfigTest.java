package com.louiekury.pet.global.config;

import com.louiekury.pet.vaccine.adapter.in.web.CreateVaccineRequestDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class WebBindingConfigTest {
    @Test
    void vaccineKeepsIgnoringUnknownFields() {
        var builder = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        new WebBindingConfig().vaccineUnknownFields().customize(builder);
        var request = builder.build().readValue("{\"name\":\"test\",\"extra\":1}", CreateVaccineRequestDto.class);
        assertThat(request.getName()).isEqualTo("test");
    }
}
