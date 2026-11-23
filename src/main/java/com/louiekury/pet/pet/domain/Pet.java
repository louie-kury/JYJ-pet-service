package com.louiekury.pet.pet.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Pet {

    private final String petId;
    private final String name;
    private final PetSpecies species;
    private final String breedId;
    private final String profileMediaId;
    private final LocalDate birth;
    private final String sex;
    private final Boolean isNeutered;
    private final BigDecimal weight;
    private final Boolean basicVaccinationCompleted;

    private Pet(String name, PetSpecies species, String breedId, String profileMediaId,
            LocalDate birth, String sex, Boolean isNeutered, BigDecimal weight,
            Boolean basicVaccinationCompleted) {
        this.petId = UUID.randomUUID().toString();
        this.name = name;
        this.species = species;
        this.breedId = breedId;
        this.profileMediaId = profileMediaId;
        this.birth = birth;
        this.sex = sex;
        this.isNeutered = isNeutered;
        this.weight = weight;
        this.basicVaccinationCompleted = basicVaccinationCompleted;
    }

    public static Pet create(String name, PetSpecies species, String breedId,
            String profileMediaId, LocalDate birth, String sex, Boolean isNeutered,
            BigDecimal weight, Boolean basicVaccinationCompleted) {
        return new Pet(name, species, breedId, profileMediaId, birth, sex, isNeutered,
            weight, basicVaccinationCompleted);
    }

    public String getSpeciesId() {
        return species.getSpeciesId();
    }

}
