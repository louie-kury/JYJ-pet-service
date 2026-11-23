package com.louiekury.pet.registration.application.port.out;

import com.louiekury.pet.registration.domain.PetBreed;
import com.louiekury.pet.registration.domain.PetSpecies;
import java.util.Optional;

public interface LoadPetBreedPort {

    Optional<PetBreed> findBySpeciesAndId(PetSpecies species, String breedId);
}
