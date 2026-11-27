package com.louiekury.pet.global.web.validation;

import com.louiekury.pet.pet.adapter.in.web.dto.request.UpdateField;
import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;

public class UpdateFieldValueExtractor implements ValueExtractor<UpdateField<@ExtractedValue ?>> {
    @Override
    public void extractValues(UpdateField<?> field, ValueReceiver receiver) {
        if (field.isPresent()) {
            receiver.value(null, field.getValue());
        }
    }
}
