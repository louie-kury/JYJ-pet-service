package com.louiekury.pet.registration.domain;

import java.util.Objects;
import lombok.Getter;

@Getter
public class PetBreed {

    private final PetSpecies species;
    private final String breedId;
    private final String name;

    public PetBreed(PetSpecies species, String breedId, String name) {
        this.species = Objects.requireNonNull(species, "species");
        if (breedId == null || breedId.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("품종 ID와 이름이 필요합니다.");
        }
        this.breedId = breedId;
        this.name = name;
    }
}
