package com.louiekury.pet.pet.application.port.out;

import com.louiekury.pet.pet.domain.PetSpecies;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface GetPetSpeciesPort {
    Optional<PetSpecies> getPetSpecies(String speciesId);

    default Map<String, PetSpecies> getPetSpeciesByIds(Set<String> ids) {
        Map<String, PetSpecies> result = new LinkedHashMap<>();
        for (String id : ids) {
            getPetSpecies(id).ifPresent(species -> result.put(id, species));
        }
        return result;
    }
}
