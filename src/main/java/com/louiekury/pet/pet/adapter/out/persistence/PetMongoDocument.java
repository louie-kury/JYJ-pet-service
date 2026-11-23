package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.global.persistence.BaseDocument;
import com.louiekury.pet.pet.application.createpet.PetResult;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Getter
@TypeAlias("com.louiekury.pet.registration.adapter.out.persistence.PetMongoDocument")
@Document("pets")
public class PetMongoDocument extends BaseDocument {

    @MongoId(FieldType.STRING)
    private String id;
    @Field(write = Field.Write.ALWAYS)
    private String caregiverId;
    private String name;
    private String speciesId;
    private String breedId;
    private String profileMediaId;
    private String birth;
    private String sex;
    @Field(write = Field.Write.ALWAYS)
    private Boolean isNeutered;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal weight;
    @Field(write = Field.Write.ALWAYS)
    private Boolean basicVaccinationCompleted;
    private Map<String, Object> registration;

    public static PetMongoDocument from(PetResult result, String idempotencyKey) {
        PetMongoDocument document = new PetMongoDocument();
        document.id = result.getPetId();
        document.caregiverId = null;
        document.name = result.getName();
        document.speciesId = result.getSpecies().getSpeciesId();
        document.breedId = result.getBreed() == null ? null : result.getBreed().getBreedId();
        document.profileMediaId = null;
        document.birth = result.getBirth() == null ? null : result.getBirth().toString();
        document.sex = result.getSex();
        document.isNeutered = result.getIsNeutered();
        document.weight = result.getWeight();
        document.basicVaccinationCompleted = result.getBasicVaccinationCompleted();
        document.initializeTimestamps(result.getCreatedAt());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("petId", result.getPetId());
        snapshot.put("name", result.getName());
        snapshot.put("species", Map.of("speciesId", result.getSpecies().getSpeciesId(),
            "name", result.getSpecies().getName()));
        snapshot.put("breed", result.getBreed() == null ? null
            : Map.of("breedId", result.getBreed().getBreedId(), "name", result.getBreed().getName()));
        snapshot.put("profileMedia", null);
        snapshot.put("birth", document.birth);
        snapshot.put("age", result.getAge() == null ? null
            : Map.of("years", result.getAge().getYears(), "months", result.getAge().getMonths()));
        snapshot.put("sex", result.getSex());
        snapshot.put("isNeutered", result.getIsNeutered());
        snapshot.put("weight", result.getWeight() == null ? null
            : new org.bson.types.Decimal128(result.getWeight()));
        snapshot.put("basicVaccinationCompleted", result.getBasicVaccinationCompleted());
        snapshot.put("createdAt", result.getCreatedAt());
        snapshot.put("updatedAt", result.getUpdatedAt());
        document.registration = new LinkedHashMap<>();
        if (idempotencyKey != null) {
            document.registration.put("requestKey", idempotencyKey);
        }
        document.registration.put("responseSnapshot", snapshot);
        return document;
    }
}
