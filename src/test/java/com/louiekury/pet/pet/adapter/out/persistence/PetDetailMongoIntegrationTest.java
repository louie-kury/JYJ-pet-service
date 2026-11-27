package com.louiekury.pet.pet.adapter.out.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "PET_TEST_MONGODB_URI", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PetDetailMongoIntegrationTest {
    private MongoClient client;
    private MongoTemplate mongo;
    private PetDetailPersistenceAdapter adapter;
    @BeforeAll void setup() {
        client = MongoClients.create(System.getenv("PET_TEST_MONGODB_URI"));
        mongo = new MongoTemplate(client, "pet_detail_test_" + UUID.randomUUID().toString().replace("-", ""));
        adapter = new PetDetailPersistenceAdapter(mongo, new PetListPersistenceAdapter(mongo, com.louiekury.pet.pet.support.CatalogFixtures.species(), com.louiekury.pet.pet.support.CatalogFixtures.noBreeds()));
        Date timestamp = new Date();
        mongo.getCollection("pets").insertOne(new Document("_id", "pet-1").append("caregiverId", "owner-1")
            .append("name", "루이").append("speciesId", "dog").append("sex", "MALE")
            .append("createdAt", timestamp).append("updatedAt", timestamp));
        mongo.getCollection("pets").insertOne(new Document("_id", "deleted").append("caregiverId", "owner-1").append("deletedAt", timestamp));
        mongo.getCollection("pets").insertOne(new Document("_id", "unowned"));
    }
    @AfterAll void close() { if (mongo != null) mongo.getDb().drop(); if (client != null) client.close(); }
    @Test void returnsOnlyMatchingActiveOwner() {
        assertThat(adapter.getPet("pet-1", "owner-1")).get().satisfies(data -> assertThat(data.summary().getName()).isEqualTo("루이"));
        assertThat(adapter.getPet("pet-1", "owner-2")).isEmpty();
        assertThat(adapter.getPet("deleted", "owner-1")).isEmpty();
        assertThat(adapter.getPet("unowned", "owner-1")).isEmpty();
        assertThat(adapter.getPet("missing", "owner-1")).isEmpty();
    }
    @Test void authenticatedRegistrationStoresOwner() {
        var species = new com.louiekury.pet.pet.domain.PetSpecies("dog", "강아지");
        var pet = com.louiekury.pet.pet.domain.Pet.create("보리", species, null, null, null, "FEMALE", null, null, null);
        var result = com.louiekury.pet.pet.application.createpet.PetResult.of(pet, null, java.time.Instant.now(), null);
        new PetPersistenceAdapter(mongo).createPet(result, null, "owner-1");
        assertThat(mongo.getCollection("pets").find(new Document("_id", result.getPetId())).first().getString("caregiverId"))
            .isEqualTo("owner-1");
        assertThat(adapter.getPet(result.getPetId(), "owner-1")).isPresent();
        assertThat(adapter.getPet(result.getPetId(), "owner-2")).isEmpty();
    }
}
