package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.global.config.WebBindingConfig;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

public final class RequestTestConverter {
    private RequestTestConverter() { }

    public static JacksonJsonHttpMessageConverter create() {
        var builder = JsonMapper.builder().disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        WebBindingConfig.configure(builder);
        return new JacksonJsonHttpMessageConverter(builder.build());
    }
}
