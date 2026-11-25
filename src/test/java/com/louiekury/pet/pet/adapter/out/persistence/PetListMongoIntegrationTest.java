package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.getpets.GetPetsQuery;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "PET_TEST_MONGODB_URI", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PetListMongoIntegrationTest {

    private static final Instant CREATED = Instant.parse("2026-11-22T15:00:00Z");
    private MongoClient client;
    private MongoTemplate template;
    private PetListPersistenceAdapter adapter;

    @BeforeAll
    void connectToDedicatedDatabase() {
        client = MongoClients.create(System.getenv("PET_TEST_MONGODB_URI"));
        template = new MongoTemplate(client, "pet_listing_test_" + UUID.randomUUID().toString().replace("-", ""));
        adapter = new PetListPersistenceAdapter(template);
    }

    @BeforeEach
    void clearOwnedTestCollections() {
        template.getCollection("pets").deleteMany(new Document());
        template.getCollection("pet_species").deleteMany(new Document());
    }

    @AfterAll
    void removeOnlyDedicatedDatabase() {
        if (template != null) {
            template.getDb().drop();
        }
        if (client != null) {
            client.close();
        }
    }

    @Test
    @DisplayName("목록과 total은 소유자 null·누락의 활성 동물만 포함하고 유소유자·삭제 동물은 제외한다")
    void isolatesUnownedActivePetsForContentAndCount() {
        Document noOwner = pet("missing-owner", null, CREATED);
        noOwner.remove("caregiverId");
        template.getCollection("pets").insertMany(List.of(
            pet("owned-live", "owner-a", CREATED),
            pet("other-live", "owner-b", CREATED),
            pet("anonymous", null, CREATED), noOwner,
            pet("anonymous-deleted", null, CREATED).append("deletedAt", Date.from(CREATED))));

        var result = adapter.getPets(GetPetsQuery.of(0, 4));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(item -> item.getPetId()).containsExactly("missing-owner", "anonymous");
    }

    @Test
    @DisplayName("동일 등록 시각은 ID 내림차순으로 정렬하며 페이지 사이에 중복·누락 없이 조회한다")
    void paginatesWithStableCreationAndIdOrdering() {
        List<Document> documents = new ArrayList<>();
        for (String id : List.of("a", "f", "c", "e", "b", "d")) {
            documents.add(pet(id, null, CREATED));
        }
        documents.add(pet("newest", null, CREATED.plusSeconds(1)));
        template.getCollection("pets").insertMany(documents);

        var first = adapter.getPets(GetPetsQuery.of(0, 4));
        var second = adapter.getPets(GetPetsQuery.of(1, 4));
        var beyond = adapter.getPets(GetPetsQuery.of(2, 4));

        assertThat(first.getContent()).extracting(item -> item.getPetId()).containsExactly("newest", "f", "e", "d");
        assertThat(second.getContent()).extracting(item -> item.getPetId()).containsExactly("c", "b", "a");
        assertThat(beyond.getContent()).isEmpty();
        assertThat(first.getTotalElements()).isEqualTo(7);
        assertThat(second.getTotalElements()).isEqualTo(7);
        assertThat(beyond.getTotalElements()).isEqualTo(7);
    }

    @Test
    @DisplayName("등록 snapshot의 오래된 이름·종·생일 대신 현재 최상위 저장 필드를 조회한다")
    void readsCurrentPetFieldsInsteadOfRegistrationSnapshot() {
        template.getCollection("pets").insertOne(pet("current", null, CREATED)
            .append("name", "새 이름").append("birth", "2024-02-29")
            .append("registration", new Document("responseSnapshot", new Document("name", "오래된 이름")
                .append("birth", "2000-01-01").append("age", new Document("years", 99))
                .append("species", new Document("speciesId", "cat")))));

        var current = adapter.getPets(GetPetsQuery.of(0, 4)).getContent().get(0);

        assertThat(current.getName()).isEqualTo("새 이름");
        assertThat(current.getBirth()).hasToString("2024-02-29");
        assertThat(current.getSpecies().getSpeciesId()).isEqualTo("dog");
    }

    @Test
    @DisplayName("비활성 품종도 현재 기준 명칭으로 표시하고 종이 다른 같은 품종 ID를 섞지 않는다")
    void resolvesCurrentInactiveBreedNamesWithinSpecies() {
        template.getCollection("pets").insertMany(List.of(
            withBreed(pet("dog", null, CREATED), "shared", "과거 강아지 품종"),
            withBreed(pet("cat", null, CREATED).append("speciesId", "cat"), "shared", "과거 고양이 품종")));
        template.getCollection("pet_species").insertMany(List.of(
            new Document("_id", "dog").append("breeds", List.of(new Document("breedId", "shared")
                .append("name", "강아지 현재 품종").append("active", false))),
            new Document("_id", "cat").append("breeds", List.of(new Document("breedId", "shared")
                .append("name", "고양이 현재 품종").append("active", true)))));

        var result = adapter.getPets(GetPetsQuery.of(0, 4));

        assertThat(result.getContent()).extracting(item -> item.getBreedName())
            .containsExactly("강아지 현재 품종", "고양이 현재 품종");
    }

    @Test
    @DisplayName("현재 품종이 사라졌으면 같은 품종 ID의 등록 당시 표시명을 보존한다")
    void preservesMatchingHistoricalBreedNameWhenCatalogIsMissing() {
        template.getCollection("pets").insertOne(withBreed(pet("pet", null, CREATED), "poodle", "푸들"));
        var result = adapter.getPets(GetPetsQuery.of(0, 4)).getContent().get(0);
        assertThat(result.getBreedId()).isEqualTo("poodle");
        assertThat(result.getBreedName()).isEqualTo("푸들");
    }

    @Test
    @DisplayName("현재 품종과 다른 ID의 snapshot 표시명은 사용하지 않고 오류로 처리한다")
    void rejectsMismatchingHistoricalBreed() {
        template.getCollection("pets").insertOne(withBreed(pet("pet", null, CREATED), "old", "다른 품종")
            .append("breedId", "current"));
        assertCode(() -> adapter.getPets(GetPetsQuery.of(0, 4)), ErrorCode.REFERENCE_DATA_UNAVAILABLE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-date", "2023-02-29"})
    @DisplayName("손상된 저장 생일은 정상 정보처럼 반환하지 않고 조회 오류로 처리한다")
    void rejectsInvalidStoredBirth(String birth) {
        template.getCollection("pets").insertOne(pet("pet", null, CREATED).append("birth", birth));
        assertCode(() -> adapter.getPets(GetPetsQuery.of(0, 4)), ErrorCode.PET_QUERY_UNAVAILABLE);
    }

    private static Document pet(String id, String caregiverId, Instant createdAt) {
        return new Document("_id", id).append("caregiverId", caregiverId).append("name", id)
            .append("speciesId", "dog").append("breedId", null).append("profileMediaId", null)
            .append("birth", null).append("sex", "MALE").append("createdAt", Date.from(createdAt));
    }

    private static Document withBreed(Document pet, String breedId, String name) {
        return pet.append("breedId", breedId).append("registration", new Document("responseSnapshot",
            new Document("species", new Document("speciesId", pet.getString("speciesId")))
                .append("breed", new Document("breedId", breedId).append("name", name))));
    }

    private static void assertCode(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }
}
