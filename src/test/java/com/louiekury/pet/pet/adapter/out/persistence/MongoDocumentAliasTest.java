package com.louiekury.pet.pet.adapter.out.persistence;

import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class MongoDocumentAliasTest {

    private static final String PET_ALIAS =
        "com.louiekury.pet.registration.adapter.out.persistence.PetMongoDocument";
    private static final String BREED_ALIAS =
        "com.louiekury.pet.registration.adapter.out.persistence.PetBreedCatalogMongoDocument";
    private MappingMongoConverter converter;

    @BeforeEach
    void setUp() throws Exception {
        MongoCustomConversions conversions = MongoCustomConversions.create(adapter -> { });
        MongoMappingContext context = new MongoMappingContext();
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        context.setInitialEntitySet(Set.of(PetMongoDocument.class, PetBreedCatalogMongoDocument.class));
        context.afterPropertiesSet();
        converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
    }

    @Test
    void writesExistingPetTypeAndReadsPreviouslyStoredPet() {
        PetMongoDocument pet = new PetMongoDocument();
        ReflectionTestUtils.setField(pet, "id", "pet-1");
        ReflectionTestUtils.setField(pet, "name", "루이");
        Document written = new Document();
        converter.write(pet, written);
        assertThat(written.getString("_class")).isEqualTo(PET_ALIAS);

        Object restored = converter.read(Object.class,
            new Document("_id", "pet-1").append("name", "루이").append("_class", PET_ALIAS));
        assertThat(restored).isInstanceOf(PetMongoDocument.class);
        assertThat(((PetMongoDocument) restored).getName()).isEqualTo("루이");
    }

    @Test
    void writesExistingBreedCatalogTypeAndReadsPreviouslyStoredCatalog() {
        PetBreedCatalogMongoDocument catalog = new PetBreedCatalogMongoDocument();
        ReflectionTestUtils.setField(catalog, "id", "dog");
        Document written = new Document();
        converter.write(catalog, written);
        assertThat(written.getString("_class")).isEqualTo(BREED_ALIAS);

        Object restored = converter.read(Object.class,
            new Document("_id", "dog").append("_class", BREED_ALIAS));
        assertThat(restored).isInstanceOf(PetBreedCatalogMongoDocument.class);
        assertThat(((PetBreedCatalogMongoDocument) restored).getId()).isEqualTo("dog");
    }
}
