package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.getpet.GetPetPort;
import com.louiekury.pet.pet.application.getpets.PetListData;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.mongodb.MongoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetDetailPersistenceAdapter implements GetPetPort {
    private final MongoTemplate mongo;
    private final PetListPersistenceAdapter listAdapter;

    @Override
    public Optional<Data> getPet(String id, String caregiverId) {
        if (caregiverId == null || caregiverId.isBlank()) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        try {
            Criteria criteria = Criteria.where("_id").is(id).and("caregiverId").is(caregiverId)
                .and("deletedAt").is(null);
            Document doc = mongo.findOne(Query.query(criteria), Document.class, "pets");
            if (doc == null) return Optional.empty();
            PetListData summary = listAdapter.getPetData(doc, listAdapter.getBreedCatalogs(List.of(doc)));
            Object value = doc.get("weight");
            BigDecimal weight = value == null ? null : ((Decimal128) value).bigDecimalValue();
            return Optional.of(new Data(summary, doc.getBoolean("isNeutered"), weight,
                doc.getBoolean("basicVaccinationCompleted"), instant(doc.get("createdAt")),
                instant(doc.get("updatedAt"))));
        } catch (DataAccessException | MongoException | ClassCastException exception) {
            throw new BusinessException(ErrorCode.PET_QUERY_UNAVAILABLE, exception);
        }
    }

    private static Instant instant(Object value) {
        if (value instanceof Date date) return date.toInstant();
        if (value instanceof Instant instant) return instant;
        throw new BusinessException(ErrorCode.PET_QUERY_UNAVAILABLE);
    }
}
