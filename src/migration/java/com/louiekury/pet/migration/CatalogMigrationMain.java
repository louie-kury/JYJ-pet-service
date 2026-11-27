package com.louiekury.pet.migration;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;

/** 기존 MongoDB 식별자를 고정 UUID로 변환한다. 실행 전 모든 쓰기를 중지해야 한다. */
public final class CatalogMigrationMain {
    private CatalogMigrationMain() { }

    public static void main(String[] args) {
        try {
            if (args.length != 1 || !("plan".equals(args[0]) || "apply".equals(args[0]))) {
                throw new IllegalArgumentException("plan 또는 apply가 필요합니다.");
            }
            String mongoUri = MongoMigrationMain.resolveUrl(System.getenv("MONGODB_MIGRATION_URI"),
                System.getenv("MONGODB_MIGRATION_DATABASE"));
            String mysqlUrl = required("MYSQL_MIGRATION_URL");
            String mysqlUser = required("MYSQL_MIGRATION_USERNAME");
            String mysqlPassword = System.getenv("MYSQL_MIGRATION_PASSWORD");
            if (mysqlPassword == null) {
                throw new IllegalArgumentException("MySQL 비밀번호가 필요합니다.");
            }
            try (var mongo = MongoClients.create(mongoUri);
                    var sql = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword)) {
                MongoDatabase database = mongo.getDatabase(required("MONGODB_MIGRATION_DATABASE"));
                Plan plan = plan(database);
                validateMysql(sql, plan);
                System.out.println("CATALOG_PLAN species=" + plan.species.size() + " breeds=" + plan.breeds.size()
                    + " pets=" + plan.petCount);
                if ("apply".equals(args[0])) {
                    applyMysql(sql, plan);
                    applyMongo(database, plan);
                    System.out.println("CATALOG_MIGRATION_COMPLETE");
                }
            }
        } catch (Exception exception) {
            System.err.println("CATALOG_MIGRATION_FAILED: 설정·기존 데이터·참조 관계를 확인하세요. ("
                + exception.getClass().getSimpleName() + ")");
            System.exit(1);
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 누락");
        }
        return value;
    }

    static Plan plan(MongoDatabase database) {
        Plan plan = new Plan();
        plan.addSpecies("dog", "강아지");
        plan.addSpecies("cat", "고양이");
        for (Document species : database.getCollection("pet_species").find()) {
            String legacyId = text(species.get("_id"));
            String name = "dog".equals(legacyId) ? "강아지" : "cat".equals(legacyId) ? "고양이"
                : text(species.get("name"));
            plan.addSpecies(legacyId, name);
            Object breeds = species.get("breeds");
            if (!(breeds instanceof List<?>)) {
                throw new CatalogProblem("품종 목록이 올바르지 않습니다.");
            }
            for (Object item : (List<?>) breeds) {
                if (!(item instanceof Document breed)) {
                    throw new CatalogProblem("품종 형식이 올바르지 않습니다.");
                }
                plan.addBreed(legacyId, text(breed.get("breedId")), text(breed.get("name")));
            }
        }
        for (Document pet : database.getCollection("pets").find()) {
            String species = plan.speciesLegacy(text(pet.get("speciesId")));
            Object breed = pet.get("breedId");
            if (breed != null) plan.requireBreed(species, text(breed));
            Document registration = pet.get("registration", Document.class);
            Document snapshot = registration == null ? null : registration.get("responseSnapshot", Document.class);
            if (snapshot != null && snapshot.get("species") instanceof Document savedSpecies) {
                String savedId = plan.speciesLegacy(text(savedSpecies.get("speciesId")));
                if (snapshot.get("breed") instanceof Document savedBreed) {
                    plan.requireBreed(savedId, text(savedBreed.get("breedId")));
                }
            }
            plan.petCount++;
        }
        return plan;
    }

    private static String text(Object value) {
        if (!(value instanceof String string) || string.isBlank() || string.length() > 255) {
            throw new CatalogProblem("기준 데이터 식별자 또는 이름이 올바르지 않습니다.");
        }
        return string;
    }

    private static String uuid(String type, String value) {
        return UUID.nameUUIDFromBytes(("louie-kury/catalog/" + type + "/" + value)
            .getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static void validateMysql(Connection sql, Plan plan) throws SQLException {
        for (Item item : plan.species.values()) {
            try (var query = sql.prepareStatement("SELECT uuid, name FROM species WHERE uuid = ? OR name = ?")) {
                query.setString(1, item.uuid);
                query.setString(2, item.name);
                try (var rows = query.executeQuery()) {
                    while (rows.next()) {
                        if (!item.uuid.equals(rows.getString(1)) || !item.name.equals(rows.getString(2))) {
                            throw new CatalogProblem("기존 종과 이관 값이 충돌합니다.");
                        }
                    }
                }
            }
        }
        for (Item item : plan.breeds.values()) {
            try (var query = sql.prepareStatement("SELECT b.uuid, b.name, s.uuid FROM breed b "
                    + "LEFT JOIN species s ON s.id = b.species_id WHERE b.uuid = ? OR (s.uuid = ? AND b.name = ?)")) {
                query.setString(1, item.uuid);
                query.setString(2, plan.species.get(item.parent).uuid);
                query.setString(3, item.name);
                try (var rows = query.executeQuery()) {
                    while (rows.next()) {
                        if (!item.uuid.equals(rows.getString(1)) || !item.name.equals(rows.getString(2))
                                || !plan.species.get(item.parent).uuid.equals(rows.getString(3))) {
                            throw new CatalogProblem("기존 품종과 이관 값이 충돌합니다.");
                        }
                    }
                }
            }
        }
    }

    private static void applyMysql(Connection sql, Plan plan) throws SQLException {
        sql.setAutoCommit(false);
        try {
            for (Item item : plan.species.values()) {
                try (var insert = sql.prepareStatement("INSERT INTO species (uuid, name) "
                        + "SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM species WHERE uuid = ?)")) {
                    insert.setString(1, item.uuid);
                    insert.setString(2, item.name);
                    insert.setString(3, item.uuid);
                    insert.executeUpdate();
                }
            }
            for (Item item : plan.breeds.values()) {
                try (var insert = sql.prepareStatement("INSERT INTO breed (uuid, species_id, name) "
                        + "SELECT ?, id, ? FROM species WHERE uuid = ? "
                        + "AND NOT EXISTS (SELECT 1 FROM breed WHERE uuid = ?)")) {
                    insert.setString(1, item.uuid);
                    insert.setString(2, item.name);
                    insert.setString(3, plan.species.get(item.parent).uuid);
                    insert.setString(4, item.uuid);
                    insert.executeUpdate();
                }
            }
            validateMysql(sql, plan);
            sql.commit();
        } catch (Exception exception) {
            sql.rollback();
            throw exception;
        } finally {
            sql.setAutoCommit(true);
        }
    }

    private static void applyMongo(MongoDatabase database, Plan plan) {
        for (Document pet : database.getCollection("pets").find()) {
            String oldSpecies = text(pet.get("speciesId"));
            String legacy = plan.speciesLegacy(oldSpecies);
            Document changes = new Document("speciesId", plan.species.get(legacy).uuid);
            if (pet.get("breedId") != null) changes.put("breedId", plan.requireBreed(legacy, text(pet.get("breedId"))).uuid);
            Document registration = pet.get("registration", Document.class);
            Document snapshot = registration == null ? null : registration.get("responseSnapshot", Document.class);
            if (snapshot != null && snapshot.get("species") instanceof Document savedSpecies) {
                String snapshotLegacy = plan.speciesLegacy(text(savedSpecies.get("speciesId")));
                changes.put("registration.responseSnapshot.species.speciesId", plan.species.get(snapshotLegacy).uuid);
                if (snapshot.get("breed") instanceof Document savedBreed) {
                    changes.put("registration.responseSnapshot.breed.breedId",
                        plan.requireBreed(snapshotLegacy, text(savedBreed.get("breedId"))).uuid);
                }
            }
            Document filter = new Document("_id", pet.get("_id")).append("speciesId", pet.get("speciesId"))
                .append("breedId", pet.get("breedId")).append("registration", pet.get("registration"));
            var result = database.getCollection("pets").updateOne(filter, new Document("$set", changes));
            if (!result.wasAcknowledged() || result.getMatchedCount() != 1) {
                throw new CatalogProblem("동물 정보가 변경되어 이관을 중지했습니다.");
            }
        }
    }

    private static final class CatalogProblem extends RuntimeException {
        CatalogProblem(String message) {
            super(message);
        }
    }

    static final class Item {
        final String legacy;
        final String parent;
        final String uuid;
        final String name;
        Item(String legacy, String parent, String uuid, String name) {
            this.legacy = legacy; this.parent = parent; this.uuid = uuid; this.name = name;
        }
    }

    static final class Plan {
        final Map<String, Item> species = new LinkedHashMap<>();
        final Map<String, Item> breeds = new LinkedHashMap<>();
        long petCount;

        void addSpecies(String legacy, String name) {
            if (name.codePointCount(0, name.length()) > 50) {
                throw new CatalogProblem("종 이름이 너무 깁니다.");
            }
            Item old = species.putIfAbsent(legacy, new Item(legacy, null, uuid("species", legacy), name));
            if (old != null && !old.name.equals(name)) {
                throw new CatalogProblem("종 이름 충돌");
            }
        }

        void addBreed(String parent, String legacy, String name) {
            if (name.codePointCount(0, name.length()) > 50) {
                throw new CatalogProblem("품종 이름이 너무 깁니다.");
            }
            String key = parent + "\u0000" + legacy;
            Item old = breeds.putIfAbsent(key, new Item(legacy, parent, uuid("breed", key), name));
            if (old != null) {
                throw new CatalogProblem("품종 식별자 중복");
            }
        }

        String speciesLegacy(String value) {
            if (species.containsKey(value)) {
                return value;
            }
            return species.values().stream().filter(item -> item.uuid.equals(value)).map(item -> item.legacy)
                .findFirst().orElseThrow(() -> new CatalogProblem("종 참조를 찾을 수 없습니다."));
        }

        Item requireBreed(String parent, String value) {
            Item direct = breeds.get(parent + "\u0000" + value);
            if (direct != null) {
                return direct;
            }
            return breeds.values().stream().filter(item -> item.parent.equals(parent) && item.uuid.equals(value))
                .findFirst().orElseThrow(() -> new CatalogProblem("품종 참조를 찾을 수 없습니다."));
        }
    }
}
