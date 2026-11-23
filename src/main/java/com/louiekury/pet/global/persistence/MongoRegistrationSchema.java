package com.louiekury.pet.global.persistence;

import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bson.Document;

/** 마이그레이션의 사전 검사와 앱의 읽기 전용 준비 검사가 공유하는 DB 계약이다. */
public final class MongoRegistrationSchema {

    public static final String HISTORY_FILE = "db/mongodb/changes/v001-registration.xml";
    public static final String AUTHOR = "louie-kury";
    public static final List<String> CHANGE_IDS = List.of(
        "001-preflight", "002-indexes", "003-seed", "004-verify");

    private MongoRegistrationSchema() {
    }

    public static void validateExistingSpecies(MongoDatabase database) {
        Set<String> codes = new HashSet<>();
        for (Document species : database.getCollection("pet_species").find()) {
            Object id = species.get("_id");
            Object code = species.get("code");
            require(id instanceof String && code instanceof String
                && species.get("name") instanceof String && species.get("active") instanceof Boolean
                && species.get("breeds") instanceof List<?> && codes.add((String) code));
            Set<String> breedIds = new HashSet<>();
            for (Object value : (List<?>) species.get("breeds")) {
                require(value instanceof Document);
                Document breed = (Document) value;
                Object breedId = breed.get("breedId");
                require(breedId instanceof String && !((String) breedId).isBlank()
                    && breed.get("name") instanceof String && breed.get("active") instanceof Boolean
                    && breedIds.add((String) breedId));
            }
            if ("dog".equals(id) || "DOG".equals(code)) {
                require("dog".equals(id) && "DOG".equals(code)
                    && Boolean.TRUE.equals(species.get("active")));
            }
            if ("cat".equals(id) || "CAT".equals(code)) {
                require("cat".equals(id) && "CAT".equals(code)
                    && Boolean.TRUE.equals(species.get("active")));
            }
        }
    }

    public static void verifyRegistrationSchema(MongoDatabase database) {
        validateExistingSpecies(database);
        verifyIndex(database, "pets", "registration_request_key_unique",
            new Document("registration.requestKey", 1),
            new Document("registration.requestKey", new Document("$type", "string")));
        verifyIndex(database, "pet_species", "species_code_unique", new Document("code", 1), null);
        for (String code : List.of("DOG", "CAT")) {
            List<Document> species = database.getCollection("pet_species")
                .find(new Document("code", code).append("active", true)).into(new ArrayList<>());
            require(species.size() == 1);
        }
    }

    /** 현재 앱은 종을 enum으로 관리한다. V1의 검증 의미는 위 메서드에 그대로 보존한다. */
    public static void verifyCurrentRegistrationSchema(MongoDatabase database) {
        verifyIndex(database, "pets", "registration_request_key_unique",
            new Document("registration.requestKey", 1),
            new Document("registration.requestKey", new Document("$type", "string")));
    }

    public static void verifyCompletedHistory(MongoDatabase database) {
        Document keyValidation = database.getCollection("DATABASECHANGELOG").find(
            new Document("id", "000-registration-key-validation")
                .append("author", AUTHOR)
                .append("fileName", "db/mongodb/changes/v000-registration-key-validation.xml")).first();
        require(keyValidation != null && "EXECUTED".equals(keyValidation.getString("execType")));
        for (String id : CHANGE_IDS) {
            List<Document> records = database.getCollection("DATABASECHANGELOG")
                .find(new Document("id", id).append("author", AUTHOR).append("fileName", HISTORY_FILE))
                .into(new ArrayList<>());
            require(records.size() == 1);
            Document record = records.get(0);
            require("EXECUTED".equals(record.getString("execType"))
                && record.get("md5sum") instanceof String
                && record.getString("md5sum").matches("[0-9]+:[a-f0-9]{32}"));
        }
        require(database.getCollection("DATABASECHANGELOGLOCK")
            .countDocuments(new Document("locked", true)) == 0);
    }

    private static void verifyIndex(MongoDatabase database, String collection, String name,
            Document key, Document partialFilter) {
        boolean found = false;
        for (Document index : database.getCollection(collection).listIndexes()) {
            if (name.equals(index.getString("name"))) {
                found = Boolean.TRUE.equals(index.getBoolean("unique")) && key.equals(index.get("key"))
                    && !Boolean.TRUE.equals(index.getBoolean("sparse"))
                    && java.util.Objects.equals(partialFilter, index.get("partialFilterExpression"))
                    && index.get("collation") == null;
            }
        }
        require(found);
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new IllegalStateException("MongoDB 등록 스키마 또는 마이그레이션 이력이 준비되지 않았습니다.");
        }
    }
}
