package com.louiekury.pet.registration.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.louiekury.pet.global.web.response.ApiTime;
import com.louiekury.pet.registration.application.model.PetResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PetDetailResponseDto {

    private final String petId;
    private final String name;
    private final PetResult.Species species;
    private final PetResult.Breed breed;
    private final Object profileMedia;
    private final LocalDate birth;
    private final PetResult.Age age;
    private final String sex;
    private final Boolean isNeutered;
    private final BigDecimal weight;
    private final Boolean basicVaccinationCompleted;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public static PetDetailResponseDto from(PetResult result) {
        return PetDetailResponseDto.builder().petId(result.getPetId()).name(result.getName())
            .species(result.getSpecies()).breed(result.getBreed()).profileMedia(null)
            .birth(result.getBirth()).age(result.getAge()).sex(result.getSex())
            .isNeutered(result.getIsNeutered()).weight(result.getWeight())
            .basicVaccinationCompleted(result.getBasicVaccinationCompleted())
            .createdAt(ApiTime.from(result.getCreatedAt())).updatedAt(ApiTime.from(result.getUpdatedAt()))
            .build();
    }
}
