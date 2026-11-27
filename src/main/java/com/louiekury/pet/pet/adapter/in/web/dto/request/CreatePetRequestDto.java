package com.louiekury.pet.pet.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreatePetRequestDto {
    @NotBlank(message = "반려동물 이름은 필수입니다.")
    private String name;
    @NotBlank(message = "반려동물 종은 필수입니다.")
    private String speciesId;
    private String breedId;
    private String profileMediaId;
    private LocalDate birth;
    @NotBlank(message = "반려동물 성별은 필수입니다.")
    private String sex;
    private Boolean isNeutered;
    private BigDecimal weight;
    private Boolean basicVaccinationCompleted;

    public void setName(String name) {
        this.name = name;
    }

    public void setSpeciesId(String speciesId) {
        this.speciesId = speciesId;
    }

    public void setBreedId(String breedId) {
        this.breedId = breedId;
    }

    public void setProfileMediaId(String profileMediaId) {
        this.profileMediaId = profileMediaId;
    }

    public void setBirth(LocalDate birth) {
        this.birth = birth;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public void setIsNeutered(Boolean isNeutered) {
        this.isNeutered = isNeutered;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public void setBasicVaccinationCompleted(Boolean basicVaccinationCompleted) {
        this.basicVaccinationCompleted = basicVaccinationCompleted;
    }
}
