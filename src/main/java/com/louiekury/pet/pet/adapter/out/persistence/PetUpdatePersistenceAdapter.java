package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.updatepet.UpdatePetPort;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.mongodb.MongoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;
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

    @Override
    public Optional<Data> find(String id, boolean resolveBreedName) {
        try {
            Document doc = mongo.findOne(Query.query(eligible(id)), Document.class, "pets");
            if (doc == null) return Optional.empty();
            String species = doc.getString("speciesId");
            String breed = doc.getString("breedId");
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
    public boolean update(Data previous, Map<String, Object> changes, Instant now) {
        try {
            Criteria criteria = eligible(previous.id()).and("updatedAt").is(Date.from(previous.updatedAt()));
            Update update = new Update();
            changes.forEach((key, value) -> update.set(key, value instanceof LocalDate date ? date.toString()
                : value instanceof BigDecimal decimal ? new Decimal128(decimal) : value));
            update.set("updatedAt", Date.from(now));
            return mongo.updateFirst(Query.query(criteria), update, "pets").getMatchedCount() == 1;
        } catch (DataAccessException | MongoException e) {
            throw new BusinessException(ErrorCode.PERSISTENCE_UNAVAILABLE, e);
        }
    }

    private Criteria eligible(String id) {
        return Criteria.where("_id").is(id).and("caregiverId").is(null).and("deletedAt").is(null);
    }

    private Instant instant(Object value) {
        if (value instanceof Date date) return date.toInstant();
        if (value instanceof Instant instant) return instant;
        throw new BusinessException(ErrorCode.PERSISTENCE_UNAVAILABLE);
    }

    private String breedName(Document pet, String species, String breed) {
        if (breed == null) return null;
        Document catalog = mongo.findById(species, Document.class, "pet_species");
        String found = null;
        if (catalog != null && catalog.get("breeds") instanceof Iterable<?> breeds) {
            for (Object value : breeds) {
                if (value instanceof Map<?, ?> entry && breed.equals(entry.get("breedId"))) {
                    if (found != null || !(entry.get("name") instanceof String name) || name.isBlank()) {
                        throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE);
                    }
                    found = name;
                }
            }
        }
        if (found != null) return found;
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
