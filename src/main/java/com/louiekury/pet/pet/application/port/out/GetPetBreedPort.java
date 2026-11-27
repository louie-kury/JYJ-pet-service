package com.louiekury.pet.pet.application.port.out;

import com.louiekury.pet.pet.domain.PetBreed;
import com.louiekury.pet.pet.domain.PetSpecies;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface GetPetBreedPort {
    Optional<PetBreed> getPetBreedBySpeciesAndId(PetSpecies species, String breedId);

    default Map<String, PetBreed> getPetBreedsByIds(Map<String, PetSpecies> species, Set<String> ids) {
        Map<String, PetBreed> result = new LinkedHashMap<>();
        for (PetSpecies item : species.values()) {
            for (String id : ids) {
                getPetBreedBySpeciesAndId(item, id).ifPresent(breed -> result.put(id, breed));
            }
        }
        return result;
    }
}
