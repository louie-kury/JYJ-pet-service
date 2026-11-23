package com.louiekury.pet.registration.domain;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Pet {

    private final String petId;
    private final String name;
    private final String speciesId;
    private final String breedId;
    private final String profileMediaId;
    private final LocalDate birth;
    private final String sex;
    private final Boolean isNeutered;
    private final BigDecimal weight;
    private final Boolean basicVaccinationCompleted;

    private Pet(String name, String speciesId, String breedId, String profileMediaId,
            LocalDate birth, String sex, Boolean isNeutered, BigDecimal weight,
            Boolean basicVaccinationCompleted) {
        this.petId = UUID.randomUUID().toString();
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

    public static Pet create(String name, String speciesId, String breedId,
            String profileMediaId, LocalDate birth, String sex, Boolean isNeutered,
            BigDecimal weight, Boolean basicVaccinationCompleted, LocalDate today) {
        String normalizedName = name == null ? null : name.strip();
        require(normalizedName != null && !normalizedName.isBlank()
            && normalizedName.codePointCount(0, normalizedName.length()) <= 50);
        require(speciesId != null && !speciesId.isBlank());
        require(breedId == null || !breedId.isBlank());
        require(profileMediaId == null || !profileMediaId.isBlank());
        require("MALE".equals(sex) || "FEMALE".equals(sex));
        require(birth == null || (birth.getYear() >= 1 && birth.getYear() <= 9999
            && !birth.isAfter(today)));
        require(weight == null || (weight.signum() > 0
            && weight.compareTo(new BigDecimal("200")) <= 0
            && weight.stripTrailingZeros().scale() <= 3));
        return new Pet(normalizedName, speciesId, breedId, profileMediaId, birth,
            sex, isNeutered, weight, basicVaccinationCompleted);
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
