package com.louiekury.pet.registration.adapter.out.persistence;

import com.louiekury.pet.registration.application.port.out.LoadPetBreedPort;
import com.louiekury.pet.registration.domain.PetBreed;
import com.louiekury.pet.registration.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetBreedPersistenceAdapter implements LoadPetBreedPort {

    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<PetBreed> findBySpeciesAndId(PetSpecies species, String breedId) {
        try {
            PetBreedCatalogMongoDocument catalog = mongoTemplate.findById(
                species.getSpeciesId(), PetBreedCatalogMongoDocument.class);
            return catalog == null ? Optional.empty() : catalog.findBreed(species, breedId);
        } catch (DataAccessException | IllegalStateException exception) {
            throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE, exception);
        }
    }
}
