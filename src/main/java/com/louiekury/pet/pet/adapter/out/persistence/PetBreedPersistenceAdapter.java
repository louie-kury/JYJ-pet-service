package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.port.out.GetPetBreedPort;
import com.louiekury.pet.pet.domain.PetBreed;
import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetBreedPersistenceAdapter implements GetPetBreedPort {

    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<PetBreed> getPetBreedBySpeciesAndId(PetSpecies species, String breedId) {
        try {
            PetBreedCatalogMongoDocument catalog = mongoTemplate.findById(
                species.getSpeciesId(), PetBreedCatalogMongoDocument.class);
            return catalog == null ? Optional.empty() : catalog.getPetBreed(species, breedId);
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE, exception);
        }
    }
}
