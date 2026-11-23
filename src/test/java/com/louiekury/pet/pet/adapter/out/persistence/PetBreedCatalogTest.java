package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PetBreedCatalogTest {

    @Test
    void missingBreedNameIsReferenceDataFailure() {
        PetBreedCatalogMongoDocument.Breed breed = new PetBreedCatalogMongoDocument.Breed();
        ReflectionTestUtils.setField(breed, "breedId", "poodle");
        ReflectionTestUtils.setField(breed, "name", " ");
        ReflectionTestUtils.setField(breed, "active", true);
        PetBreedCatalogMongoDocument catalog = new PetBreedCatalogMongoDocument();
        ReflectionTestUtils.setField(catalog, "id", "dog");
        ReflectionTestUtils.setField(catalog, "breeds", List.of(breed));

        assertThatThrownBy(() -> catalog.getPetBreed(PetSpecies.DOG, "poodle"))
            .isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFERENCE_DATA_UNAVAILABLE));
    }

    @Test
    void speciesMismatchIsReferenceDataFailure() {
        PetBreedCatalogMongoDocument catalog = new PetBreedCatalogMongoDocument();
        ReflectionTestUtils.setField(catalog, "id", "cat");

        assertThatThrownBy(() -> catalog.getPetBreed(PetSpecies.DOG, "poodle"))
            .isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFERENCE_DATA_UNAVAILABLE));
    }
}
