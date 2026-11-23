package com.louiekury.pet.registration.application.port.out;

import com.louiekury.pet.registration.domain.PetSpecies;
import java.util.Optional;

public interface LoadPetSpeciesPort {

    Optional<PetSpecies> findById(String speciesId);
}
