package com.louiekury.pet.pet.application.updatepet;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

public interface UpdatePetPort {
    record Data(String id, String name, String speciesId, String breedId, String breedName,
            String profileMediaId, LocalDate birth, String sex, Boolean neutered,
            BigDecimal weight, Boolean vaccination, Instant createdAt, Instant updatedAt) { }

    Optional<Data> find(String id, boolean resolveBreedName);
    boolean update(Data previous, Map<String, Object> changes, Instant updatedAt);
}
