package com.louiekury.pet.pet.domain;

import lombok.Getter;

@Getter
public class PetBreed {

    private final PetSpecies species;
    private final String breedId;
    private final String name;

    public PetBreed(PetSpecies species, String breedId, String name) {
        this.species = species;
        this.breedId = breedId;
        this.name = name;
    }
}
