package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.createpet.PetResult;
import com.louiekury.pet.pet.application.createpet.CreatePetCommand;
import com.louiekury.pet.pet.application.createpet.CreatePetService;
import com.louiekury.pet.pet.domain.Pet;
import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.IndexOptions;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "PET_TEST_MONGODB_URI", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PetMongoIntegrationTest {

    private static final Instant CREATED = Instant.parse("2026-11-22T15:30:00Z");
    private MongoClient client;
    private MongoTemplate template;
    private PetPersistenceAdapter adapter;

    @BeforeAll
    void connectToDedicatedDatabase() {
        client = MongoClients.create(System.getenv("PET_TEST_MONGODB_URI"));
        template = new MongoTemplate(client, "pet_registration_test_" + UUID.randomUUID().toString().replace("-", ""));
        adapter = new PetPersistenceAdapter(template);
        template.createCollection("pets");
        createKeyIndex();
    }

    @BeforeEach
    void clearOnlyTestCollections() {
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

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "Asia/Seoul"})
    @DisplayName("실제 Mongo 왕복에서 날짜·체중·미연결 소유자·감사 시각을 보존한다")
    void persistsExactTypesAndSnapshotRegardlessOfJvmZone(String zone) {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            PetResult result = result("루이");
            adapter.createPet(result, null);
            Document stored = template.getCollection("pets").find(new Document("_id", result.getPetId())).first();
            assertThat(stored).isNotNull();
            assertThat(stored.get("_id")).isInstanceOf(String.class).isEqualTo(result.getPetId());
            assertThat(stored).containsKey("caregiverId");
            assertThat(stored.get("caregiverId")).isNull();
            assertThat(stored.get("birth")).isEqualTo("2024-02-29");
            assertThat(stored.get("weight")).isInstanceOf(Decimal128.class);
            assertThat(stored.get("weight", Decimal128.class).bigDecimalValue()).isEqualByComparingTo("5.123");
            assertThat(stored.getDate("createdAt")).isEqualTo(Date.from(CREATED));
            assertThat(stored.getDate("updatedAt")).isEqualTo(Date.from(CREATED));
            Document registration = stored.get("registration", Document.class);
            assertThat(registration).doesNotContainKey("requestKey");
            Document snapshot = registration.get("responseSnapshot", Document.class);
            assertThat(snapshot.get("petId")).isEqualTo(result.getPetId());
            assertThat(snapshot.getDate("createdAt")).isEqualTo(stored.getDate("createdAt"));
            assertThat(snapshot.getDate("updatedAt")).isEqualTo(stored.getDate("updatedAt"));
            PetMongoDocument loaded = template.findById(result.getPetId(), PetMongoDocument.class);
            assertThat(loaded.getBirth()).isEqualTo("2024-02-29");
            assertThat(loaded.getWeight()).isEqualByComparingTo("5.123");
            assertThat(loaded.getCreatedAt()).isEqualTo(CREATED);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    @DisplayName("나노초 Clock에서도 최초 응답과 실제 Mongo 감사 시각은 정확히 일치한다")
    void responseAuditTimesExactlyMatchStoredMilliseconds() {
        CreatePetService service = new CreatePetService(
            (species, breedId) -> Optional.empty(), com.louiekury.pet.pet.support.CatalogFixtures.species(), adapter,
            Clock.fixed(Instant.parse("2026-11-22T15:30:00.123456789Z"), ZoneOffset.UTC));
        PetResult result = service.createPet(CreatePetCommand.of("루이", "dog", null, null,
            null, "MALE", null, null, null, null, "member-1"));
        Document stored = template.getCollection("pets").find(new Document("_id", result.getPetId())).first();
        assertThat(stored.getString("caregiverId")).isEqualTo("member-1");
        assertThat(stored.getDate("createdAt").toInstant()).isEqualTo(result.getCreatedAt());
        assertThat(stored.getDate("updatedAt").toInstant()).isEqualTo(result.getUpdatedAt());
    }

    @Test
    @DisplayName("실제 partial unique 인덱스는 무키 등록 여러 건을 허용한다")
    void permitsMultipleRequestsWithoutKey() {
        adapter.createPet(result("첫째"), null);
        adapter.createPet(result("둘째"), null);
        assertThat(template.getCollection("pets").countDocuments()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"루이", "다른 동물"})
    @DisplayName("같은 익명 키는 입력이 같거나 달라도 409이며 원본을 보존한다")
    void rejectsKeyReuseRegardlessOfPayload(String nextName) {
        PetResult first = result("루이");
        adapter.createPet(first, "same-key");
        assertThatThrownBy(() -> adapter.createPet(result(nextName), "same-key"))
            .isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
        assertThat(template.getCollection("pets").countDocuments()).isEqualTo(1);
        assertThat(template.getCollection("pets").find().first().getString("_id")).isEqualTo(first.getPetId());
    }

    @Test
    @DisplayName("동시에 동일 키로 등록해도 실제 Mongo에는 정확히 한 건만 저장된다")
    void concurrentRequestsCreateExactlyOnePet() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int i = 0; i < 8; i++) {
                results.add(executor.submit(() -> {
                    assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                    try {
                        adapter.createPet(result("동시 등록"), "concurrent-key");
                        return true;
                    } catch (BusinessException exception) {
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT);
                        return false;
                    }
                }));
            }
            start.countDown();
            int successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get(20, TimeUnit.SECONDS)) {
                    successes++;
                }
            }
            assertThat(successes).isEqualTo(1);
            assertThat(template.getCollection("pets").countDocuments()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("필수 키 인덱스가 없으면 중복 방지를 가장하지 않고 저장을 거부한다")
    void refusesKeyedInsertWithoutRequiredIndex() {
        template.getCollection("pets").dropIndex("registration_request_key_unique");
        try {
            assertThatThrownBy(() -> adapter.createPet(result("루이"), "key"))
                .isInstanceOfSatisfying(BusinessException.class,
                    exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PERSISTENCE_UNAVAILABLE));
            assertThat(template.getCollection("pets").countDocuments()).isZero();
        } finally {
            createKeyIndex();
        }
    }

    @Test
    @DisplayName("다른 고유 인덱스 충돌을 재시도 키 충돌로 잘못 보고하지 않는다")
    void distinguishesUnrelatedUniqueConstraint() {
        template.getCollection("pets").createIndex(new Document("name", 1), new IndexOptions().unique(true).name("test_name_unique"));
        try {
            adapter.createPet(result("루이"), "first");
            assertThatThrownBy(() -> adapter.createPet(result("루이"), "second"))
                .isInstanceOfSatisfying(BusinessException.class,
                    exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PERSISTENCE_UNAVAILABLE));
        } finally {
            template.getCollection("pets").dropIndex("test_name_unique");
        }
    }

    @Test
    @DisplayName("기존 카탈로그의 종 메타데이터와 무관하게 선택 종의 활성 품종만 조회한다")
    void readsOnlyActiveBreedsWithinRequestedSpecies() {
        template.getCollection("pet_species").insertOne(new Document("_id", "dog")
            .append("name", "변경된 이름").append("code", "OUTDATED").append("active", false)
            .append("breeds", List.of(new Document("breedId", "poodle").append("name", "푸들").append("active", true),
                new Document("breedId", "inactive").append("name", "미사용").append("active", false))));
        var breeds = template.findById("dog", PetBreedCatalogMongoDocument.class);
        var breed = breeds.getPetBreed(new PetSpecies("dog", "강아지"), "poodle").orElseThrow();
        assertThat(breed.getSpecies()).isEqualTo(new PetSpecies("dog", "강아지"));
        assertThat(breed.getName()).isEqualTo("푸들");
    }

    @ParameterizedTest
    @ValueSource(strings = {"inactive", "missing", "wrong-species"})
    @DisplayName("없는 품종·비활성 품종·다른 종의 품종은 조회 결과로 제공하지 않는다")
    void rejectsUnavailableBreedWithinCatalog(String condition) {
        template.getCollection("pet_species").insertOne(new Document("_id", "dog")
            .append("breeds", List.of(
                new Document("breedId", "poodle").append("name", "푸들").append("active", true),
                new Document("breedId", "inactive").append("name", "미사용").append("active", false))));
        PetSpecies species = condition.equals("wrong-species") ? new PetSpecies("cat", "고양이") : new PetSpecies("dog", "강아지");
        String breedId = condition.equals("wrong-species") ? "poodle" : condition;
        PetBreedCatalogMongoDocument catalog = template.findById(species.getSpeciesId(), PetBreedCatalogMongoDocument.class);
        assertThat(catalog == null ? Optional.empty() : catalog.getPetBreed(species, breedId)).isEmpty();
    }

    @Test
    @DisplayName("중복 품종 데이터는 임의 선택하거나 정상 부재로 숨기지 않고 조회 오류로 알린다")
    void rejectsAmbiguousBreedCatalog() {
        template.getCollection("pet_species").insertOne(new Document("_id", "dog")
            .append("breeds", List.of(
                new Document("breedId", "poodle").append("name", "첫 품종").append("active", true),
                new Document("breedId", "poodle").append("name", "다른 품종").append("active", true))));
        assertThatThrownBy(() -> template.findById("dog", PetBreedCatalogMongoDocument.class).getPetBreed(new PetSpecies("dog", "강아지"), "poodle"))
            .isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFERENCE_DATA_UNAVAILABLE));
    }

    private void createKeyIndex() {
        template.getCollection("pets").createIndex(new Document("registration.requestKey", 1),
            new IndexOptions().name("registration_request_key_unique").unique(true)
                .partialFilterExpression(new Document("registration.requestKey", new Document("$type", "string"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"dog", "cat"})
    @DisplayName("MySQL 종 확인 후 기존 종 ID를 MongoDB에 저장한다")
    void storesCanonicalEnumIdentifierWithoutCatalog(String speciesId) {
        CreatePetService service = new CreatePetService(com.louiekury.pet.pet.support.CatalogFixtures.noBreeds(), com.louiekury.pet.pet.support.CatalogFixtures.species(), adapter,
            Clock.fixed(CREATED, ZoneOffset.UTC));
        PetResult result = service.createPet(CreatePetCommand.of("루이", speciesId, null, null,
            null, "MALE", null, null, null, null, "member-1"));
        Document stored = template.getCollection("pets").find(new Document("_id", result.getPetId())).first();
        assertThat(stored.getString("speciesId")).isEqualTo(speciesId);
        assertThat(result.getSpecies().getName()).isEqualTo(speciesId.equals("dog") ? "강아지" : "고양이");
        assertThat(template.getCollection("pet_species").countDocuments()).isZero();
    }

    private static PetResult result(String name) {
        LocalDate today = LocalDate.of(2026, 11, 23);
        Pet pet = Pet.create(name, new PetSpecies("dog", "강아지"), null, null, LocalDate.of(2024, 2, 29), "MALE",
            null, new BigDecimal("5.123"), null);
        return PetResult.of(pet, null, CREATED, java.time.Period.between(pet.getBirth(), today));
    }
}
