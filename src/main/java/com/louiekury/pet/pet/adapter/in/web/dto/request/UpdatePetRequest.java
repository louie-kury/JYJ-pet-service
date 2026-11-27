package com.louiekury.pet.pet.adapter.in.web.dto.request;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UpdatePetRequest {
    private String name;
    private boolean namePresent;
    private String speciesId;
    private boolean speciesIdPresent;
    private String breedId;
    private boolean breedIdPresent;
    private String profileMediaId;
    private boolean profileMediaIdPresent;
    private LocalDate birth;
    private boolean birthPresent;
    private String sex;
    private boolean sexPresent;
    private Boolean isNeutered;
    private boolean isNeuteredPresent;
    private BigDecimal weight;
    private boolean weightPresent;
    private Boolean basicVaccinationCompleted;
    private boolean basicVaccinationCompletedPresent;

    public void setName(String name) {
        rejectDuplicate(namePresent);
        this.name = name;
        this.namePresent = true;
    }

    public void setSpeciesId(String speciesId) {
        rejectDuplicate(speciesIdPresent);
        this.speciesId = speciesId;
        this.speciesIdPresent = true;
    }

    public void setBreedId(String breedId) {
        rejectDuplicate(breedIdPresent);
        this.breedId = breedId;
        this.breedIdPresent = true;
    }

    public void setProfileMediaId(String profileMediaId) {
        rejectDuplicate(profileMediaIdPresent);
        this.profileMediaId = profileMediaId;
        this.profileMediaIdPresent = true;
    }

    public void setBirth(LocalDate birth) {
        rejectDuplicate(birthPresent);
        this.birth = birth;
        this.birthPresent = true;
    }

    public void setSex(String sex) {
        rejectDuplicate(sexPresent);
        this.sex = sex;
        this.sexPresent = true;
    }

    public void setIsNeutered(Boolean isNeutered) {
        rejectDuplicate(isNeuteredPresent);
        this.isNeutered = isNeutered;
        this.isNeuteredPresent = true;
    }

    public void setWeight(BigDecimal weight) {
        rejectDuplicate(weightPresent);
        this.weight = weight;
        this.weightPresent = true;
    }

    public void setBasicVaccinationCompleted(Boolean basicVaccinationCompleted) {
        rejectDuplicate(basicVaccinationCompletedPresent);
        this.basicVaccinationCompleted = basicVaccinationCompleted;
        this.basicVaccinationCompletedPresent = true;
    }

    public String getName() {
        return name;
    }

    public boolean hasName() {
        return namePresent;
    }

    public String getSpeciesId() {
        return speciesId;
    }

    public boolean hasSpeciesId() {
        return speciesIdPresent;
    }

    public String getBreedId() {
        return breedId;
    }

    public boolean hasBreedId() {
        return breedIdPresent;
    }

    public String getProfileMediaId() {
        return profileMediaId;
    }

    public boolean hasProfileMediaId() {
        return profileMediaIdPresent;
    }

    public LocalDate getBirth() {
        return birth;
    }

    public boolean hasBirth() {
        return birthPresent;
    }

    public String getSex() {
        return sex;
    }

    public boolean hasSex() {
        return sexPresent;
    }

    public Boolean getIsNeutered() {
        return isNeutered;
    }

    public boolean hasIsNeutered() {
        return isNeuteredPresent;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public boolean hasWeight() {
        return weightPresent;
    }

    public Boolean getBasicVaccinationCompleted() {
        return basicVaccinationCompleted;
    }

    public boolean hasBasicVaccinationCompleted() {
        return basicVaccinationCompletedPresent;
    }

    private void rejectDuplicate(boolean present) {
        if (present) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
