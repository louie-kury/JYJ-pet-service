package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.pet.adapter.in.web.dto.request.UpdatePetRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PetRequestBindingTest {
    private final JsonMapper mapper = JsonMapper.builder().disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    @Test
    void distinguishesOmittedNullAndValue() {
        UpdatePetRequest request = mapper.readValue("{\"weight\":null,\"name\":\"루이\",\"isNeutered\":false}", UpdatePetRequest.class);
        assertThat(request.hasWeight()).isTrue();
        assertThat(request.getWeight()).isNull();
        assertThat(request.getName()).isEqualTo("루이");
        assertThat(request.getIsNeutered()).isFalse();
        assertThat(request.hasBirth()).isFalse();
    }

    @Test
    void preservesDecimalPrecision() {
        UpdatePetRequest request = mapper.readValue("{\"weight\":200.00000000000000001}", UpdatePetRequest.class);
        assertThat(request.getWeight()).isEqualTo(new BigDecimal("200.00000000000000001"));
    }

    @Test
    void rejectsDuplicateFields() {
        assertThatThrownBy(() -> mapper.readValue("{\"name\":\"a\",\"name\":\"b\"}", UpdatePetRequest.class))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsUnknownFields() {
        assertThatThrownBy(() -> mapper.readValue("{\"unknown\":1}", UpdatePetRequest.class))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsInternalPresenceField() {
        assertThatThrownBy(() -> mapper.readValue("{\"namePresent\":true}", UpdatePetRequest.class))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsWrongFieldType() {
        assertThatThrownBy(() -> mapper.readValue("{\"weight\":\"2\"}", UpdatePetRequest.class))
            .isInstanceOf(RuntimeException.class);
    }
}
