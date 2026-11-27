package com.louiekury.pet.pet.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class PetSpecies {
    private final String speciesId;
    private final String name;
}
