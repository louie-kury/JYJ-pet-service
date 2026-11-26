package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.deletepet.DeletePetService;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "PET_TEST_MONGODB_URI", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PetDeleteMongoIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-11-26T00:00:00Z");
    private MongoClient client;
    private MongoTemplate mongo;
    private DeletePetService service;

    @BeforeAll void connect() {
        client = MongoClients.create(System.getenv("PET_TEST_MONGODB_URI"));
        mongo = new MongoTemplate(client, "pet_delete_test_" + UUID.randomUUID().toString().replace("-", ""));
        service = new DeletePetService(new PetDeletePersistenceAdapter(mongo), Clock.fixed(NOW, ZoneOffset.UTC));
    }
    @BeforeEach void seed() {
        mongo.getCollection("pets").deleteMany(new Document());
        mongo.getCollection("pets").insertOne(new Document("_id", "mine").append("caregiverId", "a").append("name", "루이"));
        mongo.getCollection("pets").insertOne(new Document("_id", "theirs").append("caregiverId", "b"));
        mongo.getCollection("pets").insertOne(new Document("_id", "unowned").append("caregiverId", null));
    }
    @AfterAll void close() { if (mongo != null) mongo.getDb().drop(); if (client != null) client.close(); }

    @Test void deletesOnlyOwnedPetAndPreservesOtherFields() {
        service.delete("mine", "a");
        Document pet = mongo.getCollection("pets").find(new Document("_id", "mine")).first();
        assertThat(pet.getDate("deletedAt")).isEqualTo(Date.from(NOW));
        assertThat(pet.getString("name")).isEqualTo("루이");
        assertThat(mongo.getCollection("pets").find(new Document("_id", "theirs")).first().get("deletedAt")).isNull();
        assertNotFound("mine");
    }
    @Test void rejectsOtherMissingAndUnownedPets() {
        assertNotFound("theirs");
        assertNotFound("missing");
        assertNotFound("unowned");
        assertThat(mongo.getCollection("pets").find(new Document("deletedAt", new Document("$ne", null))).first()).isNull();
    }
    @Test void rejectsMissingOwner() {
        assertThatThrownBy(() -> service.delete("mine", null)).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }
    @Test void concurrentDeletesHaveOnlyOneSuccess() throws Exception {
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> { start.await(); return new PetDeletePersistenceAdapter(mongo).delete("mine", "a", NOW); });
            var b = executor.submit(() -> { start.await(); return new PetDeletePersistenceAdapter(mongo).delete("mine", "a", NOW); });
            start.countDown();
            assertThat(a.get() ^ b.get()).isTrue();
        } finally { executor.shutdownNow(); }
    }
    private void assertNotFound(String id) {
        assertThatThrownBy(() -> service.delete(id, "a")).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
