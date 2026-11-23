package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.createpet.PetResult;
import com.louiekury.pet.pet.application.port.out.CreatePetPort;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetPersistenceAdapter implements CreatePetPort {

    public static final String REQUEST_KEY_INDEX = "registration_request_key_unique";
    private final MongoTemplate mongoTemplate;

    @Override
    public void createPet(PetResult result, String idempotencyKey) {
        try {
            mongoTemplate.execute(PetMongoDocument.class, collection -> {
                if (idempotencyKey != null) {
                    verifyRequestKeyIndex(collection.listIndexes().into(new ArrayList<>()));
                }
                Document document = new Document();
                mongoTemplate.getConverter().write(PetMongoDocument.from(result, idempotencyKey), document);
                // 감사 시각과 완료 스냅샷을 같은 시각으로 준비해 한 번에 저장한다.
                // MongoTemplate.insert의 감사 콜백이 시각을 다시 생성하지 않도록 명시적 insert를 사용한다.
                try {
                    if (!collection.insertOne(document).wasAcknowledged()) {
                        throw new BusinessException(ErrorCode.PERSISTENCE_UNAVAILABLE);
                    }
                } catch (MongoWriteException exception) {
                    if (idempotencyKey != null && exception.getError().getCode() == 11000
                            && isRequestKeyViolation(exception)) {
                        throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
                    }
                    throw exception;
                }
                return null;
            });
        } catch (DataAccessException | MongoException exception) {
            throw new BusinessException(ErrorCode.PERSISTENCE_UNAVAILABLE, exception);
        }
    }

    private void verifyRequestKeyIndex(List<Document> indexes) {
        Document expectedKey = new Document("registration.requestKey", 1);
        Document expectedFilter = new Document("registration.requestKey", new Document("$type", "string"));
        boolean ready = indexes.stream().anyMatch(index -> REQUEST_KEY_INDEX.equals(index.getString("name"))
            && Boolean.TRUE.equals(index.getBoolean("unique"))
            && expectedKey.equals(index.get("key"))
            && expectedFilter.equals(index.get("partialFilterExpression"))
            && !Boolean.TRUE.equals(index.getBoolean("sparse")));
        if (!ready) {
            throw new BusinessException(ErrorCode.PERSISTENCE_UNAVAILABLE);
        }
    }

    private boolean isRequestKeyViolation(MongoWriteException exception) {
        org.bson.BsonDocument details = exception.getError().getDetails();
        if (details != null && details.containsKey("keyPattern")) {
            return details.getDocument("keyPattern").equals(
                org.bson.BsonDocument.parse("{\"registration.requestKey\":1}"));
        }
        return exception.getError().getMessage().contains("index: " + REQUEST_KEY_INDEX + " dup key");
    }
}
