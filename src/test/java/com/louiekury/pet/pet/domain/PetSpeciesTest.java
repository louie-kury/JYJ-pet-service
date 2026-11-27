package com.louiekury.pet.pet.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PetSpeciesTest {
    @Test
    void acceptsSpeciesBeyondDogAndCat() {
        PetSpecies species = new PetSpecies("rabbit", "토끼");
        assertThat(species.getSpeciesId()).isEqualTo("rabbit");
        assertThat(species.getName()).isEqualTo("토끼");
    }
}
