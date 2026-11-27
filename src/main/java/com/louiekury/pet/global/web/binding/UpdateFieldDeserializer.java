package com.louiekury.pet.global.web.binding;

import com.louiekury.pet.pet.adapter.in.web.dto.request.UpdateField;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;

public class UpdateFieldDeserializer extends ValueDeserializer<UpdateField<?>> {
    private final JavaType valueType;

    public UpdateFieldDeserializer() {
        this(null);
    }

    private UpdateFieldDeserializer(JavaType valueType) {
        this.valueType = valueType;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
        return new UpdateFieldDeserializer(property.getType().containedType(0));
    }

    @Override
    public UpdateField<?> deserialize(JsonParser parser, DeserializationContext context) {
        return UpdateField.of(context.readValue(parser, valueType));
    }

    @Override
    public Object getNullValue(DeserializationContext context) {
        return UpdateField.of(null);
    }

    @Override
    public Object getAbsentValue(DeserializationContext context) {
        return UpdateField.absent();
    }
}
