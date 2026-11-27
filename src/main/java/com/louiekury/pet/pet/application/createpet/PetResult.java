package com.louiekury.pet.pet.application.createpet;

import com.louiekury.pet.pet.domain.Pet;
import com.louiekury.pet.pet.domain.PetBreed;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class PetResult {

    private final String petId;
    private final String name;
    private final Species species;
    private final Breed breed;
    private final LocalDate birth;
    private final Age age;
    private final String sex;
    private final Boolean isNeutered;
    private final BigDecimal weight;
    private final Boolean basicVaccinationCompleted;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static PetResult of(Pet pet, PetBreed breed, Instant createdAt, Period age) {
        return PetResult.builder()
            .petId(pet.getPetId())
            .name(pet.getName())
            .species(new Species(pet.getSpecies().getSpeciesId(), pet.getSpecies().getName()))
            .breed(breed == null ? null : new Breed(breed.getBreedId(), breed.getName()))
            .birth(pet.getBirth())
            .age(age == null ? null : new Age(age.getYears(), age.getMonths()))
            .sex(pet.getSex())
            .isNeutered(pet.getIsNeutered())
            .weight(pet.getWeight())
            .basicVaccinationCompleted(pet.getBasicVaccinationCompleted())
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .build();
    }

    public static PetResult updated(String id, String name, com.louiekury.pet.pet.domain.PetSpecies species,
            String breedId, String breedName, LocalDate birth, Period age, String sex,
            Boolean neutered, BigDecimal weight, Boolean vaccination, Instant createdAt, Instant updatedAt) {
        return PetResult.builder()
            .petId(id)
            .name(name)
            .species(new Species(species.getSpeciesId(), species.getName()))
            .breed(breedId == null ? null : new Breed(breedId, breedName))
            .birth(birth)
            .age(age == null ? null : new Age(age.getYears(), age.getMonths()))
            .sex(sex)
            .isNeutered(neutered)
            .weight(weight)
            .basicVaccinationCompleted(vaccination)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Species {
        private final String speciesId;
        private final String name;
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Breed {
        private final String breedId;
        private final String name;
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Age {
        private final int years;
        private final int months;
    }
}
