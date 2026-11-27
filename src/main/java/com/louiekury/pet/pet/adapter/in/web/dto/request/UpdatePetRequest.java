package com.louiekury.pet.pet.adapter.in.web.dto.request;

import java.beans.ConstructorProperties;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdatePetRequest {
    private final UpdateField<String> name;
    private final UpdateField<String> speciesId;
    private final UpdateField<String> breedId;
    private final UpdateField<String> profileMediaId;
    private final UpdateField<LocalDate> birth;
    private final UpdateField<String> sex;
    private final UpdateField<Boolean> isNeutered;
    private final UpdateField<BigDecimal> weight;
    private final UpdateField<Boolean> basicVaccinationCompleted;

    @ConstructorProperties({"name", "speciesId", "breedId", "profileMediaId", "birth", "sex",
        "isNeutered", "weight", "basicVaccinationCompleted"})
    public UpdatePetRequest(
            UpdateField<String> name,
            UpdateField<String> speciesId,
            UpdateField<String> breedId,
            UpdateField<String> profileMediaId,
            UpdateField<LocalDate> birth,
            UpdateField<String> sex,
            UpdateField<Boolean> isNeutered,
            UpdateField<BigDecimal> weight,
            UpdateField<Boolean> basicVaccinationCompleted
    ) {
        this.name = name == null ? UpdateField.absent() : name;
        this.speciesId = speciesId == null ? UpdateField.absent() : speciesId;
        this.breedId = breedId == null ? UpdateField.absent() : breedId;
        this.profileMediaId = profileMediaId == null ? UpdateField.absent() : profileMediaId;
        this.birth = birth == null ? UpdateField.absent() : birth;
        this.sex = sex == null ? UpdateField.absent() : sex;
        this.isNeutered = isNeutered == null ? UpdateField.absent() : isNeutered;
        this.weight = weight == null ? UpdateField.absent() : weight;
        this.basicVaccinationCompleted = basicVaccinationCompleted == null ? UpdateField.absent() : basicVaccinationCompleted;
    }

    public String getName() {
        return name.getValue();
    }

    public boolean hasName() {
        return name.isPresent();
    }

    public String getSpeciesId() {
        return speciesId.getValue();
    }

    public boolean hasSpeciesId() {
        return speciesId.isPresent();
    }

    public String getBreedId() {
        return breedId.getValue();
    }

    public boolean hasBreedId() {
        return breedId.isPresent();
    }

    public String getProfileMediaId() {
        return profileMediaId.getValue();
    }

    public boolean hasProfileMediaId() {
        return profileMediaId.isPresent();
    }

    public LocalDate getBirth() {
        return birth.getValue();
    }

    public boolean hasBirth() {
        return birth.isPresent();
    }

    public String getSex() {
        return sex.getValue();
    }

    public boolean hasSex() {
        return sex.isPresent();
    }

    public Boolean getIsNeutered() {
        return isNeutered.getValue();
    }

    public boolean hasIsNeutered() {
        return isNeutered.isPresent();
    }

    public BigDecimal getWeight() {
        return weight.getValue();
    }

    public boolean hasWeight() {
        return weight.isPresent();
    }

    public Boolean getBasicVaccinationCompleted() {
        return basicVaccinationCompleted.getValue();
    }

    public boolean hasBasicVaccinationCompleted() {
        return basicVaccinationCompleted.isPresent();
    }
}
