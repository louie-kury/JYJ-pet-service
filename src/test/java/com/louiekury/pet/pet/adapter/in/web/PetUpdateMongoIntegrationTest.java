package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.global.web.exception.GlobalExceptionHandler;
import com.louiekury.pet.pet.adapter.out.persistence.PetBreedPersistenceAdapter;
import com.louiekury.pet.pet.adapter.out.persistence.PetListPersistenceAdapter;
import com.louiekury.pet.pet.adapter.out.persistence.PetUpdatePersistenceAdapter;
import com.louiekury.pet.pet.application.getpets.GetPetsQuery;
import com.louiekury.pet.pet.application.port.in.CreatePetUseCase;
import com.louiekury.pet.pet.application.port.in.GetPetsUseCase;
import com.louiekury.pet.pet.application.updatepet.UpdatePetService;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "PET_TEST_MONGODB_URI", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PetUpdateMongoIntegrationTest {
    private static final Instant CREATED = Instant.parse("2026-11-20T00:00:00Z");
    private MongoClient client;
    private MongoTemplate mongo;
    private MockMvc mvc;

    @BeforeAll
    void connect() {
        client = MongoClients.create(System.getenv("PET_TEST_MONGODB_URI"));
        mongo = new MongoTemplate(client, "pet_update_test_" + UUID.randomUUID().toString().replace("-", ""));
        var service = new UpdatePetService(new PetUpdatePersistenceAdapter(mongo), new PetBreedPersistenceAdapter(mongo),
            Clock.fixed(Instant.parse("2026-11-25T00:00:00Z"), ZoneOffset.UTC));
        mvc = MockMvcBuilders.standaloneSetup(new PetUpdateController(new PetWebAdapter(
            mock(CreatePetUseCase.class), mock(GetPetsUseCase.class), service, mock(com.louiekury.pet.pet.application.port.in.GetPetUseCase.class))))
            .setControllerAdvice(new GlobalExceptionHandler())
            .defaultRequest(patch("/").requestAttr(PetRegistrationAuthInterceptor.CAREGIVER_ID, "owner-a"))
            .build();
    }

    @BeforeEach
    void seed() {
        mongo.getCollection("pets").deleteMany(new Document());
        mongo.getCollection("pet_species").deleteMany(new Document());
        mongo.getCollection("pets").insertOne(pet("pet-1"));
    }

    @AfterAll
    void close() {
        if (mongo != null) mongo.getDb().drop();
        if (client != null) client.close();
    }

    @Test
    void changesOnlyRequestedFieldsAndKeepsRegistrationSnapshot() throws Exception {
        Document before = stored("pet-1");
        mvc.perform(patch("/api/pets/pet-1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  새 이름  \",\"weight\":null}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("새 이름"))
            .andExpect(jsonPath("$.data.sex").value("MALE"));
        Document after = stored("pet-1");
        assertThat(after.getString("name")).isEqualTo("새 이름");
        assertThat(after.get("weight")).isNull();
        assertThat(after.getString("birth")).isEqualTo("2024-02-29");
        assertThat(after.get("registration")).isEqualTo(before.get("registration"));
        assertThat(after.getDate("updatedAt")).isAfter(before.getDate("updatedAt"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"\"}", "{\"weight\":0}",
        "{\"weight\":200.001}", "{\"weight\":1.0001}", "{\"sex\":\"OTHER\"}",
        "{\"birth\":\"2026-11-26\"}", "{\"name\":3}", "{\"weight\":\"2\"}",
        "{\"isNeutered\":\"false\"}", "{\"caregiverId\":\"victim\"}", "{\"unknown\":1}"})
    void invalidUpdatesNeverMutateDocument(String json) throws Exception {
        Document before = stored("pet-1");
        mvc.perform(patch("/api/pets/pet-1").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());
        assertThat(stored("pet-1")).isEqualTo(before);
    }

    @Test
    void unverifiedMediaLeavesDocumentUnchanged() throws Exception {
        Document before = stored("pet-1");
        mvc.perform(patch("/api/pets/pet-1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"profileMediaId\":\"media-1\"}"))
            .andExpect(status().isServiceUnavailable());
        assertThat(stored("pet-1")).isEqualTo(before);
    }

    @Test
    void ownedDeletedAndMissingPetsAreNotModified() throws Exception {
        mongo.getCollection("pets").insertOne(pet("owned").append("caregiverId", "owner"));
        mongo.getCollection("pets").insertOne(pet("deleted").append("deletedAt", Date.from(CREATED)));
        mongo.getCollection("pets").insertOne(pet("unowned").append("caregiverId", null));
        for (String id : new String[]{"owned", "deleted", "unowned", "missing"}) {
            mvc.perform(patch("/api/pets/" + id).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"변경\"}"))
                .andExpect(status().isNotFound());
        }
        assertThat(stored("owned").getString("name")).isEqualTo("루이");
        assertThat(stored("deleted").getString("name")).isEqualTo("루이");
        assertThat(stored("pet-1").getString("name")).isEqualTo("루이");
    }

    @Test
    void speciesAndExistingBreedMustRemainCompatible() throws Exception {
        mongo.getCollection("pets").updateOne(new Document("_id", "pet-1"),
            new Document("$set", new Document("breedId", "breed-dog")));
        mongo.getCollection("pet_species").insertOne(new Document("_id", "dog")
            .append("breeds", java.util.List.of(new Document("breedId", "breed-dog").append("name", "품종").append("active", true))));
        Document before = stored("pet-1");
        mvc.perform(patch("/api/pets/pet-1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"speciesId\":\"cat\"}"))
            .andExpect(status().isBadRequest());
        assertThat(stored("pet-1")).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"duplicate", "blank"})
    void malformedBreedCatalogRejectsUpdateWithoutMutation(String defect) throws Exception {
        mongo.getCollection("pets").updateOne(new Document("_id", "pet-1"),
            new Document("$set", new Document("breedId", "breed-dog")));
        var breeds = new java.util.ArrayList<Document>();
        breeds.add(new Document("breedId", "breed-dog").append("name", defect.equals("blank") ? " " : "품종"));
        if (defect.equals("duplicate")) {
            breeds.add(new Document("breedId", "breed-dog").append("name", "다른 이름"));
        }
        mongo.getCollection("pet_species").insertOne(new Document("_id", "dog").append("breeds", breeds));
        Document before = stored("pet-1");

        mvc.perform(patch("/api/pets/pet-1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"변경\"}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("REFERENCE_DATA_UNAVAILABLE"));
        assertThat(stored("pet-1")).isEqualTo(before);
    }

    @Test
    void clearingBreedSucceedsEvenWhenOldCatalogIsMalformed() throws Exception {
        mongo.getCollection("pets").updateOne(new Document("_id", "pet-1"),
            new Document("$set", new Document("breedId", "breed-dog")));
        mongo.getCollection("pet_species").insertOne(new Document("_id", "dog").append("breeds", java.util.List.of(
            new Document("breedId", "breed-dog").append("name", "첫 품종"),
            new Document("breedId", "breed-dog").append("name", "중복 품종"))));

        mvc.perform(patch("/api/pets/pet-1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"breedId\":null}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.breed").doesNotExist());
        assertThat(stored("pet-1").get("breedId")).isNull();
    }

    @Test
    void listUsesSameSpeciesSnapshotWhenBreedIsMissingFromCatalog() {
        Document snapshot = new Document("species", new Document("speciesId", "dog").append("name", "강아지"))
            .append("breed", new Document("breedId", "old-breed").append("name", "이전 품종"));
        mongo.getCollection("pets").updateOne(new Document("_id", "pet-1"),
            new Document("$set", new Document("breedId", "old-breed")
                .append("registration.responseSnapshot", snapshot)));

        mongo.getCollection("pets").updateOne(new Document("_id", "pet-1"), new Document("$set", new Document("caregiverId", "owner-a")));
        var page = new PetListPersistenceAdapter(mongo).getPets(GetPetsQuery.of(0, 4, "owner-a"));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getBreedName()).isEqualTo("이전 품종");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"name\":\"새 이름\",\"breedId\":\"old-breed\"}",
        "{\"name\":\"새 이름\",\"speciesId\":\"dog\"}",
        "{\"name\":\"새 이름\",\"speciesId\":\"dog\",\"breedId\":\"old-breed\"}"
    })
    void unchangedSpeciesAndBreedPreserveHistoricalBreedWhenCatalogIsMissing(String json) throws Exception {
        Document snapshot = new Document("species", new Document("speciesId", "dog").append("name", "강아지"))
            .append("breed", new Document("breedId", "old-breed").append("name", "이전 품종"));
        mongo.getCollection("pets").updateOne(new Document("_id", "pet-1"),
            new Document("$set", new Document("breedId", "old-breed")
                .append("registration.responseSnapshot", snapshot)));

        mvc.perform(patch("/api/pets/pet-1").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("새 이름"))
            .andExpect(jsonPath("$.data.breed.breedId").value("old-breed"))
            .andExpect(jsonPath("$.data.breed.name").value("이전 품종"));
        assertThat(stored("pet-1").getString("name")).isEqualTo("새 이름");
        assertThat(stored("pet-1").getString("breedId")).isEqualTo("old-breed");
    }

    @Test
    void changedBreedMustExistInCatalog() throws Exception {
        Document before = stored("pet-1");
        mvc.perform(patch("/api/pets/pet-1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"breedId\":\"missing-breed\"}"))
            .andExpect(status().isBadRequest());
        assertThat(stored("pet-1")).isEqualTo(before);
    }

    @Test
    void oldSpeciesSnapshotCannotSupplyNameForNewSpeciesWithSameBreedId() throws Exception {
        Document snapshot = new Document("species", new Document("speciesId", "dog").append("name", "강아지"))
            .append("breed", new Document("breedId", "shared-id").append("name", "강아지 품종"));
        mongo.getCollection("pets").updateOne(new Document("_id", "pet-1"),
            new Document("$set", new Document("speciesId", "cat").append("breedId", "shared-id")
                .append("registration.responseSnapshot", snapshot)));
        Document before = stored("pet-1");

        mongo.getCollection("pets").updateOne(new Document("_id", "pet-1"), new Document("$set", new Document("caregiverId", "owner-a")));
        assertThatThrownBy(() -> new PetListPersistenceAdapter(mongo).getPets(GetPetsQuery.of(0, 4, "owner-a")))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.REFERENCE_DATA_UNAVAILABLE));
        mongo.getCollection("pets").updateOne(new Document("_id", "pet-1"), new Document("$set", new Document("caregiverId", "owner-a")));
        mvc.perform(patch("/api/pets/pet-1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"변경\"}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("REFERENCE_DATA_UNAVAILABLE"));
        assertThat(stored("pet-1")).isEqualTo(before);
    }

    private Document stored(String id) { return mongo.getCollection("pets").find(new Document("_id", id)).first(); }
    private Document pet(String id) {
        return new Document("_id", id).append("caregiverId", "owner-a").append("name", "루이")
            .append("speciesId", "dog").append("sex", "MALE").append("birth", "2024-02-29")
            .append("weight", Decimal128.parse("5.125")).append("createdAt", Date.from(CREATED))
            .append("updatedAt", Date.from(CREATED)).append("registration", new Document("requestKey", "key-1")
                .append("responseSnapshot", new Document("name", "루이")));
    }
}
