package com.louiekury.pet.global.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.louiekury.pet.vaccine.adapter.in.web.dto.request.CreateVaccineRequestDto;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class WebBindingConfig {
    @Bean
    JsonMapperBuilderCustomizer vaccineUnknownFields() {
        return WebBindingConfig::configure;
    }
    public static void configure(JsonMapper.Builder builder) {
        builder.withConfigOverride(CreateVaccineRequestDto.class,
            config -> config.setIgnorals(JsonIgnoreProperties.Value.forIgnoreUnknown(true)));
        builder.withCoercionConfig(LogicalType.Textual, config -> config
            .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
            .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
            .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}
