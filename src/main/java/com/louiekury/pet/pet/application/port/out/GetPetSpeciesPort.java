package com.louiekury.pet.pet.application.port.out;

import com.louiekury.pet.pet.domain.PetSpecies;
import java.util.Optional;

public interface GetPetSpeciesPort {
    Optional<PetSpecies> getPetSpecies(String speciesId);
}
