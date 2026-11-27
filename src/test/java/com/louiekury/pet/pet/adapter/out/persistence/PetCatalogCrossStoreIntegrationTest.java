package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.createpet.CreatePetCommand;
import com.louiekury.pet.pet.application.createpet.CreatePetService;
import com.louiekury.pet.pet.application.getpets.GetPetsQuery;
import com.louiekury.pet.pet.application.getpets.GetPetsService;
import com.louiekury.pet.pet.application.updatepet.UpdatePetService;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "PET_TEST_MYSQL_CATALOG_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "PET_TEST_MONGODB_URI", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PetCatalogCrossStoreIntegrationTest {
    private MongoClient client;
    private MongoTemplate mongo;
    private PetSpeciesPersistenceAdapter species;
    private PetBreedPersistenceAdapter breeds;
    private String dogId;
    private String catId;
    private String poodleId;
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-27T00:00:00Z"), ZoneOffset.UTC);

    @BeforeAll
    void setup() {
        var dataSource = new DriverManagerDataSource(System.getenv("PET_TEST_MYSQL_CATALOG_URL"), "root", "");
        var jdbc = new JdbcTemplate(dataSource);
        species = new PetSpeciesPersistenceAdapter(jdbc);
        breeds = new PetBreedPersistenceAdapter(jdbc);
        dogId = jdbc.queryForObject("SELECT uuid FROM species WHERE name = ?", String.class, "강아지");
        catId = jdbc.queryForObject("SELECT uuid FROM species WHERE name = ?", String.class, "고양이");
        poodleId = jdbc.queryForObject("SELECT uuid FROM breed WHERE name = ?", String.class, "푸들");
        client = MongoClients.create(System.getenv("PET_TEST_MONGODB_URI"));
        mongo = new MongoTemplate(client, "pet_catalog_cross_test_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @AfterAll
    void cleanup() {
        if (mongo != null) mongo.getDb().drop();
        if (client != null) client.close();
    }

    @Test
    void createsReadsAndUpdatesPetWithMysqlCatalog() {
        var create = new CreatePetService(breeds, species, new PetPersistenceAdapter(mongo), clock);
        var result = create.createPet(CreatePetCommand.of("루이", dogId, poodleId, null, null,
            "MALE", null, null, null, null, "owner"));
        assertThat(result.getSpecies().getName()).isEqualTo("강아지");
        assertThat(result.getBreed().getName()).isEqualTo("푸들");

        var listAdapter = new PetListPersistenceAdapter(mongo, species, breeds);
        var list = new GetPetsService(listAdapter, clock).getPets(GetPetsQuery.of(0, 4, "owner"));
        assertThat(list.getContent()).hasSize(1);
        assertThat(list.getContent().get(0).getBreed().getName()).isEqualTo("푸들");
        var detail = new PetDetailPersistenceAdapter(mongo, listAdapter).getPet(result.getPetId(), "owner");
        assertThat(detail).isPresent();
        assertThat(detail.orElseThrow().getSummary().getSpecies().getName()).isEqualTo("강아지");

        var update = new UpdatePetService(new PetUpdatePersistenceAdapter(mongo, species, breeds), breeds, species, clock);
        var changed = update.updatePet(result.getPetId(), new HashMap<>(Map.of("name", "수정")), "owner");
        assertThat(changed.getName()).isEqualTo("수정");
        assertThat(changed.getBreed().getName()).isEqualTo("푸들");
    }

    @Test
    void rejectsBreedOfAnotherSpecies() {
        var create = new CreatePetService(breeds, species, new PetPersistenceAdapter(mongo), clock);
        assertThatThrownBy(() -> create.createPet(CreatePetCommand.of("루이", catId, poodleId, null, null,
            "MALE", null, null, null, null, "owner")))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }
}
