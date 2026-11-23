package com.louiekury.pet.migration;

import java.util.List;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ResourceAccessor;
import org.bson.Document;

public class RegistrationKeyTypeValidation implements CustomTaskChange {
    @Override
    public void execute(Database database) throws CustomChangeException {
        Document type = new Document("$type", "$registration.requestKey");
        Document allowed = new Document("$in", List.of(type, List.of("string", "missing")));
        Document filter = new Document("$expr", new Document("$not", List.of(allowed)));
        if (((MongoLiquibaseDatabase) database).getMongoDatabase()
                .getCollection("pets").find(filter).first() != null) {
            throw new CustomChangeException("기존 등록 키의 BSON 타입을 확인하세요.");
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "기존 등록 키 필드 타입 검사 완료";
    }

    @Override
    public void setUp() {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
