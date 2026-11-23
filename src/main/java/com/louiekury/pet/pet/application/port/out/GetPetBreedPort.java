package com.louiekury.pet.pet.application.port.out;

import com.louiekury.pet.pet.domain.PetBreed;
import com.louiekury.pet.pet.domain.PetSpecies;
import java.util.Optional;

public interface GetPetBreedPort {

    Optional<PetBreed> getPetBreedBySpeciesAndId(PetSpecies species, String breedId);
}
