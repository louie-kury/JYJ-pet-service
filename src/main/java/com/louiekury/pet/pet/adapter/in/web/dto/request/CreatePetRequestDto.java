package com.louiekury.pet.pet.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreatePetRequestDto {
    @NotBlank(message = "필수 값입니다.")
    private String name;
    @NotBlank(message = "필수 값입니다.")
    private String speciesId;
    private String breedId;
    private String profileMediaId;
    private LocalDate birth;
    @NotBlank(message = "필수 값입니다.")
    private String sex;
    private Boolean isNeutered;
    private BigDecimal weight;
    private Boolean basicVaccinationCompleted;

    public void setName(Object value) {
        this.name = RequestValue.text(value);
    }

    public void setSpeciesId(Object value) {
        this.speciesId = RequestValue.text(value);
    }

    public void setBreedId(Object value) {
        this.breedId = RequestValue.text(value);
    }

    public void setProfileMediaId(Object value) {
        this.profileMediaId = RequestValue.text(value);
    }

    public void setBirth(Object value) {
        this.birth = RequestValue.date(value);
    }

    public void setSex(Object value) {
        this.sex = RequestValue.text(value);
    }

    public void setIsNeutered(Object value) {
        this.isNeutered = RequestValue.bool(value);
    }

    public void setWeight(Object value) {
        this.weight = RequestValue.decimal(value);
    }

    public void setBasicVaccinationCompleted(Object value) {
        this.basicVaccinationCompleted = RequestValue.bool(value);
    }
}
