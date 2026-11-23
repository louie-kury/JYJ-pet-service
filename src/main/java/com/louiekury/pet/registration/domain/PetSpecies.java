package com.louiekury.pet.registration.domain;

import java.util.Map;
import java.util.Optional;
import lombok.Getter;

@Getter
public class PetSpecies {

    private final String speciesId;
    private final String name;
    private final String code;
    private final boolean active;
    private final Map<String, String> activeBreeds;

    public PetSpecies(String speciesId, String name, String code, boolean active,
            Map<String, String> activeBreeds) {
        this.speciesId = speciesId;
        this.name = name;
        this.code = code;
        this.active = active;
        this.activeBreeds = Map.copyOf(activeBreeds);
    }

    public boolean isSupported() {
        return active && ("DOG".equals(code) || "CAT".equals(code));
    }

    public Optional<String> findBreedName(String breedId) {
        return Optional.ofNullable(activeBreeds.get(breedId));
    }
}
