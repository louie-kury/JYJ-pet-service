package com.louiekury.pet.pet.application.getpets;

import java.time.Period;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class PetListItemResult {

    private final String petId;
    private final String name;
    private final Species species;
    private final Breed breed;
    private final Age age;
    private final String sex;

    public static PetListItemResult of(PetListData pet, Period age) {
        return PetListItemResult.builder().petId(pet.getPetId()).name(pet.getName())
            .species(new Species(pet.getSpecies().getSpeciesId(), pet.getSpecies().getName()))
            .breed(pet.getBreedId() == null ? null : new Breed(pet.getBreedId(), pet.getBreedName()))
            .age(age == null ? null : new Age(age.getYears(), age.getMonths()))
            .sex(pet.getSex()).build();
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
