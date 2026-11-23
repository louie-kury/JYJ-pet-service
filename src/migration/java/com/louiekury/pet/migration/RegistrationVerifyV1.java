package com.louiekury.pet.migration;

import com.louiekury.pet.global.persistence.MongoRegistrationSchema;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ResourceAccessor;

/** 실행 이력 편입 전에 실제 최종 구조를 검사한다. baseline 강제 등록은 하지 않는다. */
public class RegistrationVerifyV1 implements CustomTaskChange {

    @Override
    public void execute(Database database) throws CustomChangeException {
        try {
            MongoRegistrationSchema.verifyRegistrationSchema(
                ((MongoLiquibaseDatabase) database).getMongoDatabase());
        } catch (RuntimeException exception) {
            throw new CustomChangeException("등록 인덱스와 활성 종 기준 데이터를 확인하세요.");
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "등록 DB 구조 확인 완료";
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
