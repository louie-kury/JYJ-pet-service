package com.louiekury.pet.pet.adapter.in.web.dto.request;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

@Getter
@JsonDeserialize(using = UpdatePetRequest.Deserializer.class)
public class UpdatePetRequest {
    private final Map<String, Object> changes;

    private UpdatePetRequest(Map<String, Object> changes) {
        this.changes = changes;
    }

    public static class Deserializer extends ValueDeserializer<UpdatePetRequest> {
        private static final Set<String> TEXT = Set.of("name", "speciesId", "breedId", "profileMediaId", "sex");
        private static final Set<String> BOOL = Set.of("isNeutered", "basicVaccinationCompleted");

        @Override
        public UpdatePetRequest deserialize(JsonParser parser, DeserializationContext context) {
            if (parser.currentToken() != JsonToken.START_OBJECT) throw invalid();
            Map<String, Object> changes = new LinkedHashMap<>();
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() != JsonToken.PROPERTY_NAME) throw invalid();
                String key = parser.currentName();
                JsonToken token = parser.nextToken();
                if ((!TEXT.contains(key) && !BOOL.contains(key) && !"birth".equals(key) && !"weight".equals(key))
                        || changes.containsKey(key)) throw invalid();
                if (token == JsonToken.VALUE_NULL) {
                    changes.put(key, null);
                } else if (TEXT.contains(key) && token == JsonToken.VALUE_STRING) {
                    changes.put(key, parser.getString());
                } else if (BOOL.contains(key) && token.isBoolean()) {
                    changes.put(key, parser.getBooleanValue());
                } else if ("weight".equals(key) && token.isNumeric()) {
                    changes.put(key, parser.getDecimalValue());
                } else if ("birth".equals(key) && token == JsonToken.VALUE_STRING) {
                    String text = parser.getString();
                    if (!text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw invalid();
                    try {
                        changes.put(key, LocalDate.parse(text));
                    } catch (DateTimeParseException e) {
                        throw invalid();
                    }
                } else {
                    throw invalid();
                }
            }
            return new UpdatePetRequest(changes);
        }

        private static BusinessException invalid() {
            return new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
