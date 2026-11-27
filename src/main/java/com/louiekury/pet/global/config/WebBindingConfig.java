package com.louiekury.pet.global.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.louiekury.pet.global.web.binding.UpdateFieldDeserializer;
import com.louiekury.pet.pet.adapter.in.web.dto.request.UpdateField;
import com.louiekury.pet.vaccine.adapter.in.web.dto.request.CreateVaccineRequestDto;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class WebBindingConfig {
    @Bean
    JsonMapperBuilderCustomizer vaccineUnknownFields() {
        return WebBindingConfig::configure;
    }
    public static void configure(JsonMapper.Builder builder) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(UpdateField.class, new UpdateFieldDeserializer());
        builder.addModule(module);
        builder.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION);
        builder.withConfigOverride(CreateVaccineRequestDto.class,
            config -> config.setIgnorals(JsonIgnoreProperties.Value.forIgnoreUnknown(true)));
        builder.withCoercionConfig(LogicalType.Textual, config -> config
            .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
            .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
            .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}
