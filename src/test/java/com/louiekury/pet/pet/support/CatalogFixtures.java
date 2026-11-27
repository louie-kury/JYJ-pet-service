package com.louiekury.pet.pet.support;

import com.louiekury.pet.pet.application.port.out.GetPetBreedPort;
import com.louiekury.pet.pet.application.port.out.GetPetSpeciesPort;
import com.louiekury.pet.pet.domain.PetSpecies;
import java.util.Optional;

public final class CatalogFixtures {
    private CatalogFixtures() { }

    public static GetPetSpeciesPort species() {
        return id -> {
            if ("dog".equals(id)) return Optional.of(new PetSpecies("dog", "강아지"));
            if ("cat".equals(id)) return Optional.of(new PetSpecies("cat", "고양이"));
            return Optional.empty();
        };
    }

    public static GetPetBreedPort noBreeds() {
        return (species, breedId) -> Optional.empty();
    }
}
