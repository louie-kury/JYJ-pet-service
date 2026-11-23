package com.louiekury.pet.migration;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ResourceAccessor;
import org.bson.Document;

public class RegistrationKeyValidation implements CustomTaskChange {

    @Override
    public void execute(Database database) throws CustomChangeException {
        Document filter = new Document("registration.requestKey",
            new Document("$exists", true).append("$not", new Document("$type", "string")));
        if (((MongoLiquibaseDatabase) database).getMongoDatabase()
                .getCollection("pets").find(filter).first() != null) {
            throw new CustomChangeException("기존 등록 키의 BSON 타입을 확인하세요.");
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "기존 등록 키 타입 검사 완료";
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
