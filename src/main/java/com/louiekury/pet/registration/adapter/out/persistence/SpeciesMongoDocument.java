package com.louiekury.pet.registration.adapter.out.persistence;

import com.louiekury.pet.registration.domain.PetSpecies;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Getter
@Document("pet_species")
public class SpeciesMongoDocument {

    @MongoId(FieldType.STRING)
    private String id;
    private String code;
    private String name;
    private boolean active;
    private List<Breed> breeds;

    public PetSpecies toDomain() {
        Map<String, String> activeBreeds = new HashMap<>();
        if (breeds != null) {
            for (Breed breed : breeds) {
                if (breed.active && (breed.breedId == null || breed.name == null
                        || activeBreeds.putIfAbsent(breed.breedId, breed.name) != null)) {
                    throw new IllegalStateException("품종 기준 데이터가 올바르지 않습니다.");
                }
            }
        }
        if (id == null || code == null || name == null) {
            throw new IllegalStateException("종 기준 데이터가 올바르지 않습니다.");
        }
        return new PetSpecies(id, name, code, active, activeBreeds);
    }

    @Getter
    public static class Breed {
        private String breedId;
        private String name;
        private boolean active;
    }
}
