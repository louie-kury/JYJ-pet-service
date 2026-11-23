package com.louiekury.pet.global.config;

import com.louiekury.pet.global.persistence.MongoRegistrationSchema;
import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/** 서버가 요청을 받기 전에 완료 이력과 필수 DB 구조를 읽기만 하여 확인한다. */
@Component
@RequiredArgsConstructor
public class MongoMigrationReadiness implements SmartInitializingSingleton {

    private final MongoTemplate mongoTemplate;

    @Override
    public void afterSingletonsInstantiated() {
        try {
            verify(mongoTemplate.getDb());
        } catch (RuntimeException exception) {
            // 연결 URI·자격증명·서버 오류 원문을 기동 예외에 포함하지 않는다.
            throw new IllegalStateException("MongoDB 준비 확인에 실패했습니다. mongoMigrate 실행과 DB 설정을 확인하세요.");
        }
    }

    public static void verify(MongoDatabase database) {
        MongoRegistrationSchema.verifyCompletedHistory(database);
        MongoRegistrationSchema.verifyRegistrationSchema(database);
    }
}
