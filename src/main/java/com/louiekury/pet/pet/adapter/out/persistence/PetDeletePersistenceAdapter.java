package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.port.out.DeletePetPort;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.mongodb.MongoException;
import java.time.Instant;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetDeletePersistenceAdapter implements DeletePetPort {
    private final MongoTemplate mongo;

    @Override
    public boolean delete(String id, String caregiverId, Instant deletedAt) {
        try {
            Criteria eligible = Criteria.where("_id").is(id).and("caregiverId").is(caregiverId)
                .and("deletedAt").is(null);
            return mongo.updateFirst(Query.query(eligible), new Update().set("deletedAt", Date.from(deletedAt)),
                "pets").getMatchedCount() == 1;
        } catch (DataAccessException | MongoException exception) {
            throw new BusinessException(ErrorCode.PERSISTENCE_UNAVAILABLE, exception);
        }
    }
}
