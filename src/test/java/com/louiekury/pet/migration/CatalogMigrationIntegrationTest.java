package com.louiekury.pet.migration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "PET_TEST_MONGODB_URI", matches = ".+")
@EnabledIfEnvironmentVariable(named = "PET_TEST_MYSQL_ADMIN_URL", matches = ".+")
class CatalogMigrationIntegrationTest {
    private String databaseName;
    private String mysqlUrl;
    private MongoClient client;
    private MongoDatabase mongo;

    @BeforeEach
    void setup() throws Exception {
        databaseName = "pet_catalog_test_" + UUID.randomUUID().toString().replace("-", "");
        String adminUrl = System.getenv("PET_TEST_MYSQL_ADMIN_URL");
        if (!adminUrl.endsWith("/")) throw new IllegalArgumentException("관리 URL은 /로 끝나야 합니다.");
        try (Connection admin = DriverManager.getConnection(adminUrl, "root", "");
                var statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + databaseName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
        mysqlUrl = adminUrl + databaseName + "?allowPublicKeyRetrieval=true&useSSL=false";
        String ddl = Files.readString(Path.of("src/migration/resources/db/mysql/catalog-schema.sql"))
            .replaceAll("(?m)^--.*$", "");
        try (Connection sql = DriverManager.getConnection(mysqlUrl, "root", ""); var statement = sql.createStatement()) {
            for (String command : ddl.split(";")) {
                if (!command.isBlank()) statement.execute(command);
            }
        }
        client = MongoClients.create(System.getenv("PET_TEST_MONGODB_URI"));
        mongo = client.getDatabase(databaseName);
        mongo.getCollection("pet_species").insertMany(List.of(
            new Document("_id", "dog").append("name", "강아지").append("breeds", List.of(
                new Document("breedId", "poodle").append("name", "푸들").append("active", false))),
            new Document("_id", "cat").append("name", "고양이").append("breeds", List.of())));
        mongo.getCollection("pets").insertOne(new Document("_id", "pet-1")
            .append("speciesId", "dog").append("breedId", "poodle")
            .append("registration", new Document("responseSnapshot", new Document("species",
                new Document("speciesId", "dog").append("name", "강아지"))
                .append("breed", new Document("breedId", "poodle").append("name", "푸들")))));
    }

    @AfterEach
    void cleanup() throws Exception {
        if (mongo != null) mongo.drop();
        if (client != null) client.close();
        if (databaseName != null) {
            try (Connection admin = DriverManager.getConnection(System.getenv("PET_TEST_MYSQL_ADMIN_URL"), "root", "");
                    var statement = admin.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS " + databaseName);
            }
        }
    }

    @Test
    void planDoesNotWriteAndApplyIsRepeatable() throws Exception {
        assertThat(run("plan")).isZero();
        assertThat(count("species")).isZero();
        assertThat(mongo.getCollection("pets").find().first().getString("speciesId")).isEqualTo("dog");
        assertThat(run("apply")).isZero();
        Document migrated = mongo.getCollection("pets").find().first();
        String speciesId = migrated.getString("speciesId");
        String breedId = migrated.getString("breedId");
        assertThat(UUID.fromString(speciesId)).isNotNull();
        assertThat(UUID.fromString(breedId)).isNotNull();
        Document snapshot = migrated.get("registration", Document.class).get("responseSnapshot", Document.class);
        assertThat(snapshot.get("species", Document.class).getString("speciesId")).isEqualTo(speciesId);
        assertThat(snapshot.get("breed", Document.class).getString("breedId")).isEqualTo(breedId);
        try (Connection sql = DriverManager.getConnection(mysqlUrl, "root", "");
                var query = sql.prepareStatement("SELECT s.uuid, b.uuid FROM breed b JOIN species s ON s.id=b.species_id" );
                var rows = query.executeQuery()) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo(speciesId);
            assertThat(rows.getString(2)).isEqualTo(breedId);
        }
        assertThat(run("apply")).isZero();
        assertThat(count("species")).isEqualTo(2);
        assertThat(count("breed")).isEqualTo(1);
        assertThat(mongo.getCollection("pets").find().first()).isEqualTo(migrated);
    }

    @Test
    void conflictingExistingMysqlRowStopsBeforeAnyWrites() throws Exception {
        try (Connection sql = DriverManager.getConnection(mysqlUrl, "root", "");
                var insert = sql.prepareStatement("INSERT INTO species (uuid, name) VALUES (?, ?)") ) {
            insert.setString(1, UUID.randomUUID().toString());
            insert.setString(2, "강아지");
            insert.executeUpdate();
        }
        assertThat(run("apply")).isNotZero();
        assertThat(count("species")).isEqualTo(1);
        assertThat(count("breed")).isZero();
        assertThat(mongo.getCollection("pets").find().first().getString("speciesId")).isEqualTo("dog");
    }

    @Test
    void orphanedMysqlBreedStopsBeforeAnyWrites() throws Exception {
        String key = "dog" + "\u0000" + "poodle";
        String id = UUID.nameUUIDFromBytes(("louie-kury/catalog/breed/" + key)
            .getBytes(StandardCharsets.UTF_8)).toString();
        try (Connection sql = DriverManager.getConnection(mysqlUrl, "root", "");
                var insert = sql.prepareStatement("INSERT INTO breed (uuid, species_id, name) VALUES (?, ?, ?)") ) {
            insert.setString(1, id);
            insert.setLong(2, 999999L);
            insert.setString(3, "푸들");
            insert.executeUpdate();
        }
        assertThat(run("apply")).isNotZero();
        assertThat(count("species")).isZero();
        assertThat(count("breed")).isEqualTo(1);
        assertThat(mongo.getCollection("pets").find().first().getString("speciesId")).isEqualTo("dog");
    }

    @Test
    void missingBreedStopsBeforeAnyWrites() throws Exception {
        mongo.getCollection("pets").updateOne(new Document("_id", "pet-1"),
            new Document("$set", new Document("breedId", "missing")));
        assertThat(run("apply")).isNotZero();
        assertThat(count("species")).isZero();
        assertThat(mongo.getCollection("pets").find().first().getString("speciesId")).isEqualTo("dog");
    }

    private long count(String table) throws Exception {
        try (Connection sql = DriverManager.getConnection(mysqlUrl, "root", "");
                var statement = sql.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private int run(String command) throws Exception {
        Path output = Files.createTempFile("pet-catalog-migration-", ".log");
        try {
            ProcessBuilder builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("pet.mongoMigrationClasspath"),
                "com.louiekury.pet.migration.CatalogMigrationMain", command);
            builder.environment().put("MONGODB_MIGRATION_URI", System.getenv("PET_TEST_MONGODB_URI"));
            builder.environment().put("MONGODB_MIGRATION_DATABASE", databaseName);
            builder.environment().put("MYSQL_MIGRATION_URL", mysqlUrl);
            builder.environment().put("MYSQL_MIGRATION_USERNAME", "root");
            builder.environment().put("MYSQL_MIGRATION_PASSWORD", "");
            builder.redirectErrorStream(true).redirectOutput(output.toFile());
            Process process = builder.start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new AssertionError("이관 테스트 시간 초과");
            }
            int result = process.exitValue();
            if (result != 0 && "plan".equals(command)) {
                throw new AssertionError(Files.readString(output, StandardCharsets.UTF_8));
            }
            return result;
        } finally {
            Files.deleteIfExists(output);
        }
    }
}
