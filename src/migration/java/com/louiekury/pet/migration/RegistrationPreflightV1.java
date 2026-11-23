package com.louiekury.pet.migration;

import com.louiekury.pet.global.persistence.MongoRegistrationSchema;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ResourceAccessor;

/** V1의 기존 데이터 검증. 변경이 필요하면 새 버전의 변경 클래스를 추가한다. */
public class RegistrationPreflightV1 implements CustomTaskChange {

    @Override
    public void execute(Database database) throws CustomChangeException {
        try {
            MongoRegistrationSchema.validateExistingSpecies(
                ((MongoLiquibaseDatabase) database).getMongoDatabase());
        } catch (RuntimeException exception) {
            throw new CustomChangeException("기존 종·품종 구조·중복·종 ID/code를 확인하세요.");
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "기존 등록 기준 데이터 검사 완료";
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
