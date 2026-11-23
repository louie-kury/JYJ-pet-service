package com.louiekury.pet.pet.application.createpet;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class CreatePetCommand {

    private final String name;
    private final String speciesId;
    private final String breedId;
    private final String profileMediaId;
    private final LocalDate birth;
    private final String sex;
    private final Boolean isNeutered;
    private final BigDecimal weight;
    private final Boolean basicVaccinationCompleted;
    private final String idempotencyKey;

    public static CreatePetCommand of(String name, String speciesId, String breedId,
            String profileMediaId, LocalDate birth, String sex, Boolean isNeutered,
            BigDecimal weight, Boolean basicVaccinationCompleted, String idempotencyKey) {
        return CreatePetCommand.builder().name(name).speciesId(speciesId).breedId(breedId)
            .profileMediaId(profileMediaId).birth(birth).sex(sex).isNeutered(isNeutered)
            .weight(weight).basicVaccinationCompleted(basicVaccinationCompleted)
            .idempotencyKey(idempotencyKey).build();
    }
}
