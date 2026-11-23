package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.domain.PetBreed;
import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

/** 기존 종 문서의 품종 배열만 조회한다. 종 이름·종류의 기준은 enum이다. */
@Getter
@TypeAlias("com.louiekury.pet.registration.adapter.out.persistence.PetBreedCatalogMongoDocument")
@Document("pet_species")
public class PetBreedCatalogMongoDocument {

    @MongoId(FieldType.STRING)
    private String id;
    private List<Breed> breeds;

    public Optional<PetBreed> getPetBreed(PetSpecies species, String breedId) {
        if (!species.getSpeciesId().equals(id)) {
            throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE);
        }
        PetBreed found = null;
        Set<String> breedIds = new HashSet<>();
        if (breeds != null) {
            for (Breed breed : breeds) {
                if (breed == null || breed.breedId == null || breed.breedId.isBlank()
                        || breed.name == null || breed.name.isBlank() || breed.active == null
                        || !breedIds.add(breed.breedId)) {
                    throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE);
                }
                if (breedId.equals(breed.breedId) && breed.active) {
                    found = new PetBreed(species, breed.breedId, breed.name);
                }
            }
        }
        return Optional.ofNullable(found);
    }

    @Getter
    public static class Breed {
        private String breedId;
        private String name;
        private Boolean active;
    }
}
