package com.louiekury.pet.pet.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.beans.ConstructorProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;

@Getter
public class CreatePetRequestDto {
    @NotBlank(message = "반려동물 이름은 필수입니다.")
    private final String name;
    @NotBlank(message = "반려동물 종은 필수입니다.")
    private final String speciesId;
    private final String breedId;
    private final String profileMediaId;
    private final LocalDate birth;
    @NotBlank(message = "반려동물 성별은 필수입니다.")
    private final String sex;
    private final Boolean isNeutered;
    private final BigDecimal weight;
    private final Boolean basicVaccinationCompleted;

    @ConstructorProperties({"name", "speciesId", "breedId", "profileMediaId", "birth", "sex",
        "isNeutered", "weight", "basicVaccinationCompleted"})
    public CreatePetRequestDto(String name, String speciesId, String breedId, String profileMediaId,
            LocalDate birth, String sex, Boolean isNeutered, BigDecimal weight, Boolean basicVaccinationCompleted) {
        this.name = name;
        this.speciesId = speciesId;
        this.breedId = breedId;
        this.profileMediaId = profileMediaId;
        this.birth = birth;
        this.sex = sex;
        this.isNeutered = isNeutered;
        this.weight = weight;
        this.basicVaccinationCompleted = basicVaccinationCompleted;
    }
}
