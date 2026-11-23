package com.louiekury.pet.registration.adapter.out.persistence;

import com.louiekury.pet.registration.application.port.out.LoadPetSpeciesPort;
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
public class PetSpeciesPersistenceAdapter implements LoadPetSpeciesPort {

    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<PetSpecies> findById(String speciesId) {
        try {
            return Optional.ofNullable(mongoTemplate.findById(speciesId, SpeciesMongoDocument.class))
                .map(SpeciesMongoDocument::toDomain);
        } catch (DataAccessException | IllegalStateException exception) {
            throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE, exception);
        }
    }
}
