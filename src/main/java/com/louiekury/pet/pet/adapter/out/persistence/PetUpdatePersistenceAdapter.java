package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.port.out.UpdatePetPort;
import com.louiekury.pet.pet.application.port.out.GetPetSpeciesPort;
import com.louiekury.pet.pet.application.port.out.GetPetBreedPort;
import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.pet.domain.PetBreed;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.mongodb.MongoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetUpdatePersistenceAdapter implements UpdatePetPort {
    private final MongoTemplate mongo;
    private final GetPetSpeciesPort speciesPort;
    private final GetPetBreedPort breedPort;

    @Override
    public Optional<Data> find(String id, Map<String, Object> changes, String caregiverId) {
        try {
            Document doc = mongo.findOne(Query.query(eligible(id, caregiverId)), Document.class, "pets");
            if (doc == null) {
                return Optional.empty();
            }
            String species = doc.getString("speciesId");
            String breed = doc.getString("breedId");
            boolean resolveBreedName = Objects.equals(species, changes.getOrDefault("speciesId", species))
                && Objects.equals(breed, changes.getOrDefault("breedId", breed));
            String breedName = resolveBreedName ? breedName(doc, species, breed) : null;
            Object weight = doc.get("weight");
            BigDecimal decimal = weight == null ? null : ((Decimal128) weight).bigDecimalValue();
            String birth = doc.getString("birth");
            return Optional.of(new Data(id, doc.getString("name"), species, breed, breedName,
                doc.getString("profileMediaId"), birth == null ? null : LocalDate.parse(birth),
                doc.getString("sex"), doc.getBoolean("isNeutered"), decimal,
                doc.getBoolean("basicVaccinationCompleted"), instant(doc.get("createdAt")),
                instant(doc.get("updatedAt"))));
        } catch (DataAccessException | MongoException | ClassCastException | java.time.DateTimeException e) {
            throw new BusinessException(ErrorCode.PERSISTENCE_UNAVAILABLE, e);
        }
    }

    @Override
    public boolean update(Data previous, Map<String, Object> changes, Instant now, String caregiverId) {
        try {
            Criteria criteria = eligible(previous.getId(), caregiverId).and("updatedAt").is(Date.from(previous.getUpdatedAt()));
            Update update = new Update();
            changes.forEach((key, value) -> update.set(key, value instanceof LocalDate date ? date.toString()
                : value instanceof BigDecimal decimal ? new Decimal128(decimal) : value));
            update.set("updatedAt", Date.from(now));
            return mongo.updateFirst(Query.query(criteria), update, "pets").getMatchedCount() == 1;
        } catch (DataAccessException | MongoException e) {
            throw new BusinessException(ErrorCode.PERSISTENCE_UNAVAILABLE, e);
        }
    }

    private Criteria eligible(String id, String caregiverId) {
        return Criteria.where("_id").is(id).and("caregiverId").is(caregiverId).and("deletedAt").is(null);
    }

    private Instant instant(Object value) {
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new BusinessException(ErrorCode.PERSISTENCE_UNAVAILABLE);
    }

    private String breedName(Document pet, String species, String breed) {
        if (breed == null) {
            return null;
        }
        PetSpecies speciesData = speciesPort.getPetSpecies(species)
            .orElseThrow(() -> new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE));
        String found = breedPort.getPetBreedBySpeciesAndId(speciesData, breed).map(PetBreed::getName).orElse(null);
        if (found != null) {
            return found;
        }
        if (pet.get("registration") instanceof Map<?, ?> registration
                && registration.get("responseSnapshot") instanceof Map<?, ?> snapshot
                && snapshot.get("species") instanceof Map<?, ?> savedSpecies
                && species.equals(savedSpecies.get("speciesId"))
                && snapshot.get("breed") instanceof Map<?, ?> saved
                && breed.equals(saved.get("breedId")) && saved.get("name") instanceof String name
                && !name.isBlank()) return name;
        return null;
    }
}
