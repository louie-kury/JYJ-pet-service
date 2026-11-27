package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "PET_TEST_MYSQL_CATALOG_URL", matches = ".+")
class PetCatalogMysqlIntegrationTest {
    private static PetSpeciesPersistenceAdapter species;
    private static PetBreedPersistenceAdapter breed;
    private static String dogId;
    private static String catId;
    private static String rabbitId;
    private static String poodleId;

    @BeforeAll
    static void connect() {
        var source = new DriverManagerDataSource(System.getenv("PET_TEST_MYSQL_CATALOG_URL"), "root", "");
        var jdbc = new JdbcTemplate(source);
        species = new PetSpeciesPersistenceAdapter(jdbc);
        breed = new PetBreedPersistenceAdapter(jdbc);
        dogId = jdbc.queryForObject("SELECT uuid FROM species WHERE name = ?", String.class, "강아지");
        catId = jdbc.queryForObject("SELECT uuid FROM species WHERE name = ?", String.class, "고양이");
        rabbitId = jdbc.queryForObject("SELECT uuid FROM species WHERE name = ?", String.class, "토끼");
        poodleId = jdbc.queryForObject("SELECT uuid FROM breed WHERE name = ?", String.class, "푸들");
    }

    @Test
    void readsSpeciesBeyondFormerEnum() {
        assertThat(species.getPetSpecies(rabbitId)).get().satisfies(value -> {
            assertThat(value.getSpeciesId()).isEqualTo(rabbitId);
            assertThat(UUID.fromString(value.getSpeciesId())).isNotNull();
            assertThat(value.getName()).isEqualTo("토끼");
        });
        assertThat(species.getPetSpecies("missing")).isEmpty();
        assertThat(species.getPetSpecies(dogId.toUpperCase(java.util.Locale.ROOT))).isEmpty();
    }

    @Test
    void readsBreedOnlyForItsSpecies() {
        PetSpecies dog = species.getPetSpecies(dogId).orElseThrow();
        PetSpecies cat = species.getPetSpecies(catId).orElseThrow();
        assertThat(breed.getPetBreedBySpeciesAndId(dog, poodleId))
            .get().satisfies(value -> assertThat(value.getName()).isEqualTo("푸들"));
        assertThat(breed.getPetBreedBySpeciesAndId(cat, poodleId)).isEmpty();
        assertThat(breed.getPetBreedBySpeciesAndId(dog, "missing")).isEmpty();
    }

    @Test
    void readsCatalogForSeveralPetsInBatches() {
        var speciesById = species.getPetSpeciesByIds(java.util.Set.of(dogId, catId, rabbitId));
        assertThat(speciesById).containsKeys(dogId, catId, rabbitId);
        var breedsById = breed.getPetBreedsByIds(speciesById, java.util.Set.of(poodleId));
        assertThat(breedsById.get(poodleId).getName()).isEqualTo("푸들");
        assertThat(breedsById.get(poodleId).getSpecies().getSpeciesId()).isEqualTo(dogId);
    }

    @Test
    void blankCatalogNameIsNotReturnedAsValidData() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(System.getenv("PET_TEST_MYSQL_CATALOG_URL"), "root", ""));
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO species (uuid, name) VALUES (?, ?)", id, "");
        try {
            assertThatThrownBy(() -> species.getPetSpecies(id)).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.REFERENCE_DATA_UNAVAILABLE));
        } finally {
            jdbc.update("DELETE FROM species WHERE uuid = ?", id);
        }
    }

    @Test
    void databaseFailureIsNotTreatedAsMissingSpecies() {
        var source = new DriverManagerDataSource("jdbc:mysql://127.0.0.1:1/unused?connectTimeout=500", "root", "");
        var unavailable = new PetSpeciesPersistenceAdapter(new JdbcTemplate(source));
        assertThatThrownBy(() -> unavailable.getPetSpecies("dog")).isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.REFERENCE_DATA_UNAVAILABLE));
    }

    @Test
    void orphanBreedIsNotTreatedAsUserInputError() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(System.getenv("PET_TEST_MYSQL_CATALOG_URL"), "root", ""));
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO breed (uuid, species_id, name) VALUES (?, ?, ?)", id, 999999L, "orphan-" + id);
        try {
            PetSpecies dog = species.getPetSpecies(dogId).orElseThrow();
            assertThatThrownBy(() -> breed.getPetBreedBySpeciesAndId(dog, id))
                .isInstanceOfSatisfying(BusinessException.class,
                    error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.REFERENCE_DATA_UNAVAILABLE));
        } finally {
            jdbc.update("DELETE FROM breed WHERE uuid = ?", id);
        }
    }
}
