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

    @BeforeAll
    static void connect() {
        var source = new DriverManagerDataSource(System.getenv("PET_TEST_MYSQL_CATALOG_URL"), "root", "");
        var jdbc = new JdbcTemplate(source);
        species = new PetSpeciesPersistenceAdapter(jdbc);
        breed = new PetBreedPersistenceAdapter(jdbc);
    }

    @Test
    void readsSpeciesBeyondFormerEnum() {
        assertThat(species.getPetSpecies("rabbit")).get().satisfies(value -> {
            assertThat(value.getSpeciesId()).isEqualTo("rabbit");
            assertThat(value.getName()).isEqualTo("토끼");
        });
        assertThat(species.getPetSpecies("missing")).isEmpty();
        assertThat(species.getPetSpecies("DOG")).isEmpty();
    }

    @Test
    void readsBreedOnlyForItsSpecies() {
        PetSpecies dog = species.getPetSpecies("dog").orElseThrow();
        PetSpecies cat = species.getPetSpecies("cat").orElseThrow();
        assertThat(breed.getPetBreedBySpeciesAndId(dog, "poodle"))
            .get().satisfies(value -> assertThat(value.getName()).isEqualTo("푸들"));
        assertThat(breed.getPetBreedBySpeciesAndId(cat, "poodle")).isEmpty();
        assertThat(breed.getPetBreedBySpeciesAndId(dog, "missing")).isEmpty();
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
            PetSpecies dog = species.getPetSpecies("dog").orElseThrow();
            assertThatThrownBy(() -> breed.getPetBreedBySpeciesAndId(dog, id))
                .isInstanceOfSatisfying(BusinessException.class,
                    error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.REFERENCE_DATA_UNAVAILABLE));
        } finally {
            jdbc.update("DELETE FROM breed WHERE uuid = ?", id);
        }
    }
}
