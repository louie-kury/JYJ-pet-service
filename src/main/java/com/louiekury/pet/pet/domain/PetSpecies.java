package com.louiekury.pet.pet.domain;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PetSpecies {

    DOG("dog", "강아지"),
    CAT("cat", "고양이");

    private final String speciesId;
    private final String name;

    public static Optional<PetSpecies> fromId(String speciesId) {
        return Arrays.stream(values()).filter(species -> species.speciesId.equals(speciesId)).findFirst();
    }
}
