package com.louiekury.pet.registration.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.annotation.JsonDeserialize;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class RegisterPetRequestDto {

    @NotBlank
    private final String name;
    @NotBlank
    private final String speciesId;
    private final String breedId;
    private final String profileMediaId;
    private final LocalDate birth;
    @NotBlank
    private final String sex;
    private final Boolean isNeutered;
    private final BigDecimal weight;
    private final Boolean basicVaccinationCompleted;

    public static RegisterPetRequestDto of(String name, String speciesId, String breedId,
            String profileMediaId, LocalDate birth, String sex, Boolean isNeutered,
            BigDecimal weight, Boolean basicVaccinationCompleted) {
        return RegisterPetRequestDto.builder().name(name).speciesId(speciesId).breedId(breedId)
            .profileMediaId(profileMediaId).birth(birth).sex(sex).isNeutered(isNeutered)
            .weight(weight).basicVaccinationCompleted(basicVaccinationCompleted).build();
    }

    @JsonCreator
    public static RegisterPetRequestDto fromJson(@JsonProperty("name") JsonNode name,
            @JsonProperty("speciesId") JsonNode speciesId,
            @JsonProperty("breedId") JsonNode breedId,
            @JsonProperty("profileMediaId") JsonNode profileMediaId,
            @JsonProperty("birth") JsonNode birth,
            @JsonProperty("sex") JsonNode sex,
            @JsonProperty("isNeutered") JsonNode isNeutered,
            @JsonProperty("weight") @JsonDeserialize(using = StrictDecimalDeserializer.class) BigDecimal weight,
            @JsonProperty("basicVaccinationCompleted") JsonNode basicVaccinationCompleted) {
        return of(text(name), text(speciesId), text(breedId), text(profileMediaId),
            date(birth), text(sex), bool(isNeutered), weight, bool(basicVaccinationCompleted));
    }

    @JsonAnySetter
    public void rejectUnknown(String field, JsonNode value) {
        throw invalid();
    }

    private static boolean absent(JsonNode value) {
        return value == null || value.isNull();
    }

    private static String text(JsonNode value) {
        if (absent(value)) {
            return null;
        }
        if (!value.isString()) {
            throw invalid();
        }
        return value.stringValue();
    }

    private static Boolean bool(JsonNode value) {
        if (absent(value)) {
            return null;
        }
        if (!value.isBoolean()) {
            throw invalid();
        }
        return value.booleanValue();
    }

    private static LocalDate date(JsonNode value) {
        String date = text(value);
        if (date == null) {
            return null;
        }
        if (!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw invalid();
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException exception) {
            throw invalid();
        }
    }

    private static BusinessException invalid() {
        return new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
}
