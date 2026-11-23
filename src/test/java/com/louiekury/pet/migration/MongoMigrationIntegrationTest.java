package com.louiekury.pet.migration;

import com.louiekury.pet.global.config.MongoMigrationReadiness;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "PET_TEST_MONGODB_URI", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MongoMigrationIntegrationTest {

    private MongoClient client;
    private final List<String> ownedDatabases = new ArrayList<>();

    @BeforeAll
    void connect() {
        client = MongoClients.create(System.getenv("PET_TEST_MONGODB_URI"));
        assertThat(System.getProperty("pet.mongoMigrationClasspath"))
            .as("Gradle test가 별도 migration 실행 경로를 제공해야 한다").isNotBlank();
    }

    @AfterAll
    void removeOnlyOwnedDatabases() {
        if (client != null) {
            for (String name : ownedDatabases) {
                client.getDatabase(name).drop();
            }
            client.close();
        }
    }

    @Test
    @DisplayName("빈 DB를 마이그레이션하면 버전·체크섬·인덱스·기본 종이 준비되고 반복 실행은 중복되지 않는다")
    void migratesEmptyDatabaseOnce() throws Exception {
        MongoDatabase database = freshDatabase();
        assertSuccess(run("migrate", database));
        assertPrepared(database);
        List<Document> history = history(database);
        assertSuccess(run("migrate", database));
        assertSuccess(run("status", database));
        assertThat(history(database)).containsExactlyInAnyOrderElementsOf(history);
        assertThat(database.getCollection("pet_species").countDocuments()).isEqualTo(2);
        assertThat(database.getCollection("DATABASECHANGELOGLOCK").countDocuments()).isEqualTo(1);
    }

    @Test
    @DisplayName("기존 JS가 준비한 DB의 동물·품종은 수정하지 않고 버전 관리에 편입한다")
    void preservesPreviouslyPreparedData() throws Exception {
        MongoDatabase database = freshDatabase();
        createLegacyIndexes(database);
        Document existingPet = new Document("_id", "existing-pet").append("name", "기존 동물")
            .append("caregiverId", null).append("custom", new Document("keep", true));
        database.getCollection("pets").insertOne(existingPet);
        Document dog = new Document("_id", "dog").append("code", "DOG").append("name", "기존 강아지 이름")
            .append("active", true).append("breeds", List.of(new Document("breedId", "existing-breed")
                .append("name", "기존 품종").append("active", true)));
        database.getCollection("pet_species").insertOne(dog);
        assertSuccess(run("migrate", database));
        assertThat(database.getCollection("pets").find().first()).isEqualTo(existingPet);
        assertThat(database.getCollection("pet_species").find(new Document("_id", "dog")).first()).isEqualTo(dog);
        assertPrepared(database);
    }

    @Test
    @DisplayName("두 독립 CLI를 동시에 실행해도 각 변경 이력은 한 번만 기록된다")
    void serializesConcurrentMigrationProcesses() throws Exception {
        MongoDatabase database = freshDatabase();
        Running first = start("migrate", database);
        Running second = start("migrate", database);
        assertSuccess(finish(first));
        assertSuccess(finish(second));
        assertPrepared(database);
        assertThat(history(database)).extracting(row -> row.getString("id"))
            .doesNotHaveDuplicates();
        Document lock = database.getCollection("DATABASECHANGELOGLOCK").find().first();
        assertThat(lock.getBoolean("locked")).isFalse();
    }

    @Test
    @DisplayName("인덱스 이후 종 생성이 실패해도 원인 해소 후 재실행하면 완료된 변경을 중복하지 않는다")
    void resumesAfterPartialSeedFailure() throws Exception {
        MongoDatabase database = freshDatabase();
        database.createCollection("pet_species");
        database.runCommand(new Document("collMod", "pet_species")
            .append("validator", new Document("blocked", new Document("$eq", true))));
        Run failed = run("migrate", database);
        assertThat(failed.exitCode()).as(failed.output()).isNotZero();
        assertThat(history(database)).extracting(row -> row.getString("id"))
            .contains("001-preflight", "002-indexes").doesNotContain("003-seed", "004-verify");
        assertThat(database.getCollection("pet_species").countDocuments()).isZero();
        database.runCommand(new Document("collMod", "pet_species").append("validator", new Document()));
        assertSuccess(run("migrate", database));
        assertPrepared(database);
    }

    @Test
    @DisplayName("다른 실행자가 보유한 정상 락은 덮어쓰지 않고 해제될 때까지 기다린다")
    void preservesHeldLockUntilOwnerReleasesIt() throws Exception {
        MongoDatabase database = freshDatabase();
        Document heldLock = new Document("_id", 1).append("locked", true)
            .append("lockedBy", "test-owned-lock").append("lockGranted", new java.util.Date());
        database.getCollection("DATABASECHANGELOGLOCK").insertOne(heldLock);
        Running running = start("migrate", database);
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (System.nanoTime() < deadline) {
                assertThat(running.process().isAlive()).isTrue();
                assertThat(database.getCollection("DATABASECHANGELOGLOCK").find(new Document("_id", 1)).first())
                    .isEqualTo(heldLock);
                Thread.sleep(100);
            }
            assertThat(history(database)).isEmpty();
        } finally {
            database.getCollection("DATABASECHANGELOGLOCK").updateOne(
                new Document("_id", 1).append("lockedBy", "test-owned-lock"),
                new Document("$set", new Document("locked", false)));
        }
        assertSuccess(finish(running));
        assertPrepared(database);
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-type", "missing-field", "wrong-id"})
    @DisplayName("기존 락 문서가 잘못됐으면 고쳐 쓰거나 마이그레이션을 진행하지 않는다")
    void rejectsMalformedExistingLockWithoutOverwriting(String malformed) throws Exception {
        MongoDatabase database = freshDatabase();
        Document lock = new Document("_id", 1);
        if (malformed.equals("wrong-type")) {
            lock.append("locked", "false");
        } else if (malformed.equals("wrong-id")) {
            lock.put("_id", 2);
            lock.append("locked", false);
        }
        database.getCollection("DATABASECHANGELOGLOCK").insertOne(lock);
        Run failed = run("migrate", database);
        assertThat(failed.exitCode()).as(failed.output()).isNotZero();
        assertThat(database.getCollection("DATABASECHANGELOGLOCK").find().first()).isEqualTo(lock);
        assertThat(history(database)).isEmpty();
        assertThat(database.getCollection("pet_species").countDocuments()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"duplicate-key", "duplicate-species", "duplicate-breed", "conflicting-species-id"})
    @DisplayName("기존 데이터 충돌은 데이터를 덮어쓰거나 무시하지 않고 마이그레이션을 실패시킨다")
    void refusesConflictingExistingData(String conflict) throws Exception {
        MongoDatabase database = freshDatabase();
        if (conflict.equals("duplicate-key")) {
            database.getCollection("pets").insertMany(List.of(
                new Document("_id", "first").append("registration", new Document("requestKey", "duplicate")),
                new Document("_id", "second").append("registration", new Document("requestKey", "duplicate"))));
        } else if (conflict.equals("duplicate-species")) {
            database.getCollection("pet_species").insertMany(List.of(species("dog", "DOG"), species("other-dog", "DOG")));
        } else if (conflict.equals("duplicate-breed")) {
            database.getCollection("pet_species").insertOne(species("dog", "DOG").append("breeds", List.of(
                new Document("breedId", "same").append("name", "첫 품종").append("active", true),
                new Document("breedId", "same").append("name", "다음 품종").append("active", true))));
        } else {
            database.getCollection("pet_species").insertOne(species("dog", "CAT"));
        }
        List<Document> originalPets = database.getCollection("pets").find().into(new ArrayList<>());
        List<Document> originalSpecies = database.getCollection("pet_species").find().into(new ArrayList<>());
        Run failed = run("migrate", database);
        assertThat(failed.exitCode()).as(failed.output()).isNotZero();
        assertThat(database.getCollection("pets").find().into(new ArrayList<>())).isEqualTo(originalPets);
        assertThat(database.getCollection("pet_species").find().into(new ArrayList<>())).isEqualTo(originalSpecies);
        assertThat(history(database)).extracting(row -> row.getString("id"))
            .doesNotContain("003-seed", "004-verify");
    }

    @Test
    @DisplayName("이력 체크섬이 변조되면 재실행을 거부한다")
    void rejectsTamperedChecksum() throws Exception {
        MongoDatabase database = freshDatabase();
        assertSuccess(run("migrate", database));
        Document row = database.getCollection("DATABASECHANGELOG").find(new Document("id", "001-preflight")).first();
        assertThat(row).isNotNull();
        database.getCollection("DATABASECHANGELOG").updateOne(new Document("_id", row.get("_id")),
            new Document("$set", new Document("md5sum", "9:00000000000000000000000000000000")));
        Run failed = run("migrate", database);
        assertThat(failed.exitCode()).as(failed.output()).isNotZero();
        assertThat(failed.output()).contains("MIGRATION_FAILED")
            .doesNotContain("MIGRATION_COMPLETE");
        assertThat(history(database)).hasSize(4);
    }

    @Test
    @DisplayName("앱 준비 검사는 빈 DB에 쓰기를 수행하지 않고 시작을 거부한다")
    void readinessRejectsUnpreparedDatabaseWithoutWriting() {
        MongoDatabase database = freshDatabase();
        assertThatThrownBy(() -> MongoMigrationReadiness.verify(database)).isInstanceOf(RuntimeException.class);
        assertThat(database.listCollectionNames().into(new ArrayList<>())).isEmpty();
    }

    @Test
    @DisplayName("앱 준비 검사는 완료된 DB를 읽기만 하며 부족한 인덱스를 복구하지 않는다")
    void readinessIsReadOnlyAndRejectsMissingIndex() throws Exception {
        MongoDatabase database = freshDatabase();
        assertSuccess(run("migrate", database));
        List<Document> beforeHistory = history(database);
        List<Document> beforeSpecies = database.getCollection("pet_species").find().into(new ArrayList<>());
        MongoMigrationReadiness.verify(database);
        assertThat(history(database)).isEqualTo(beforeHistory);
        assertThat(database.getCollection("pet_species").find().into(new ArrayList<>())).isEqualTo(beforeSpecies);
        database.getCollection("pets").dropIndex("registration_request_key_unique");
        assertThatThrownBy(() -> MongoMigrationReadiness.verify(database)).isInstanceOf(RuntimeException.class);
        assertThat(database.getCollection("pets").listIndexes().into(new ArrayList<>()))
            .noneMatch(index -> "registration_request_key_unique".equals(index.getString("name")));
    }

    private MongoDatabase freshDatabase() {
        String name = "pet_migration_test_" + UUID.randomUUID().toString().replace("-", "");
        ownedDatabases.add(name);
        return client.getDatabase(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-history", "inactive-species", "held-lock", "missing-species-index"})
    @DisplayName("이력·활성 종·해제된 락·종 고유 인덱스 중 하나라도 없으면 앱 준비 검사를 실패시킨다")
    void readinessRequiresEveryPrerequisite(String missing) throws Exception {
        MongoDatabase database = freshDatabase();
        assertSuccess(run("migrate", database));
        switch (missing) {
            case "missing-history" -> database.getCollection("DATABASECHANGELOG")
                .deleteOne(new Document("id", "004-verify"));
            case "inactive-species" -> database.getCollection("pet_species")
                .updateOne(new Document("_id", "dog"), new Document("$set", new Document("active", false)));
            case "held-lock" -> database.getCollection("DATABASECHANGELOGLOCK")
                .updateOne(new Document(), new Document("$set", new Document("locked", true)));
            case "missing-species-index" -> database.getCollection("pet_species").dropIndex("species_code_unique");
            default -> throw new AssertionError(missing);
        }
        List<Document> history = history(database);
        assertThatThrownBy(() -> MongoMigrationReadiness.verify(database)).isInstanceOf(RuntimeException.class);
        assertThat(history(database)).isEqualTo(history);
    }

    private static List<Document> history(MongoDatabase database) {
        return database.getCollection("DATABASECHANGELOG").find().into(new ArrayList<>());
    }

    private static void assertPrepared(MongoDatabase database) {
        List<Document> rows = history(database);
        assertThat(rows).extracting(row -> row.getString("id"))
            .containsExactlyInAnyOrder("001-preflight", "002-indexes", "003-seed", "004-verify");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getString("author")).isEqualTo("louie-kury");
            assertThat(row.getString("md5sum")).isNotBlank();
            assertThat(row.getString("execType")).isEqualTo("EXECUTED");
        });
        List<Document> indexes = database.getCollection("pets").listIndexes().into(new ArrayList<>());
        assertThat(indexes).anySatisfy(index -> {
            assertThat(index.getString("name")).isEqualTo("registration_request_key_unique");
            assertThat(index.getBoolean("unique")).isTrue();
            assertThat(index.get("key")).isEqualTo(new Document("registration.requestKey", 1));
            assertThat(index.get("partialFilterExpression")).isEqualTo(new Document("registration.requestKey", new Document("$type", "string")));
        });
        assertThat(database.getCollection("pet_species").countDocuments(new Document("code", "DOG").append("active", true))).isEqualTo(1);
        assertThat(database.getCollection("pet_species").countDocuments(new Document("code", "CAT").append("active", true))).isEqualTo(1);
    }

    private static void createLegacyIndexes(MongoDatabase database) {
        database.getCollection("pets").createIndex(new Document("registration.requestKey", 1),
            new IndexOptions().name("registration_request_key_unique").unique(true)
                .partialFilterExpression(new Document("registration.requestKey", new Document("$type", "string"))));
        database.getCollection("pet_species").createIndex(new Document("code", 1),
            new IndexOptions().name("species_code_unique").unique(true));
    }

    private static Document species(String id, String code) {
        return new Document("_id", id).append("code", code).append("name", "종 이름")
            .append("active", true).append("breeds", List.of());
    }

    private static Running start(String command, MongoDatabase database) throws Exception {
        Path output = Files.createTempFile("pet-migration-test-", ".log");
        ProcessBuilder builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", System.getProperty("pet.mongoMigrationClasspath"),
            "com.louiekury.pet.migration.MongoMigrationMain", command);
        builder.environment().put("MONGODB_MIGRATION_URI", System.getenv("PET_TEST_MONGODB_URI"));
        builder.environment().put("MONGODB_MIGRATION_DATABASE", database.getName());
        builder.redirectErrorStream(true).redirectOutput(output.toFile());
        return new Running(builder.start(), output);
    }

    private static Run run(String command, MongoDatabase database) throws Exception {
        return finish(start(command, database));
    }

    private static Run finish(Running running) throws Exception {
        try {
            if (!running.process().waitFor(60, TimeUnit.SECONDS)) {
                running.process().destroyForcibly();
                throw new AssertionError("마이그레이션 제한시간 초과: " + Files.readString(running.output()));
            }
            return new Run(running.process().exitValue(), Files.readString(running.output(), StandardCharsets.UTF_8));
        } finally {
            Files.deleteIfExists(running.output());
        }
    }

    private static void assertSuccess(Run run) {
        assertThat(run.exitCode()).as(run.output()).isZero();
    }

    private record Running(Process process, Path output) { }
    private record Run(int exitCode, String output) { }
}
